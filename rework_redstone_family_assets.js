// 赤石家族五件材质重做（3 物品 + 2 方块，全部 32×32）。
//   akaishi_crystal            赤石晶        —— 斜置菱形晶体（左受光 / 中轴棱 / 右背光）
//   raw_akaishi_block          粗制赤石块    —— 对标原版 raw_iron_block（16×16 七档均匀噪点，×2 到 32）
//   akaishi_essence            赤石精华      —— 碎晶粒密堆
//   akaishi_essence_compressed 浓缩赤石精华  —— 多颗晶柱密堆
//   akaishi_essence_block      浓缩赤石精华块 —— 对标原版 redstone_block（1px 深色外框 + 由中心向外放射渐亮）
//
// 为什么这样改：
//   1) 现状配色不是赤石 —— 三件物品主色是粉红 #DB6B79 配 暗蓝灰 #192133，粗制赤石块是土黄。
//      统一改为「粉红主色 + 深红暗部」，去掉蓝色分量，与既有赤石矿（鲑红）同族。
//   2) 两块方块照原版对标重排结构：raw_iron_block 是七档均匀噪点（无中心/边缘组织），
//      redstone_block 是 1px 最暗外框 + 内圈最亮 + 中心最暗、由中心向外放射渐亮。
//      原版都在 16×16 生成后 ×2 铺满 32×32，与底材同粒度，避免「细密噪声贴在粗格上」的割裂。
//   3) 三件物品改用晶体切面语言（原版 amethyst_shard 的语法）：
//      最外圈最暗 → 左侧受光 → 中轴亮棱 → 右侧背光，逐级递进（碎粒 < 密堆柱 < 单晶）。
//   4) 色阶补足到 15~30 档（现状仅 6~9 档，是「发平发糊」主因）。
//
// 注：两块方块原由 rework_ore_crystal_assets.ps1 的 DrawRawBlock/DrawEssenceBlock 生成，
//     现已迁到本脚本统一出图，PS 脚本中的对应生成点已移除，避免双源互相覆盖。
// 幂等：首次运行备份原图到 gui_layouts/redstone_family_backup/，之后始终以备份为源。
// 用法：node rework_redstone_family_assets.js            备份 + 写五张贴图 + 出三行对照图
//       node rework_redstone_family_assets.js --preview  仅出对照图，不写贴图
//       node rework_redstone_family_assets.js --jar <p>  指定客户端 jar（取原版参照）
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const ITEM_DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/item');
const BLOCK_DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/block');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/redstone_family_backup');
const PREVIEW = path.join(ROOT, 'gui_layouts/redstone_family_preview.png');
const PREVIEW_ONLY = process.argv.includes('--preview');

const W = 32, H = 32;

// ---- 统一色板：粉红主色 + 深红暗部（不含蓝色分量）----
const P = {
  deep: [88, 20, 38],     // 深红暗部
  dark: [140, 40, 64],
  mid: [182, 64, 90],
  base: [219, 107, 121],  // 粉红主色
  light: [238, 150, 162],
  hi: [255, 198, 204],
  hi2: [255, 236, 238],
};
const OUTLINE = [38, 14, 22];  // 物品描边：暗栗色（同族暗部，非蓝黑）

// 粗制赤石块七档（对标 raw_iron_block 的明度阶梯，色相换成哑光玫瑰红＝原矿质感）
const RAW = [
  [100, 54, 62], [126, 72, 80], [154, 94, 102], [186, 126, 134],
  [214, 158, 164], [236, 196, 200], [252, 228, 230],
];
// 各档占比（照原版实测约 8/17/22/24/17/9/2%，用分位数动态取阈值，保证七档全部命中）
const RAW_QUANT = [0.08, 0.25, 0.47, 0.71, 0.88, 0.97];
// 浓缩赤石精华块五档（对标 redstone_block 的 5 色，由核心向外增亮）
const ESS = [[92, 18, 36], [134, 36, 58], [180, 68, 94], [218, 110, 130], [248, 158, 172]];

