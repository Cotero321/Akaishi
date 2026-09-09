'use strict';
// 校验流体贴图：PNG 头 + 16x(16*FRAMES) 动画条 + 逐帧全不透明 + 逐帧双向无缝 + 循环连续性
//             + .mcmeta 帧参数 + ModFluidsImpl 引用可达 + 无孤儿
const fs = require('node:fs'), path = require('node:path'), zlib = require('node:zlib');
const root = __dirname;
const DIR = path.join(root, 'common/src/main/resources/assets/akaishi/textures/block/fluid');
const IMPL = path.join(root, 'forge/src/main/java/com/example/akaishi/forge/fluid/ModFluidsImpl.java');

// 与 rework_fluid_assets.js 保持一致
const FRAMES = 32;
const FRAMETIME = 2;
// 相邻帧像素最大允许跳变（保证播放平滑不闪跳）
const LOOP_LIMIT = 16;
// 接缝容差：环向步进恰为函数最陡的一步时，其值可比内部采样最大值多 1（取整误差）
const SEAM_TOL = 1;

// 与 ModFluidsImpl.java 的 12 种流体一一对应
const FLUIDS = ['activated_life_fuel', 'advanced_mixture_fuel', 'dragon_fuel', 'end_mixture_fuel',
  'exhausted_life_fuel', 'nether_compound_energy', 'nether_compound_fuel', 'nether_pure_energy',
  'plasma', 'pure_fuel', 'sculk_life_fuel', 'ultimate_mixture_fuel'];

const errors = [], warns = [];
const PNG = Buffer.from('89504e470d0a1a0a', 'hex');

// 仅需支持重制脚本产出的 RGBA8/RGB8 filter 0
function readPng(file) {
  const b = fs.readFileSync(file);
  if (!b.subarray(0, 8).equals(PNG)) throw Error('PNG header');
  const w = b.readUInt32BE(16), h = b.readUInt32BE(20), depth = b[24], ctype = b[25];
  if (depth !== 8 || (ctype !== 6 && ctype !== 2)) throw Error('expect RGBA8/RGB8, got depth ' + depth + ' ctype ' + ctype);
  const ch = ctype === 6 ? 4 : 3, stride = w * ch;
  let pos = 8; const idat = [];
  while (pos + 8 <= b.length) {
    const len = b.readUInt32BE(pos), tag = b.toString('ascii', pos + 4, pos + 8);
    if (tag === 'IDAT') idat.push(b.subarray(pos + 8, pos + 8 + len));
    pos += 12 + len;
  }
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const px = Buffer.alloc(w * h * 4);
  let i = 0, o = 0, prev = Buffer.alloc(stride);
  for (let y = 0; y < h; y++) {
    const ft = raw[i++];
    const line = Buffer.from(raw.subarray(i, i + stride)); i += stride;
    if (ft === 1) { for (let x = ch; x < stride; x++) line[x] = (line[x] + line[x - ch]) & 255; }
    else if (ft === 2) { for (let x = 0; x < stride; x++) line[x] = (line[x] + prev[x]) & 255; }
    else if (ft === 3) { for (let x = 0; x < stride; x++) line[x] = (line[x] + (((x >= ch ? line[x - ch] : 0) + prev[x]) >> 1)) & 255; }
    else if (ft === 4) {
      for (let x = 0; x < stride; x++) {
        const a = x >= ch ? line[x - ch] : 0, bb = prev[x], c = x >= ch ? prev[x - ch] : 0;
        const p = a + bb - c, pa = Math.abs(p - a), pb = Math.abs(p - bb), pc = Math.abs(p - c);
        line[x] = (line[x] + ((pa <= pb && pa <= pc) ? a : (pb <= pc ? bb : c))) & 255;
      }
    } else if (ft !== 0) throw Error('unsupported filter ' + ft);
    for (let x = 0; x < w; x++) {
      const s = x * ch;
      px[o++] = line[s]; px[o++] = line[s + 1]; px[o++] = line[s + 2];
      px[o++] = ch === 4 ? line[s + 3] : 255;
    }
    prev = line;
  }
  return { w, h, px };
}

// 通道最大绝对差（y0 为所在帧在整条中的起始行）
function maxDiff(px, w, y0, ax, ay, bx, by) {
  let m = 0;
  for (let c = 0; c < 3; c++) {
    const d = Math.abs(px[((y0 + ay) * w + ax) * 4 + c] - px[((y0 + by) * w + bx) * 4 + c]);
    if (d > m) m = d;
  }
  return m;
}

