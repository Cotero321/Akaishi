// 衰竭（Decay）家族材质重置：读取原版贴图并按紫调色阶换色。
// 原理：原版贴图多为低饱和/灰阶，直接调色相无法得到紫色；因此按像素感知亮度
// l = (299R + 587G + 114B) / 255000 映射到「暗紫 -> 亮紫」色阶，保留 alpha 与原图像素结构。
// 用法：
//   node recolor_decay_vanilla.js                // 默认 32x32，写入资源目录
//   node recolor_decay_vanilla.js --size 16      // 保持原版 16x16
//   node recolor_decay_vanilla.js --preview-only // 只输出预览图，不覆盖资源
//   node recolor_decay_vanilla.js --jar <path>   // 指定客户端 jar
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const ASSETS = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures');
const PREVIEW = path.join(ROOT, 'gui_layouts');

// 输出资源 -> 原版贴图（相对 assets/minecraft/textures/）
const MAP = [
  ['block/akaishi_decay_stone.png', 'block/stone.png'],
  ['block/akaishi_decay_cobblestone.png', 'block/cobblestone.png'],
  ['block/akaishi_decay_stone_bricks.png', 'block/stone_bricks.png'],
  ['block/akaishi_decay_sand.png', 'block/sand.png'],
  ['block/akaishi_decay_soil.png', 'block/dirt.png'],
  ['block/akaishi_decay_gravel.png', 'block/gravel.png'],
  ['block/akaishi_decay_grass_block_top.png', 'block/grass_block_top.png'],
  ['block/akaishi_decay_grass_block_side.png', 'block/grass_block_side.png'],
  ['block/akaishi_decay_log.png', 'block/oak_log.png'],
  ['block/akaishi_decay_log_top.png', 'block/oak_log_top.png'],
  ['block/akaishi_decay_planks.png', 'block/oak_planks.png'],
  ['block/akaishi_decay_door_top.png', 'block/oak_door_top.png'],
  ['block/akaishi_decay_door_bottom.png', 'block/oak_door_bottom.png'],
  ['block/akaishi_decay_trapdoor.png', 'block/oak_trapdoor.png'],
  ['item/akaishi_decay_door.png', 'item/oak_door.png'],
  ['block/akaishi_decay_purifier_side.png', 'block/deepslate_tiles.png'],
  ['block/akaishi_decay_purifier_top.png', 'block/polished_deepslate.png'],
  ['block/akaishi_decay_purifier_bottom.png', 'block/deepslate_bricks.png'],
];

// 亮度 -> 紫调色阶（0.0 最暗，1.0 最亮）
const RAMP = [
  [0.00, [26, 16, 42]],
  [0.28, [64, 40, 96]],
  [0.55, [108, 70, 150]],
  [0.80, [156, 114, 200]],
  [1.00, [214, 184, 240]],
];

const JAR_CANDIDATES = [
  path.join(process.env.USERPROFILE || '', '.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar'),
  path.join(process.env.USERPROFILE || '', '.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraft/client/1.20.1/client-1.20.1-extra.jar'),
];

const CRC_TABLE = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1); t[n] = c; }
  return t;
})();
function crc32(buf) { let c = 0xFFFFFFFF; for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 255] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; }

// 从 zip 读取单个条目（支持 stored / deflate）
function zipRead(jarPath, entryName) {
  const buf = fs.readFileSync(jarPath);
  let eocd = -1;
  for (let i = buf.length - 22; i >= 0 && i >= buf.length - 65557; i--) { if (buf.readUInt32LE(i) === 0x06054b50) { eocd = i; break; } }
  if (eocd < 0) throw Error('EOCD not found: ' + jarPath);
  const count = buf.readUInt16LE(eocd + 10);
  let off = buf.readUInt32LE(eocd + 16);
  for (let n = 0; n < count; n++) {
    if (buf.readUInt32LE(off) !== 0x02014b50) throw Error('bad central directory');
    const method = buf.readUInt16LE(off + 10);
    const compSize = buf.readUInt32LE(off + 20);
    const nameLen = buf.readUInt16LE(off + 28);
    const extraLen = buf.readUInt16LE(off + 30);
    const commentLen = buf.readUInt16LE(off + 32);
    const localOff = buf.readUInt32LE(off + 42);
    const name = buf.toString('utf8', off + 46, off + 46 + nameLen);
    if (name === entryName) {
      const lNameLen = buf.readUInt16LE(localOff + 26);
      const lExtraLen = buf.readUInt16LE(localOff + 28);
      const start = localOff + 30 + lNameLen + lExtraLen;
      const data = buf.subarray(start, start + compSize);
      return method === 0 ? Buffer.from(data) : zlib.inflateRawSync(data);
    }
    off += 46 + nameLen + extraLen + commentLen;
  }
  throw Error('entry not found: ' + entryName);
}