// ---- PNG 读写（filter 0 / IHDR depth=8 ctype=6 / deflate 9）----
const CRC_TABLE = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1); t[n] = c; }
  return t;
})();
function crc32(buf) { let c = 0xFFFFFFFF; for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 255] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; }
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
function decodePng(buf) {
  if (buf.subarray(0, 8).toString('hex') !== '89504e470d0a1a0a') throw Error('not a png');
  let pos = 8, width = 0, height = 0, depth = 0, ctype = 0, palette = null, trns = null;
  const idat = [];
  while (pos + 8 <= buf.length) {
    const len = buf.readUInt32BE(pos), tag = buf.toString('ascii', pos + 4, pos + 8), chunk = buf.subarray(pos + 8, pos + 8 + len);
    pos += 12 + len;
    if (tag === 'IHDR') { width = chunk.readUInt32BE(0); height = chunk.readUInt32BE(4); depth = chunk[8]; ctype = chunk[9]; }
    else if (tag === 'PLTE') { palette = []; for (let i = 0; i < chunk.length; i += 3) palette.push([chunk[i], chunk[i + 1], chunk[i + 2]]); }
    else if (tag === 'tRNS') trns = Array.from(chunk);
    else if (tag === 'IDAT') idat.push(chunk);
    else if (tag === 'IEND') break;
  }
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[ctype], bits = channels * depth;
  const bpp = Math.max(1, Math.floor(bits / 8)), stride = Math.ceil(width * bits / 8), lines = [];
  let prev = Buffer.alloc(stride), i = 0;
  for (let y = 0; y < height; y++) {
    const ft = raw[i++], line = Buffer.from(raw.subarray(i, i + stride)); i += stride;
    if (ft === 1) { for (let x = bpp; x < stride; x++) line[x] = (line[x] + line[x - bpp]) & 255; }
    else if (ft === 2) { for (let x = 0; x < stride; x++) line[x] = (line[x] + prev[x]) & 255; }
    else if (ft === 3) { for (let x = 0; x < stride; x++) { const a = x >= bpp ? line[x - bpp] : 0; line[x] = (line[x] + ((a + prev[x]) >> 1)) & 255; } }
    else if (ft === 4) for (let x = 0; x < stride; x++) {
      const a = x >= bpp ? line[x - bpp] : 0, b = prev[x], c = x >= bpp ? prev[x - bpp] : 0;
      const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
      line[x] = (line[x] + ((pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c))) & 255;
    }
    lines.push(line); prev = line;
  }
  const px = Buffer.alloc(width * height * 4); let o = 0;
  if (depth === 8) {
    for (const line of lines) {
      if (ctype === 6) for (let k = 0; k < line.length; k += 4) { px[o++] = line[k]; px[o++] = line[k + 1]; px[o++] = line[k + 2]; px[o++] = line[k + 3]; }
      else if (ctype === 2) for (let k = 0; k < line.length; k += 3) { px[o++] = line[k]; px[o++] = line[k + 1]; px[o++] = line[k + 2]; px[o++] = 255; }
      else if (ctype === 3) for (let k = 0; k < line.length; k++) { const p = palette[line[k]]; px[o++] = p[0]; px[o++] = p[1]; px[o++] = p[2]; px[o++] = (trns && line[k] < trns.length) ? trns[line[k]] : 255; }
      else if (ctype === 0) for (let k = 0; k < line.length; k++) { px[o++] = line[k]; px[o++] = line[k]; px[o++] = line[k]; px[o++] = 255; }
      else if (ctype === 4) for (let k = 0; k < line.length; k += 2) { px[o++] = line[k]; px[o++] = line[k]; px[o++] = line[k]; px[o++] = line[k + 1]; }
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
function readPng(file) { return decodePng(fs.readFileSync(file)); }
function zipRead(jarPath, entryName) {
  const buf = fs.readFileSync(jarPath);
  let eocd = -1;
  for (let i = buf.length - 22; i >= 0 && i >= buf.length - 65557; i--) if (buf.readUInt32LE(i) === 0x06054b50) { eocd = i; break; }
  if (eocd < 0) throw Error('EOCD not found: ' + jarPath);
  const count = buf.readUInt16LE(eocd + 10);
  let off = buf.readUInt32LE(eocd + 16);
  for (let n = 0; n < count; n++) {
    const method = buf.readUInt16LE(off + 10), compSize = buf.readUInt32LE(off + 20);
    const nameLen = buf.readUInt16LE(off + 28), extraLen = buf.readUInt16LE(off + 30), commentLen = buf.readUInt16LE(off + 32);
    const localOff = buf.readUInt32LE(off + 42), name = buf.toString('utf8', off + 46, off + 46 + nameLen);
    if (name === entryName) {
      const lNameLen = buf.readUInt16LE(localOff + 26), lExtraLen = buf.readUInt16LE(localOff + 28);
      const start = localOff + 30 + lNameLen + lExtraLen, data = buf.subarray(start, start + compSize);
      return method === 0 ? Buffer.from(data) : zlib.inflateRawSync(data);
    }
    off += 46 + nameLen + extraLen + commentLen;
  }
  throw Error('entry not found: ' + entryName);
}
const JAR_CANDIDATES = [
  path.join(process.env.USERPROFILE || '', '.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar'),
  path.join(process.env.USERPROFILE || '', '.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraft/client/1.20.1/client-1.20.1-extra.jar'),
];
let JAR = null;
function resizeNearest(img, dw, dh) {
  const out = Buffer.alloc(dw * dh * 4);
  for (let y = 0; y < dh; y++) for (let x = 0; x < dw; x++) {
    const sx = Math.min(img.width - 1, Math.floor(x * img.width / dw));
    const sy = Math.min(img.height - 1, Math.floor(y * img.height / dh));
    const s = (sy * img.width + sx) * 4, d = (y * dw + x) * 4;
    out[d] = img.px[s]; out[d + 1] = img.px[s + 1]; out[d + 2] = img.px[s + 2]; out[d + 3] = img.px[s + 3];
  }
  return out;
}
// 从 client.jar 取原版贴图（relative 形如 'item/amethyst_shard' 或 'block/redstone_block'）
function vanillaTex(relative) {
  if (!JAR) {
    const arg = process.argv.indexOf('--jar');
    JAR = arg >= 0 ? process.argv[arg + 1] : JAR_CANDIDATES.find(p => fs.existsSync(p));
    if (!JAR) throw Error('未找到 1.20.1 client.jar，请用 --jar <path> 指定');
  }
  return decodePng(zipRead(JAR, 'assets/minecraft/textures/' + relative + '.png'));
}

// ---- 确定性随机（同参数必然同结果，保证幂等）----
function hash(x, y, s) {
  let h = Math.imul(x | 0, 374761393) ^ Math.imul(y | 0, 668265263) ^ Math.imul(s | 0, 1274126177);
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return ((h ^ (h >>> 16)) >>> 0) / 4294967296;
}
function mulberry32(a) {
  return function () {
    a |= 0; a = (a + 0x6D2B79F5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
function valueNoise(x, y, cell, seed) {
  const gx = x / cell, gy = y / cell;
  const x0 = Math.floor(gx), y0 = Math.floor(gy), fx = gx - x0, fy = gy - y0;
  const sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
  const n00 = hash(x0, y0, seed), n10 = hash(x0 + 1, y0, seed);
  const n01 = hash(x0, y0 + 1, seed), n11 = hash(x0 + 1, y0 + 1, seed);
  const a = n00 + (n10 - n00) * sx, b = n01 + (n11 - n01) * sx;
  return a + (b - a) * sy;
}

// ---- 通用像素工具（尺寸参数化，物品 32 与方块 16 共用）----
function put(px, size, x, y, c) { if (x < 0 || y < 0 || x >= size || y >= size) return; const o = (y * size + x) * 4; px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; px[o + 3] = 255; }
function mark(mask, size, x, y) { if (x < 0 || y < 0 || x >= size || y >= size) return; mask[y * size + x] = 1; }
function outline(px, mask, size, c) {
  const add = [];
  for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
    if (mask[y * size + x]) continue;
    for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
      const nx = x + dx, ny = y + dy;
      if (nx >= 0 && ny >= 0 && nx < size && ny < size && mask[ny * size + nx]) { add.push([x, y]); break; }
    }
  }
  for (const [x, y] of add) put(px, size, x, y, c);
}
// 细微颗粒：打散死平色，让色阶自然分层
function grain(px, mask, size, amp) {
  for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
    if (!mask[y * size + x]) continue;
    const o = (y * size + x) * 4, n = (((hash(x, y, 977) * 5) | 0) - 2) * amp;
    px[o] = Math.max(0, Math.min(255, px[o] + n));
    px[o + 1] = Math.max(0, Math.min(255, px[o + 1] + n));
    px[o + 2] = Math.max(0, Math.min(255, px[o + 2] + n));
  }
}

// 晶柱：在旋转坐标系里画一根菱形晶体。ly 为晶轴、lx 为横向；
// 宽度按 sin 曲线两头收尖（指数 0.85 ≈ 略外凸的菱形），着色照原版语法左亮右暗、中轴亮棱。
function shard(px, mask, cx, cy, halfH, halfW, deg) {
  const rad = deg * Math.PI / 180, cos = Math.cos(rad), sin = Math.sin(rad);
  const R = Math.ceil(halfH + halfW) + 2;
  for (let y = Math.floor(cy - R); y <= Math.ceil(cy + R); y++) {
    for (let x = Math.floor(cx - R); x <= Math.ceil(cx + R); x++) {
      if (x < 0 || y < 0 || x >= W || y >= H) continue;
      const dx = x - cx, dy = y - cy;
      const lx = dx * cos + dy * sin, ly = -dx * sin + dy * cos;
      if (Math.abs(ly) > halfH) continue;
      const v = (ly + halfH) / (halfH * 2);
      // 中段直边 + 两端 1/3 收尖 → 六棱柱状晶面（纯 sin 曲线太圆钝，不像晶体）
      const w = halfW * Math.min(1, Math.min(v, 1 - v) * 3.2);
      if (w < 0.6) continue;
      const nx = lx / w;
      if (Math.abs(nx) > 1) continue;
      let c;
      if (Math.abs(nx) > 0.84) c = P.deep;          // 最外圈最暗
      else if (nx < -0.5) c = P.hi;                 // 左侧受光
      else if (nx < -0.16) c = P.light;
      else if (nx <= 0.14) c = P.base;              // 中轴棱（原版中轴高光位）
      else if (nx <= 0.5) c = P.mid;
      else c = P.dark;                              // 右侧背光
      put(px, W, x, y, c); mark(mask, W, x, y);
    }
  }
}
// 晶面高光：沿晶轴中段点两颗亮核，模拟切面反光
function sparkle(px, x, y) { put(px, W, x, y, P.hi2); put(px, W, x + 1, y, P.hi2); put(px, W, x, y + 1, P.hi2); }
// 斑驳：用低频道值噪声在晶面上叠深色包裹体与浅色亮斑，破掉整面死平色。
// amt 控制强度（赤石晶重斑驳、精华保持洁净），cell 越小斑块越细碎。
function mottle(px, mask, size, seed, amt, cell) {
  for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
    if (!mask[y * size + x]) continue;
    const n = valueNoise(x, y, cell, seed), o = (y * size + x) * 4;
    if (n > 0.58) {                       // 深色包裹体
      const t = Math.min(1, (n - 0.58) / 0.24) * 0.85 * amt;
      for (let k = 0; k < 3; k++) px[o + k] = Math.round(px[o + k] * (1 - t) + P.deep[k] * t);
    } else if (n < 0.42) {                // 浅色亮斑
      const t = Math.min(1, (0.42 - n) / 0.24) * 0.6 * amt;
      for (let k = 0; k < 3; k++) px[o + k] = Math.round(px[o + k] * (1 - t) + P.hi2[k] * t);
    }
  }
}

// 赤石晶：三晶簇（左右小晶在后、主晶压前），重斑驳＝未经提纯的原矿晶簇
function drawCrystal() {
  const px = Buffer.alloc(W * H * 4), mask = new Uint8Array(W * H);
  shard(px, mask, 8, 22, 7.0, 4.4, -36);     // 左小晶
  shard(px, mask, 24, 22, 6.2, 4.0, 22);     // 右小晶
  shard(px, mask, 16, 16, 12.0, 7.2, -20);   // 主晶
  sparkle(px, 13, 12);
  mottle(px, mask, W, 313, 1.0, 3.4);
  mottle(px, mask, W, 319, 0.7, 6.5);        // 第二层大斑，叠加出成片包体
  outline(px, mask, W, OUTLINE);
  grain(px, mask, W, 6);
  return px;
}
// 赤石精华：单块晶体（一颗完整晶柱，洁净无包体，与晶簇区分）
function drawEssence() {
  const px = Buffer.alloc(W * H * 4), mask = new Uint8Array(W * H);
  shard(px, mask, 16, 16, 13.5, 8.0, -16);
  sparkle(px, 13, 11);
  outline(px, mask, W, OUTLINE);
  grain(px, mask, W, 5);
  return px;
}
// 浓缩赤石精华：照原版 flint 的斜向打制石片——形与明暗逐格对位，只换粉红家族配色
function drawCompressed() {
  const S = 16;
  // 原版 item/flint 像素形态（'.'透明 '#'暗 '+'中 ':'亮 '-'高光），按实拍逐字符照抄
  const ART = [
    '................', '................', '........###.....', '.......#+###....',
    '......#++++#....', '.....##+++:+#...', '....##+++-++#...', '...#++++-++###..',
    '..#+##+:++####..', '..####:++####+#.', '..###+#+####+:#.', '..##+######+:#..',
    '...######+++#...', '....####+++#....', '.....######.....', '................',
  ];
  // 燧石 4 符号 → 宝石家族配色：深红暗部 / 玫红主色 / 亮粉 / 近白镜面反光
  const TONE = { '#': [156, 54, 84], '+': [212, 116, 144], ':': [242, 168, 190], '-': [255, 234, 242] };
  const CORE = [104, 22, 52];                            // 内核沉红，保住原矿厚重感
  const px = Buffer.alloc(S * S * 4), mask = new Uint8Array(S * S);
  for (let y = 0; y < S; y++) for (let x = 0; x < S; x++) {
    const s = ART[y][x];
    if (s === '.') continue;
    let c = TONE[s];
    // 四邻皆实的暗部像素压深：复现燧石"外缘过渡、中心沉暗"的立体分层
    if (s === '#') {
      const solid = [[1, 0], [-1, 0], [0, 1], [0, -1]].every(([dx, dy]) => {
        const nx = x + dx, ny = y + dy;
        return nx >= 0 && nx < S && ny >= 0 && ny < S && ART[ny][nx] !== '.';
      });
      if (solid) c = CORE;
    } else if (s === '+') {
      // 紧贴高光条的中间调提亮，让镜面反光成片而非单点 —— 宝石感的关键
      const glint = [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => {
        const nx = x + dx, ny = y + dy;
        return nx >= 0 && nx < S && ny >= 0 && ny < S && ART[ny][nx] === '-';
      });
      if (glint) c = TONE[':'];
    }
    put(px, S, x, y, c); mark(mask, S, x, y);
  }
  outline(px, mask, S, OUTLINE);
  grain(px, mask, S, 3);
  return scale2(px, S);
}

// 粗制赤石块：16×16 七档均匀噪点（对标 raw_iron_block），×2 到 32
function drawRawBlock() {
  const S = 16, buf = Buffer.alloc(S * S * 4), vals = [];
  for (let y = 0; y < S; y++) for (let x = 0; x < S; x++) {
    // 双八度值噪声 → 成块色调；叠加散列抖动破色带
    const t = valueNoise(x, y, 2.6, 71) * 0.62 + valueNoise(x, y, 1.3, 72) * 0.38;
    vals.push(Math.min(0.999, Math.max(0, t * 0.92 + hash(x, y, 73) * 0.16 - 0.04)));
  }
  // 分位数阈值：保证七档按目标占比全部命中（固定阈值会因噪声分布集中而丢档）
  const sorted = [...vals].sort((a, b) => a - b);
  const steps = RAW_QUANT.map(q => sorted[Math.min(sorted.length - 1, Math.floor(q * sorted.length))]);
  for (let y = 0; y < S; y++) for (let x = 0; x < S; x++) {
    let i = 0; while (i < steps.length && vals[y * S + x] >= steps[i]) i++;
    put(buf, S, x, y, RAW[i]);
  }
  return scale2(buf, S);
}
// 浓缩赤石精华块：16×16 五档放射（对标 redstone_block：1px 最暗外框 + 中心暗、向外增亮），×2 到 32
function drawEssenceBlock() {
  const S = 16, buf = Buffer.alloc(S * S * 4), cen = (S - 1) / 2;
  for (let y = 0; y < S; y++) for (let x = 0; x < S; x++) {
    if (x === 0 || y === 0 || x === S - 1 || y === S - 1) { put(buf, S, x, y, ESS[0]); continue; }
    const dx = x - cen, dy = y - cen;
    // 欧氏 + 切比雪夫混合半径：方中带圆，避免完美同心方环的「靶心」感
    const r = (Math.hypot(dx, dy) / cen) * 0.62 + (Math.max(Math.abs(dx), Math.abs(dy)) / cen) * 0.38;
    // 叠加散列 + 值噪声，打散色带、形成斑驳块面（幅度小于梯度，保住中心暗→边缘亮的走向）
    const n = (hash(x, y, 89) - 0.5) * 0.7 + (valueNoise(x, y, 1.7, 91) - 0.5) * 0.5;
    const lv = Math.max(0, Math.min(4, Math.round(0.6 + 3.4 * r + n)));
    put(buf, S, x, y, ESS[lv]);
  }
  return scale2(buf, S);
}
// 最近邻 ×2：与底材（原版 16×16 放大）同粒度；沿用源图 alpha，保住物品透明背景
function scale2(src, S) {
  const out = Buffer.alloc(S * 2 * S * 2 * 4);
  for (let y = 0; y < S * 2; y++) for (let x = 0; x < S * 2; x++) {
    const s = ((y >> 1) * S + (x >> 1)) * 4, d = (y * S * 2 + x) * 4;
    out[d] = src[s]; out[d + 1] = src[s + 1]; out[d + 2] = src[s + 2]; out[d + 3] = src[s + 3];
  }
  return out;
}

// ---- 五件清单（顺序照用户提问）----
const ASSETS = [
  { name: 'akaishi_crystal', dir: ITEM_DIR, kind: 'item', ref: 'item/amethyst_shard', draw: drawCrystal },
  { name: 'raw_akaishi_block', dir: BLOCK_DIR, kind: 'block', ref: 'block/raw_iron_block', draw: drawRawBlock },
  { name: 'akaishi_essence', dir: ITEM_DIR, kind: 'item', ref: 'item/amethyst_shard', draw: drawEssence },
  { name: 'akaishi_essence_compressed', dir: ITEM_DIR, kind: 'item', ref: 'item/flint', draw: drawCompressed },
  { name: 'akaishi_essence_block', dir: BLOCK_DIR, kind: 'block', ref: 'block/redstone_block', draw: drawEssenceBlock },
];

function checker(w, h, cell) {
  const px = Buffer.alloc(w * h * 4);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const on = ((Math.floor(x / cell) + Math.floor(y / cell)) & 1) === 0;
    const v = on ? 90 : 62, o = (y * w + x) * 4;
    px[o] = v; px[o + 1] = v; px[o + 2] = v + 4; px[o + 3] = 255;
  }
  return px;
}
function blit(dst, DW, src, SW, SH, ox, oy, f) {
  for (let y = 0; y < SH * f; y++) for (let x = 0; x < SW * f; x++) {
    const so = (Math.floor(y / f) * SW + Math.floor(x / f)) * 4;
    if (src[so + 3] === 0) continue;
    const dof = ((oy + y) * DW + ox + x) * 4;
    dst[dof] = src[so]; dst[dof + 1] = src[so + 1]; dst[dof + 2] = src[so + 2]; dst[dof + 3] = 255;
  }
}
function colorCount(px) {
  const s = new Set();
  for (let i = 0; i < W * H; i++) if (px[i * 4 + 3]) s.add(`${px[i * 4]},${px[i * 4 + 1]},${px[i * 4 + 2]}`);
  return s.size;
}

function main() {
  const news = [], olds = [], refs = [];
  for (const a of ASSETS) {
    const src = path.join(a.dir, a.name + '.png');
    const px = a.draw();
    news.push(px);
    console.log(a.name.padEnd(30) + ' colors=' + colorCount(px));
    // 改前：优先读备份（幂等）
    const bak = path.join(BACKUP_DIR, a.name + '.png');
    olds.push(resizeNearest(readPng(fs.existsSync(bak) ? bak : src), W, H));
    refs.push(resizeNearest(vanillaTex(a.ref), W, H));
    if (!PREVIEW_ONLY) {
      fs.mkdirSync(a.dir, { recursive: true });
      if (!fs.existsSync(bak)) { fs.mkdirSync(BACKUP_DIR, { recursive: true }); fs.copyFileSync(src, bak); }
      fs.writeFileSync(src, writePng(W, H, px));
    }
  }

  // 三行对照图：改后 / 改前 / 原版参照，5 列
  const f = 4, pad = 6, cw = W * f + pad * 2, ch = H * f + pad * 2;
  const rows = [news, olds, refs];
  const PW = cw * ASSETS.length, PH = ch * rows.length;
  const sheet = checker(PW, PH, 8);
  rows.forEach((row, r) => row.forEach((px, c) => blit(sheet, PW, px, W, H, c * cw + pad, r * ch + pad, f)));
  fs.mkdirSync(path.dirname(PREVIEW), { recursive: true });
  fs.writeFileSync(PREVIEW, writePng(PW, PH, sheet));
  console.log(PREVIEW_ONLY ? '预览（未写贴图）: ' : '已写 5 张贴图，预览: ', path.relative(ROOT, PREVIEW));
  return 0;
}

process.exit(main());
