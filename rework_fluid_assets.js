// 流体贴图重制：把旧「边框色块」占位图替换为可无缝平铺、可循环播放的流体表面（工业机械 + 生物科技风）。
// 原理：
//   1) 从原图自动提取流体主色（出现最多的非灰非白像素），保证 12 种流体识别色不变；
//   2) 用周期为 16 的正弦波叠加确定性噪声，保证单帧 16x16 双向无缝（sin 的周期整除贴图边长）；
//   3) 帧相位 phase = t/FRAMES * TAU 代入正弦项，构成行波；因各项相位增量均为 TAU 的整数倍，
//      第 FRAMES-1 帧回到第 0 帧无跳变，整段动画首尾闭合；
//   4) still = 缓波面，flow = 竖向流纹叠加向下推移的亮度带，二者同色系但纹理可区分；
//   5) 输出 16 x (16*FRAMES) 纵向帧条 + 同名 .mcmeta（frametime/interpolate），MC 自动按帧切分。
// 幂等：首次运行把原图备份到 gui_layouts/fluid_backup/，之后始终以备份为源，可反复执行。
// 用法：node rework_fluid_assets.js
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/block/fluid');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/fluid_backup');
const PREVIEW = path.join(ROOT, 'gui_layouts/fluid_rework_preview.png');

// 动画参数：32 帧 x 2 tick = 64 tick（3.2s）整循环，与原版水一致
const FRAMES = 32;
const FRAMETIME = 2;
const MCMETA = JSON.stringify({ animation: { frametime: FRAMETIME, interpolate: true } }, null, 2) + '\n';

const CRC_TABLE = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1); t[n] = c; }
  return t;
})();
function crc32(buf) { let c = 0xFFFFFFFF; for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 255] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; }

function readPng(buf) {
  if (buf.readUInt32BE(0) !== 0x89504e47) throw Error('PNG header');
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
  if (depth !== 8 || (ctype !== 6 && ctype !== 2)) throw Error('expect RGBA8/RGB8');
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
    else if (ft === 3) { for (let x = 0; x < stride; x++) line[x] = (line[x] + (((x >= ch ? line[x - ch] : 0) + prev[x]) >> 1)) & 255; }
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

function writePng(width, height, px) {
  const raw = Buffer.alloc(height * (1 + width * 4));
  let o = 0;
  for (let y = 0; y < height; y++) { raw[o++] = 0; px.copy(raw, o, y * width * 4, (y + 1) * width * 4); o += width * 4; }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0); ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; ihdr[9] = 6;
  const chunk = (tag, data) => {
    const b = Buffer.alloc(12 + data.length);
    b.writeUInt32BE(data.length, 0); b.write(tag, 4, 'ascii'); data.copy(b, 8);
    b.writeUInt32BE(crc32(b.subarray(4, 8 + data.length)), 8 + data.length);
    return b;
  };
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]);
}

// 提取流体主色：出现次数最多的「非灰、非白」像素
function detectBase(px) {
  const hist = new Map();
  for (let i = 0; i < px.length; i += 4) {
    if (px[i + 3] === 0) continue;
    const r = px[i], g = px[i + 1], b = px[i + 2];
    const max = Math.max(r, g, b), min = Math.min(r, g, b);
    if (max - min < 18) continue;        // 灰阶（边框/刻线）跳过
    if (min > 235) continue;             // 近白高光跳过
    const k = (r << 16) | (g << 8) | b;
    hist.set(k, (hist.get(k) || 0) + 1);
  }
  let best = 0x6C4696, bestN = -1;
  for (const [k, n] of hist) if (n > bestN) { bestN = n; best = k; }
  return [(best >> 16) & 255, (best >> 8) & 255, best & 255];
}

// 无缝细节项：两组正弦相乘，周期整除 16，故 x/y 双向周期均为 16（替换非周期噪声，避免纵向接缝跳变）
function detail(x, y) {
  return Math.sin(TAU * (2 * x + 3 * y) / 16) * Math.sin(TAU * (3 * x - 2 * y) / 16);
}

const TAU = Math.PI * 2;