// 单帧无缝断言：环向相邻差（x=w-1 -> x=0、y=h-1 -> y=0）不得大于内部相邻差的最大值
function checkSeamless(px, w, y0, name) {
  const h = 16;
  let maxH = 0, maxV = 0;
  for (let y = 0; y < h; y++) for (let x = 0; x < w - 1; x++) maxH = Math.max(maxH, maxDiff(px, w, y0, x, y, x + 1, y));
  for (let x = 0; x < w; x++) for (let y = 0; y < h - 1; y++) maxV = Math.max(maxV, maxDiff(px, w, y0, x, y, x, y + 1));
  let seamH = 0, seamV = 0;
  for (let y = 0; y < h; y++) seamH = Math.max(seamH, maxDiff(px, w, y0, w - 1, y, 0, y));
  for (let x = 0; x < w; x++) seamV = Math.max(seamV, maxDiff(px, w, y0, x, h - 1, x, 0));
  if (seamH > maxH + SEAM_TOL) errors.push(name + ': 水平接缝跳变 ' + seamH + ' > 内部最大 ' + maxH);
  if (seamV > maxV + SEAM_TOL) errors.push(name + ': 垂直接缝跳变 ' + seamV + ' > 内部最大 ' + maxV);
}

// 两帧同位置像素最大跳变（t2 可为 0，用于末帧 -> 首帧闭环校验）
function frameDiff(px, w, t1, t2) {
  const o1 = t1 * 16 * w * 4, o2 = t2 * 16 * w * 4, len = 16 * w * 4;
  let m = 0;
  for (let i = 0; i < len; i += 4) {
    for (let c = 0; c < 3; c++) {
      const d = Math.abs(px[o1 + i + c] - px[o2 + i + c]);
      if (d > m) m = d;
    }
  }
  return m;
}

// 1. 逐张校验尺寸 / 不透明 / 逐帧无缝 / 首尾闭环
let count = 0;
for (const n of FLUIDS) {
  for (const suffix of ['_still', '_flow']) {
    const name = n + suffix;
    const file = path.join(DIR, name + '.png');
    if (!fs.existsSync(file)) { errors.push('缺失 ' + name + '.png'); continue; }
    try {
      const img = readPng(file);
      if (img.w !== 16 || img.h !== 16 * FRAMES) { errors.push(name + ': 尺寸 ' + img.w + 'x' + img.h + '，应为 16x' + (16 * FRAMES)); continue; }
      let opaque = true;
      for (let i = 3; i < img.px.length; i += 4) if (img.px[i] !== 255) { opaque = false; break; }
      if (!opaque) errors.push(name + ': 存在非不透明像素');
      for (let t = 0; t < FRAMES; t++) checkSeamless(img.px, img.w, t * 16, name + ' 帧' + t);
      // 逐帧步进上限 + 首尾闭环：末帧 -> 首帧的跳变不得大于正常相邻帧步进，否则播放会闪跳
      let maxAdj = 0;
      for (let t = 0; t < FRAMES - 1; t++) maxAdj = Math.max(maxAdj, frameDiff(img.px, img.w, t, t + 1));
      if (maxAdj > LOOP_LIMIT) errors.push(name + ': 相邻帧跳变 ' + maxAdj + ' > ' + LOOP_LIMIT);
      const loop = frameDiff(img.px, img.w, FRAMES - 1, 0);
      if (loop > maxAdj) errors.push(name + ': 首尾帧跳变 ' + loop + ' > 相邻帧最大 ' + maxAdj);
      count++;
    } catch (x) { errors.push(name + ': ' + x.message); }
  }
}

// 1b. .mcmeta 帧参数
for (const n of FLUIDS) {
  for (const suffix of ['_still', '_flow']) {
    const name = n + suffix;
    const mf = path.join(DIR, name + '.png.mcmeta');
    if (!fs.existsSync(mf)) { errors.push('缺失 ' + name + '.png.mcmeta'); continue; }
    try {
      const a = (JSON.parse(fs.readFileSync(mf, 'utf8')).animation) || {};
      if (a.frametime !== FRAMETIME) errors.push(name + '.mcmeta frametime=' + a.frametime + '，应为 ' + FRAMETIME);
      if (a.interpolate !== true) errors.push(name + '.mcmeta interpolate 应为 true');
    } catch (x) { errors.push(name + '.mcmeta 解析失败: ' + x.message); }
  }
}

// 2. 引用可达：ModFluidsImpl.java 必须同时引用 <n>_still 与 <n>_flow
try {
  const src = fs.readFileSync(IMPL, 'utf8');
  for (const n of FLUIDS) {
    if (!src.includes('block/fluid/' + n + '_still')) errors.push('ModFluidsImpl 未引用 ' + n + '_still');
    if (!src.includes('block/fluid/' + n + '_flow')) errors.push('ModFluidsImpl 未引用 ' + n + '_flow');
  }
} catch (x) { errors.push('ModFluidsImpl 读取失败: ' + x.message); }

// 3. 孤儿文件（目录内多余 PNG / mcmeta）
const expected = new Set(FLUIDS.flatMap(n => [n + '_still.png', n + '_flow.png',
  n + '_still.png.mcmeta', n + '_flow.png.mcmeta']));
for (const f of fs.readdirSync(DIR)) {
  if ((f.endsWith('.png') || f.endsWith('.mcmeta')) && !expected.has(f)) errors.push('孤儿 ' + f);
}

console.log(JSON.stringify({ fluids: FLUIDS.length, textures: count, warns: warns.length, errors: errors.length }, null, 2));
warns.forEach(w => console.log('WARN ' + w));
errors.forEach(e => console.log('ERR ' + e));
if (errors.length) process.exitCode = 1;