// 解码 PNG 为 {width,height,px(RGBA8)}，支持位深 1/2/4/8
function readPng(buf) {
  if (buf.subarray(0, 8).toString('hex') !== '89504e470d0a1a0a') throw Error('not a png');
  let pos = 8, width = 0, height = 0, depth = 0, ctype = 0, palette = null, trns = null;
  const idat = [];
  while (pos + 8 <= buf.length) {
    const len = buf.readUInt32BE(pos);
    const tag = buf.toString('ascii', pos + 4, pos + 8);
    const chunk = buf.subarray(pos + 8, pos + 8 + len);
    pos += 12 + len;
    if (tag === 'IHDR') {
      width = chunk.readUInt32BE(0); height = chunk.readUInt32BE(4); depth = chunk[8]; ctype = chunk[9];
      if (chunk[10] !== 0 || chunk[11] !== 0 || chunk[12] !== 0) throw Error('unsupported png variant');
    } else if (tag === 'PLTE') {
      palette = []; for (let i = 0; i < chunk.length; i += 3) palette.push([chunk[i], chunk[i + 1], chunk[i + 2]]);
    } else if (tag === 'tRNS') { trns = Array.from(chunk); }
    else if (tag === 'IDAT') { idat.push(chunk); }
    else if (tag === 'IEND') { break; }
  }
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[ctype];
  const bits = channels * depth;
  const bpp = Math.max(1, Math.floor(bits / 8));
  const stride = Math.ceil(width * bits / 8);
  const lines = [];
  let prev = Buffer.alloc(stride), i = 0;
  for (let y = 0; y < height; y++) {
    const ft = raw[i++];
    const line = Buffer.from(raw.subarray(i, i + stride)); i += stride;
    if (ft === 1) { for (let x = bpp; x < stride; x++) line[x] = (line[x] + line[x - bpp]) & 255; }
    else if (ft === 2) { for (let x = 0; x < stride; x++) line[x] = (line[x] + prev[x]) & 255; }
    else if (ft === 3) { for (let x = 0; x < stride; x++) { const a = x >= bpp ? line[x - bpp] : 0; line[x] = (line[x] + ((a + prev[x]) >> 1)) & 255; } }
    else if (ft === 4) {
      for (let x = 0; x < stride; x++) {
        const a = x >= bpp ? line[x - bpp] : 0, b = prev[x], c = x >= bpp ? prev[x - bpp] : 0;
        const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
        const pr = (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c);
        line[x] = (line[x] + pr) & 255;
      }
    }
    lines.push(line); prev = line;
  }
  const px = Buffer.alloc(width * height * 4);
  let o = 0;
  if (depth === 8) {
    for (const line of lines) {
      if (ctype === 6) { for (let k = 0; k < line.length; k += 4) { px[o++] = line[k]; px[o++] = line[k + 1]; px[o++] = line[k + 2]; px[o++] = line[k + 3]; } }
      else if (ctype === 2) { for (let k = 0; k < line.length; k += 3) { px[o++] = line[k]; px[o++] = line[k + 1]; px[o++] = line[k + 2]; px[o++] = 255; } }
      else if (ctype === 0) { for (let k = 0; k < line.length; k++) { px[o++] = line[k]; px[o++] = line[k]; px[o++] = line[k]; px[o++] = 255; } }
      else if (ctype === 4) { for (let k = 0; k < line.length; k += 2) { px[o++] = line[k]; px[o++] = line[k]; px[o++] = line[k]; px[o++] = line[k + 1]; } }
      else if (ctype === 3) { for (let k = 0; k < line.length; k++) { const p = palette[line[k]]; px[o++] = p[0]; px[o++] = p[1]; px[o++] = p[2]; px[o++] = (trns && line[k] < trns.length) ? trns[line[k]] : 255; } }
    }
  } else {
    const maxv = (1 << depth) - 1;
    for (const line of lines) {
      const vals = [];
      for (const byte of line) for (let k = 0; k < 8 / depth; k++) vals.push((byte >> (8 - depth * (k + 1))) & maxv);
      for (let k = 0; k < width; k++) {
        const v = vals[k];
        if (ctype === 3) { const p = palette[v]; px[o++] = p[0]; px[o++] = p[1]; px[o++] = p[2]; px[o++] = (trns && v < trns.length) ? trns[v] : 255; }
        else { const g = Math.floor(v * 255 / maxv); px[o++] = g; px[o++] = g; px[o++] = g; px[o++] = 255; }
      }
    }
  }
  return { width, height, px };
}

