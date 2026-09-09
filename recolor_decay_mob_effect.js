// 衰竭（Decay）效果图标换色：复用衰竭家族同一套「暗紫 -> 亮紫」亮度色阶，
// 保证 MobEffect 图标与衰变方块/物品贴图识别色一致。
// 原理：按像素感知亮度 l = (299R + 587G + 114B) / 255000 映射到 RAMP，保留 alpha 与像素结构。
// 幂等：首次运行把原图备份到 gui_layouts/mob_effect_backup/，之后始终以备份为源，可反复执行。
// 用法：node recolor_decay_mob_effect.js
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const ASSET = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/mob_effect/decay.png');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/mob_effect_backup');
const BACKUP = path.join(BACKUP_DIR, 'decay.png');
const PREVIEW = path.join(ROOT, 'gui_layouts/decay_mob_effect_preview.png');

// 与 recolor_decay_vanilla.js 保持完全一致的紫调色阶
const RAMP = [
  [0.00, [26, 16, 42]],
  [0.28, [64, 40, 96]],
  [0.55, [108, 70, 150]],
  [0.80, [156, 114, 200]],
  [1.00, [214, 184, 240]],
];

const CRC_TABLE = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1); t[n] = c; }
  return t;
})();
function crc32(buf) { let c = 0xFFFFFFFF; for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 255] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; }

// 最小 PNG 解码：8 位 RGBA/RGB/灰度/灰度+A/调色板
function readPng(buf) {
  if (buf.readUInt32BE(0) !== 0x89504e47) throw Error('PNG header');
  let pos = 8, width = 0, height = 0, depth = 0, ctype = 0, palette = null, trns = null;
  const idat = [];
  while (pos + 8 <= buf.length) {
    const len = buf.readUInt32BE(pos);
    const tag = buf.toString('ascii', pos + 4, pos + 8);
    const chunk = buf.subarray(pos + 8, pos + 8 + len);
    pos += 12 + len;
    if (tag === 'IHDR') { width = chunk.readUInt32BE(0); height = chunk.readUInt32BE(4); depth = chunk[8]; ctype = chunk[9]; }
    else if (tag === 'PLTE') { palette = []; for (let i = 0; i < chunk.length; i += 3) palette.push([chunk[i], chunk[i + 1], chunk[i + 2]]); }
    else if (tag === 'tRNS') trns = chunk;
    else if (tag === 'IDAT') idat.push(chunk);
    else if (tag === 'IEND') break;
  }
  if (depth !== 8) throw Error('仅支持 8 位深, got ' + depth);
  const ch = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[ctype];
  if (!ch) throw Error('不支持的颜色类型 ' + ctype);
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
      if (ctype === 6) { px[o++] = line[s]; px[o++] = line[s + 1]; px[o++] = line[s + 2]; px[o++] = line[s + 3]; }
      else if (ctype === 2) { px[o++] = line[s]; px[o++] = line[s + 1]; px[o++] = line[s + 2]; px[o++] = 255; }
      else if (ctype === 0) { px[o++] = line[s]; px[o++] = line[s]; px[o++] = line[s]; px[o++] = 255; }
      else if (ctype === 4) { px[o++] = line[s]; px[o++] = line[s]; px[o++] = line[s]; px[o++] = line[s + 1]; }
      else { const p = palette[line[s]]; px[o++] = p[0]; px[o++] = p[1]; px[o++] = p[2]; px[o++] = (trns && line[s] < trns.length) ? trns[line[s]] : 255; }
    }
    prev = line;
  }
  return { width, height, px };
}

// 编码 RGBA8 PNG（每行 filter 0）
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

// 亮度 -> 紫调色阶，保留 alpha
function recolor(px) {
  const out = Buffer.alloc(px.length);
  for (let i = 0; i < px.length; i += 4) {
    const r = px[i], g = px[i + 1], b = px[i + 2], a = px[i + 3];
    if (a === 0) { out[i] = r; out[i + 1] = g; out[i + 2] = b; out[i + 3] = 0; continue; }
    const l = (299 * r + 587 * g + 114 * b) / 255000.0;
    let lo = RAMP[0], hi = RAMP[RAMP.length - 1];
    for (let k = 0; k < RAMP.length - 1; k++) { if (l <= RAMP[k + 1][0]) { lo = RAMP[k]; hi = RAMP[k + 1]; break; } }
    const span = (hi[0] - lo[0]) || 1.0, t = (l - lo[0]) / span;
    out[i] = Math.round(lo[1][0] + (hi[1][0] - lo[1][0]) * t);
    out[i + 1] = Math.round(lo[1][1] + (hi[1][1] - lo[1][1]) * t);
    out[i + 2] = Math.round(lo[1][2] + (hi[1][2] - lo[1][2]) * t);
    out[i + 3] = a;
  }
  return out;
}

// 最近邻放大，便于人眼复核
function scale(src, w, h, factor) {
  const out = Buffer.alloc(w * factor * h * factor * 4);
  for (let y = 0; y < h * factor; y++) {
    const sy = Math.floor(y / factor);
    for (let x = 0; x < w * factor; x++) {
      const sx = Math.floor(x / factor);
      src.copy(out, (y * w * factor + x) * 4, (sy * w + sx) * 4, (sy * w + sx) * 4 + 4);
    }
  }
  return out;
}

function main() {
  if (!fs.existsSync(BACKUP)) {
    fs.mkdirSync(BACKUP_DIR, { recursive: true });
    fs.copyFileSync(ASSET, BACKUP);
    console.log('已备份原图 -> ' + path.relative(ROOT, BACKUP));
  }
  const img = readPng(fs.readFileSync(BACKUP));
  const out = recolor(img.px);
  let changed = 0;
  for (let i = 0; i < img.px.length; i += 4) {
    if (img.px[i] !== out[i] || img.px[i + 1] !== out[i + 1] || img.px[i + 2] !== out[i + 2]) changed++;
  }
  fs.writeFileSync(ASSET, writePng(img.width, img.height, out));
  fs.mkdirSync(path.dirname(PREVIEW), { recursive: true });
  const factor = 12;
  fs.writeFileSync(PREVIEW, writePng(img.width * factor, img.height * factor, scale(out, img.width, img.height, factor)));
  console.log('decay.png ' + img.width + 'x' + img.height + ' 换色完成，改写像素 ' + changed);
  console.log('预览: ' + path.relative(ROOT, PREVIEW));
  return 0;
}

process.exit(main());