// still：缓波面；flow：竖向流纹。均以 16 为周期，单帧双向无缝。
// phase 为行波相位（0~TAU 循环），驱动正弦项平移，细节项固定以免逐帧闪烁。
function buildFluid(base, mode, phase) {
  const S = 16, px = Buffer.alloc(S * S * 4);
  const [br, bg, bb] = base;
  for (let y = 0; y < S; y++) {
    for (let x = 0; x < S; x++) {
      let d;
      if (mode === 'still') {
        // 缓波面：两组斜向正弦反向平移，形成缓慢起伏的液面
        d = 9 * Math.sin(TAU * (x + y) / 16 + phase)
          + 5 * Math.sin(TAU * (x - 2 * y) / 16 - phase)
          + 4 * detail(x, y);
      } else {
        // 竖向流纹：条纹本身不动，叠加一条向下推移的亮度带（相位 2 倍速）形成流动感
        const wob = 0.6 * Math.sin(TAU * y / 16 + 2 * phase);
        d = 10 * Math.sin(TAU * (4 * (x + wob)) / 16)
          + 8 * Math.sin(TAU * y / 16 - 2 * phase)
          + 5 * detail(x, y) + 5;
      }
      const o = (y * S + x) * 4;
      px[o] = Math.max(0, Math.min(255, Math.round(br + d)));
      px[o + 1] = Math.max(0, Math.min(255, Math.round(bg + d)));
      px[o + 2] = Math.max(0, Math.min(255, Math.round(bb + d)));
      px[o + 3] = 255;
    }
  }
  return px;
}

// 把 FRAMES 个 16x16 帧纵向拼成 16 x (16*FRAMES) 的动画条
function buildStrip(base, mode) {
  const strip = Buffer.alloc(16 * 16 * FRAMES * 4);
  for (let t = 0; t < FRAMES; t++) {
    buildFluid(base, mode, (t / FRAMES) * TAU).copy(strip, t * 16 * 16 * 4);
  }
  return strip;
}

function scale(src, w, h, f) {
  const out = Buffer.alloc(w * f * h * f * 4);
  for (let y = 0; y < h * f; y++) {
    const sy = Math.floor(y / f);
    for (let x = 0; x < w * f; x++) {
      const sx = Math.floor(x / f);
      src.copy(out, (y * w * f + x) * 4, (sy * w + sx) * 4, (sy * w + sx) * 4 + 4);
    }
  }
  return out;
}

function main() {
  fs.mkdirSync(BACKUP_DIR, { recursive: true });
  const names = fs.readdirSync(DIR).filter(f => f.endsWith('_still.png')).map(f => f.slice(0, -'_still.png'.length)).sort();
  const strips = new Map();
  for (const n of names) {
    const srcPath = path.join(DIR, n + '_still.png');
    const bakPath = path.join(BACKUP_DIR, n + '_still.png');
    if (!fs.existsSync(bakPath)) fs.copyFileSync(srcPath, bakPath);
    const bakFlow = path.join(BACKUP_DIR, n + '_flow.png');
    if (!fs.existsSync(bakFlow)) fs.copyFileSync(path.join(DIR, n + '_flow.png'), bakFlow);

    const base = detectBase(readPng(fs.readFileSync(bakPath)).px);
    const still = buildStrip(base, 'still');
    const flow = buildStrip(base, 'flow');
    fs.writeFileSync(path.join(DIR, n + '_still.png'), writePng(16, 16 * FRAMES, still));
    fs.writeFileSync(path.join(DIR, n + '_flow.png'), writePng(16, 16 * FRAMES, flow));
    fs.writeFileSync(path.join(DIR, n + '_still.png.mcmeta'), MCMETA);
    fs.writeFileSync(path.join(DIR, n + '_flow.png.mcmeta'), MCMETA);
    strips.set(n, { still, flow });
    console.log(n.padEnd(30) + ' base rgb(' + base.join(',') + ')  ' + FRAMES + ' frames');
  }

  // 预览：8 列（still 4 帧 + flow 4 帧）x N 行（每种流体一行），4x 放大
  const f = 4, cell = 16 * f + 4, cols = 8;
  const W = cols * cell + 4, H = names.length * cell + 4;
  const sheet = Buffer.alloc(W * H * 4);
  for (let i = 0; i < W * H; i++) { sheet[i * 4] = 26; sheet[i * 4 + 1] = 26; sheet[i * 4 + 2] = 30; sheet[i * 4 + 3] = 255; }
  const SAMPLE = [0, 8, 16, 24];
  names.forEach((n, row) => {
    const s = strips.get(n);
    ['still', 'flow'].forEach((mode, si) => {
      SAMPLE.forEach((t, k) => {
        const frame = s[mode].subarray(t * 16 * 16 * 4, (t + 1) * 16 * 16 * 4);
        const big = scale(frame, 16, 16, f);
        const ox = 4 + (si * 4 + k) * cell, oy = 4 + row * cell;
        for (let y = 0; y < 16 * f; y++) big.copy(sheet, ((oy + y) * W + ox) * 4, y * 16 * f * 4, (y + 1) * 16 * f * 4);
      });
    });
  });
  fs.writeFileSync(PREVIEW, writePng(W, H, sheet));
  console.log('共重制 ' + names.length * 2 + ' 张动画贴图（' + names.length + ' 种流体 x still/flow x ' + FRAMES + ' 帧）');
  console.log('预览: ' + path.relative(ROOT, PREVIEW));
  return 0;
}

process.exit(main());