// 编码为 RGBA8 PNG（每行 filter 0）
function writePng(width, height, px) {
  const raw = Buffer.alloc(height * (1 + width * 4));
  let o = 0;
  for (let y = 0; y < height; y++) { raw[o++] = 0; px.copy(raw, o, y * width * 4, (y + 1) * width * 4); o += width * 4; }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0); ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; ihdr[9] = 6; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  const chunk = (tag, data) => {
    const b = Buffer.alloc(12 + data.length);
    b.writeUInt32BE(data.length, 0); b.write(tag, 4, 'ascii'); data.copy(b, 8);
    b.writeUInt32BE(crc32(b.subarray(4, 8 + data.length)), 8 + data.length);
    return b;
  };
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]);
}

// 按感知亮度映射到紫调色阶，保留 alpha
function recolor(width, height, px) {
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

// 最近邻缩放，保持原版像素结构
function resize(width, height, px, size) {
  if (size === width && size === height) return { width, height, px };
  const out = Buffer.alloc(size * size * 4);
  let o = 0;
  for (let y = 0; y < size; y++) {
    const sy = Math.min(height - 1, Math.floor(y * height / size));
    for (let x = 0; x < size; x++) {
      const sx = Math.min(width - 1, Math.floor(x * width / size));
      const s = (sy * width + sx) * 4;
      out[o++] = px[s]; out[o++] = px[s + 1]; out[o++] = px[s + 2]; out[o++] = px[s + 3];
    }
  }
  return { width: size, height: size, px: out };
}

function montage(tiles, size, outPath) {
  const cols = 6, rows = Math.ceil(tiles.length / cols), pad = 4, cell = size + pad;
  const w = cols * cell + pad, h = rows * cell + pad;
  const canvas = Buffer.alloc(w * h * 4);
  for (let i = 0; i < w * h; i++) { canvas[i * 4] = 28; canvas[i * 4 + 1] = 28; canvas[i * 4 + 2] = 32; canvas[i * 4 + 3] = 255; }
  tiles.forEach((tile, i) => {
    const ox = pad + (i % cols) * cell, oy = pad + Math.floor(i / cols) * cell;
    for (let y = 0; y < size; y++) tile.px.copy(canvas, ((oy + y) * w + ox) * 4, y * size * 4, (y + 1) * size * 4);
  });
  fs.writeFileSync(outPath, writePng(w, h, canvas));
}

function findJar(explicit) {
  if (explicit) return fs.existsSync(explicit) ? explicit : null;
  for (const p of JAR_CANDIDATES) if (fs.existsSync(p)) return p;
  return null;
}

function main() {
  const args = process.argv.slice(2);
  let jar = null, size = 32, previewOnly = false;
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--jar') jar = args[++i];
    else if (args[i] === '--size') size = parseInt(args[++i], 10);
    else if (args[i] === '--preview-only') previewOnly = true;
  }
  jar = findJar(jar);
  if (!jar) { console.error('未找到客户端 jar，请用 --jar 指定路径'); return 1; }
  console.log('源贴图 jar: ' + jar);

  const tiles = [];
  for (const [relOut, relSrc] of MAP) {
    const img = readPng(zipRead(jar, 'assets/minecraft/textures/' + relSrc));
    const scaled = resize(img.width, img.height, recolor(img.width, img.height, img.px), size);
    tiles.push({ relOut, px: scaled.px });
    const dest = path.join(ASSETS, relOut);
    if (!previewOnly) {
      fs.mkdirSync(path.dirname(dest), { recursive: true });
      fs.writeFileSync(dest, writePng(scaled.width, scaled.height, scaled.px));
    }
    console.log('  ' + relOut.padEnd(46) + ' <- ' + relSrc + ' (' + scaled.width + 'x' + scaled.height + ')');
  }

  fs.mkdirSync(PREVIEW, { recursive: true });
  const previewPath = path.join(PREVIEW, 'decay_vanilla_recolor_preview.png');
  montage(tiles, size, previewPath);
  console.log('预览: ' + previewPath);

  const known = new Set(MAP.map(m => path.basename(m[0])));
  const blockDir = path.join(ASSETS, 'block');
  const orphans = fs.existsSync(blockDir) ? fs.readdirSync(blockDir).filter(n => n.startsWith('akaishi_decay_') && !known.has(n)).sort() : [];
  if (orphans.length) {
    console.log('未纳入换色的 decay 孤儿贴图（无模型引用，建议清理）:');
    orphans.forEach(n => console.log('  ' + n));
  }
  return 0;
}

process.exit(main());
