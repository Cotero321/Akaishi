'use strict';
// 校验 GUI 贴图重制：PNG 头 + 256x256 + 不透明 + 「仅面板底纹像素被改」像素级回归 + Java 引用可达 + 无孤儿
// 依据：GuiWidgets 运行时按 139/55/255/198 自绘槽位与面板，故贴图中这四色必须逐像素保真，
//       只允许原本为 198(面板灰) 的像素被替换为拉丝底纹/刻线/主题描边。
const fs = require('node:fs'), path = require('node:path'), zlib = require('zlib');
const root = __dirname;
const guiDir = path.join(root, 'common/src/main/resources/assets/akaishi/textures/gui');
const bakDir = path.join(root, 'gui_layouts/gui_backup_original');
const javaRoots = [path.join(root, 'common/src/main/java'), path.join(root, 'forge/src/main/java')];

const TEXTURES = ['akaishi_auto_collector', 'akaishi_energy_cell',
  'akaishi_energy_generator', 'akaishi_fuel_canner', 'akaishi_fusion_controller', 'akaishi_life_struct',
  'akaishi_purifier', 'akaishi_purifier_matrix', 'akaishi_reactor_controller', 'akaishi_reactor_fuel_port',
  'akaishi_super_generator', 'akaishi_wireless_terminal'];

const PNG = Buffer.from('89504e470d0a1a0a', 'hex');
const errors = [], warns = [];

// --- 最小 PNG 解码（RGBA8，支持 filter 0~4） ---
function readPng(buf) {
  if (!buf.subarray(0, 8).equals(PNG)) throw Error('PNG header');
  let pos = 8, width = 0, height = 0, depth = 0, ctype = 0;
  const idat = [];
  while (pos + 8 <= buf.length) {
    const len = buf.readUInt32BE(pos);
    const tag = buf.toString('ascii', pos + 4, pos + 8);
    const chunk = buf.subarray(pos + 8, pos + 8 + len);
    pos += 12 + len;
    if (tag === 'IHDR') { width = chunk.readUInt32BE(0); height = chunk.readUInt32BE(4); depth = chunk[8]; ctype = chunk[9]; }
    else if (tag === 'IDAT') idat.push(chunk);
    else if (tag === 'IEND') break;
  }
  if (depth !== 8 || (ctype !== 6 && ctype !== 2)) throw Error('expect RGBA8/RGB8, got depth=' + depth + ' type=' + ctype);
  const ch = ctype === 6 ? 4 : 3;
  const stride = width * ch;
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const px = Buffer.alloc(width * height * 4);
  let prev = Buffer.alloc(stride), i = 0, o = 0;
  for (let y = 0; y < height; y++) {
    const ft = raw[i++];
    const line = Buffer.from(raw.subarray(i, i + stride)); i += stride;
    if (ft === 1) { for (let x = ch; x < stride; x++) line[x] = (line[x] + line[x - ch]) & 255; }
    else if (ft === 2) { for (let x = 0; x < stride; x++) line[x] = (line[x] + prev[x]) & 255; }
    else if (ft === 3) { for (let x = 0; x < stride; x++) line[x] = (line[x] + ((x >= ch ? line[x - ch] : 0) + prev[x] >> 1)) & 255; }
    else if (ft === 4) {
      for (let x = 0; x < stride; x++) {
        const a = x >= ch ? line[x - ch] : 0, b = prev[x], c = x >= ch ? prev[x - ch] : 0;
        const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
        line[x] = (line[x] + ((pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c))) & 255;
      }
    }
    for (let x = 0; x < width; x++) {
      const s = x * ch;
      px[o++] = line[s]; px[o++] = line[s + 1]; px[o++] = line[s + 2];
      px[o++] = ch === 4 ? line[s + 3] : 255;
    }
    prev = line;
  }
  return { width, height, px };
}

function walkJava(dir, out) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walkJava(p, out);
    else if (e.name.endsWith('.java')) out.push(p);
  }
}

// 1. 逐张校验
let changedTotal = 0;
for (const n of TEXTURES) {
  const cur = path.join(guiDir, n + '.png');
  try {
    const img = readPng(fs.readFileSync(cur));
    if (img.width !== 256 || img.height !== 256) throw Error('size ' + img.width + 'x' + img.height + ' expect 256x256');
    // 不透明校验
    for (let i = 3; i < img.px.length; i += 4) { if (img.px[i] !== 255) throw Error('has transparent pixel @' + (i >> 2)); }
    // 像素级回归：仅允许原 198 灰被改写
    const bakPath = path.join(bakDir, n + '.png');
    if (!fs.existsSync(bakPath)) { warns.push(n + ': 无备份，跳过像素回归'); continue; }
    const bak = readPng(fs.readFileSync(bakPath));
    if (bak.width !== img.width || bak.height !== img.height) throw Error('backup size mismatch');
    let changed = 0;
    for (let i = 0; i < img.px.length; i += 4) {
      const same = img.px[i] === bak.px[i] && img.px[i + 1] === bak.px[i + 1] && img.px[i + 2] === bak.px[i + 2];
      if (same) continue;
      changed++;
      const br = bak.px[i], bg = bak.px[i + 1], bb = bak.px[i + 2];
      if (!(br === 198 && bg === 198 && bb === 198)) {
        throw Error('非面板像素被改写 @(' + ((i >> 2) % 256) + ',' + ((i >> 2) / 256 | 0) + ') src=' + br + ',' + bg + ',' + bb);
      }
    }
    if (changed === 0) throw Error('未发生任何重制');
    changedTotal += changed;
    console.log('OK ' + n + ' (changed px ' + changed + ')');
  } catch (x) { errors.push(n + ': ' + x.message); }
}

// 2. Java 引用可达
const javaFiles = [];
for (const r of javaRoots) if (fs.existsSync(r)) walkJava(r, javaFiles);
const javaSrc = javaFiles.map(f => fs.readFileSync(f, 'utf8')).join('\n');
for (const n of TEXTURES) {
  if (!javaSrc.includes('textures/gui/' + n + '.png')) errors.push('Java 未引用 textures/gui/' + n + '.png');
}

// 3. 孤儿检查
const present = fs.readdirSync(guiDir).filter(f => f.endsWith('.png')).map(f => f.slice(0, -4));
for (const p of present) if (!TEXTURES.includes(p)) errors.push('孤儿贴图 gui/' + p + '.png');

console.log(JSON.stringify({ textures: TEXTURES.length, changedPixels: changedTotal, warns: warns.length, errors: errors.length }, null, 2));
warns.forEach(w => console.log('WARN ' + w));
errors.forEach(e => console.log('ERR ' + e));
if (errors.length) process.exitCode = 1;
