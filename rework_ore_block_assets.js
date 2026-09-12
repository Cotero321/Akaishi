// 赤石矿方块贴图重制（16 张 = 4 环境 × 4 浓度）：按原版矿石的构造方式重做。
//
// 为什么这样改（依据对原版贴图的实测）：
//   1) 原版矿石的底材 **就是** 对应石头方块的原像素（iron_ore 里 stone 的 4 个色占 75%），
//      所以自绘噪声底材与原版石头并排必然「割裂」——改为直接读 client.jar 里的
//      stone / deepslate / netherrack / end_stone 贴图，最近邻 ×2 铺满 32×32，底材与原版逐像素一致。
//   2) 原版矿斑是 6~9 个「扁平小石粒」（实测 iron_ore：9 块，包围盒 4×2 / 8×3 / 5×3 / 3×2…，
//      宽约为高的 2 倍），每块内部是沿轮廓的色调渐变 —— 外缘暗、中心亮，逐行读作 2 3 4 4 5 4 3；
//      5 级色调占比约 12/30/32/21/5%（铁 119,103,79 → 136,116,85 → 175,142,119 → 216,175,147
//      → 226,192,170），且**没有连续描边**。斑块尺寸也照原版（rx 1.5~2.7 格 ≈ 宽 6~11 px，
//      折合原版 3~5 px）。走过的弯路：rx 缩到 1.2~2.8 并配「放不下就缩小重试」→ 斑块被压碎成
//      十几个小点；斑块过小 → 每块只剩一档色，画出来是纯色贴片。故 rx 不再缩、块数看 min 下限。
//      形状用超椭圆（指数 2.1）而非正椭圆 —— 稍饱满又不至于变成 n=2.6 那种「圆角长方块」。
//      斑块排版在 16×16 虚拟网格（G）上生成后按 SCALE 放大 —— 与底材的 2×2 像素块同粒度，
//      两者画风才统一；矿斑整体亮度也照原版略高于底材（铁 143.9 vs 石头 122.5），不做成暗洞。
// 3) 浓度只由「矿石占比」表达：斑块数与半径逐档递增，四档占比目标约 8/15/23/32%。
// 同环境四档共用同一底材与同一套斑块种子前缀，只让斑块数量与大小变化，
// 因此「浓度」信息 100% 来自矿石占比。
// 4) 立体感由「微凸矿斑层」实现：另外产出一张透明底、仅含矿斑的 overlay 贴图
//    （底材像素 alpha=0），方块模型在 cube_all 底材之外再叠 6 个外凸 1/16 格的矿斑面。
//    底材贴图保留矿斑像素，正面看两层完全重合，斜看才有视差 —— 既稳又有厚度。
//    同一次绘制同时写主贴图与 overlay，保证两层像素绝对对齐。
// 幂等：首次运行备份原图到 gui_layouts/ore_block_backup/；重绘由固定种子从零生成。
// 用法：node rework_ore_block_assets.js                写入贴图（主贴图 + overlay） + 出前后对照图
//       node rework_ore_block_assets.js --preview      仅出对照图，不写贴图
//       node rework_ore_block_assets.js --zoom         仅出「改后」4x4 放大图
//       node rework_ore_block_assets.js --overlay      仅出「微凸矿斑层」预览图（overlay 贴图 + 外凸错位合成）
//       node rework_ore_block_assets.js --iso          仅出「微凸矿斑层」的等轴测立体模拟图（建模出稿）
//       node rework_ore_block_assets.js --jar <path>   指定客户端 jar
//       node rework_ore_block_assets.js --size 16|32   输出尺寸（默认 32；16 = 与原版同分辨率）
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/block');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/ore_block_backup');

// 输出尺寸：16 = 与原版矿石同分辨率（底材 1:1 逐像素同源，无放大格纹）；
//           32 = 沿用项目现有方块尺寸（原版底材最近邻 ×2，会有 2×2 像素块感）。
const SIZE = (() => {
  const i = process.argv.indexOf('--size');
  const v = i >= 0 ? parseInt(process.argv[i + 1], 10) : 32;
  if (v !== 16 && v !== 32) throw Error('--size 只支持 16 或 32');
  return v;
})();
const W = SIZE, H = SIZE;
const SCALE = SIZE / 16; // 相对原版 16×16 的倍率：半径、噪声格、高光数量都按它缩放
const TAG = SIZE === 16 ? '_s16' : '';
const PREVIEW = path.join(ROOT, 'gui_layouts/ore_block_rework_preview' + TAG + '.png');
const ZOOM = path.join(ROOT, 'gui_layouts/ore_block_rework_zoom' + TAG + '.png');
const OVERLAY = path.join(ROOT, 'gui_layouts/ore_block_overlay_preview' + TAG + '.png');
const ISO = path.join(ROOT, 'gui_layouts/ore_block_iso_preview' + TAG + '.png');

const JAR_CANDIDATES = [
  path.join(process.env.USERPROFILE || '', '.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar'),
  path.join(process.env.USERPROFILE || '', '.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraft/client/1.20.1/client-1.20.1-extra.jar'),
];

// 各环境：底材直接取原版对应石头方块贴图（16×16 → 最近邻 ×2）
const ENVS = [
  { prefix: '', tex: 'stone' },
  { prefix: 'deepslate_', tex: 'deepslate' },
  { prefix: 'nether_', tex: 'netherrack' },
  { prefix: 'end_', tex: 'end_stone' },
];
// 原版矿石参考图（对照图末尾一行用，直观比对斑块尺度与色调阶梯）
const VANILLA_REF = ['iron_ore', 'deepslate_iron_ore', 'nether_quartz_ore', 'end_stone'];

// 五档赤色阶梯：亮度比照原版铁矿（106/121/155/185/195），色相仍是赤石红，但降饱和到
// 哑光矿物级（最亮档也不超过鲑红），避免高饱和红块像打光的珠子贴在石面上。
const ORE = {
  edge: [148, 60, 56],
  dark: [174, 72, 64],
  body: [202, 90, 80],
  light: [228, 118, 102],
  hi: [242, 148, 128],
};
const TONE_LADDER = [ORE.edge, ORE.dark, ORE.body, ORE.light, ORE.hi];

// 矿斑排版网格：在原版 16×16 上生成，输出时按 SCALE 放大成 SCALE×SCALE 像素块 —— 
// 与底材（原版石头最近邻放大）同粒度，避免「细密矿斑贴在粗块石头上」的割裂感。
const G = 16;
// 色调阈值（作用在归一化半径的补值 d' 上）：由原版铁矿实测占比 12/30/32/21/5% 反推 ——
// d' < 第一档 → 最暗（外缘一圈），d' ≥ 末档 → 最亮（中心一小簇）。
// 末档从 0.776 降到 0.72：原版的亮核只占 5%，但那是 1px 尺度的 16×16 贴图；
// 本项目矿斑每格有 SCALE 个像素，亮核太小就看不见层次，故略放大亮核。
const TONE_STEPS = [0.056, 0.22, 0.46, 0.72];

// 四档浓度：rx 斑块基准半宽（16 网格单位）、min 斑块数下限、target 目标矿石占比。
// 放置策略是「先凑够块数下限，再按占比放满即停」——
// 纯按固定块数会让高浓度档排不下而占比倒退；纯按占比又会让低浓度档只剩两三块大补丁。
// rx 逐档放大 → 高浓度档靠「斑块变大」而非「斑块塞满」提升占比。
const TIERS = [
  { suffix: 'low', rx: [1.5, 1.9], min: 5, target: 0.090 },
  { suffix: 'medium', rx: [1.7, 2.1], min: 6, target: 0.150 },
  { suffix: 'perfect', rx: [1.9, 2.4], min: 7, target: 0.220 },
  { suffix: 'flawless', rx: [2.1, 2.7], min: 8, target: 0.300 },
];

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

// 从 zip 读取单个条目（支持 stored / deflate）
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

let JAR = null;
// 最近邻缩放：把任意尺寸的 png 采样到 dw×dh
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
// 读取原版方块贴图并按 SIZE 采样（16 → 1:1 逐像素同源；32 → 最近邻 ×2）
function vanillaBase(name) {
  if (!JAR) {
    const arg = process.argv.indexOf('--jar');
    JAR = arg >= 0 ? process.argv[arg + 1] : JAR_CANDIDATES.find(p => fs.existsSync(p));
    if (!JAR) throw Error('未找到 1.20.1 client.jar，请用 --jar <path> 指定');
  }
  return resizeNearest(decodePng(zipRead(JAR, 'assets/minecraft/textures/block/' + name + '.png')), W, H);
}
function put(px, x, y, c) { if (x < 0 || y < 0 || x >= W || y >= H) return; const o = (y * W + x) * 4; px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; px[o + 3] = 255; }

// 32 位整数散列，用于斑块随机；同参数必然同结果（幂等）
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
// 值噪声：粗网格 + 双线性插值 —— 让色调分区成块，而不是逐像素静态噪点
function valueNoise(x, y, cell, seed) {
  const gx = x / cell, gy = y / cell;
  const x0 = Math.floor(gx), y0 = Math.floor(gy);
  const fx = gx - x0, fy = gy - y0;
  const sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
  const n00 = hash(x0, y0, seed), n10 = hash(x0 + 1, y0, seed);
  const n01 = hash(x0, y0 + 1, seed), n11 = hash(x0 + 1, y0 + 1, seed);
  const a = n00 + (n10 - n00) * sx, b = n01 + (n11 - n01) * sx;
  return a + (b - a) * sy;
}

// 单块矿斑：超椭圆（|dx|^n + |dy|^n ≤ 1，n=2 是正椭圆、n 越大越接近圆角方）——
// 取 n=2.1：比正椭圆稍饱满一点，又不至于变成 n=2.6 那种「圆角长方块」（画出来像木板）。
// 轮廓半径带值噪声扰动，避免斑块排成整齐的鹅卵石。
// 返回 [x, y, t]：t = 该格到边界的归一化距离（0 = 中心，1 = 轮廓线），供着色复用 ——
// 着色必须与形状用同一个范数，否则「椭圆的渐变」会画在「方圆的轮廓」里，边缘色调对不上。
function patchCells(cx, cy, rx, ry, seed) {
  const N = 2.1;
  const cells = [], padX = Math.ceil(rx) + 1, padY = Math.ceil(ry) + 1;
  const x0 = Math.max(0, Math.floor(cx - padX)), x1 = Math.min(G - 1, Math.ceil(cx + padX));
  const y0 = Math.max(0, Math.floor(cy - padY)), y1 = Math.min(G - 1, Math.ceil(cy + padY));
  for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) {
    const ax = Math.abs(x - cx) / rx, ay = Math.abs(y - cy) / ry;
    const d = Math.pow(Math.pow(ax, N) + Math.pow(ay, N), 1 / N);
    const edge = 1 + 0.18 * (valueNoise(x, y, 2.2, seed) - 0.5) * 2;
    if (d > edge) continue;
    cells.push([x, y, Math.min(1, d / edge)]);
  }
  // 去毛刺：4 邻中同伴少于 2 的孤立像素丢弃，避免边缘出现单像素尖刺
  const set = new Set(cells.map(c => c[1] * G + c[0]));
  return cells.filter(([x, y]) => {
    let n = 0;
    for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) if (set.has((y + dy) * G + (x + dx))) n++;
    return n >= 2;
  });
}
// 邻域内是否有已落定的矿斑本体 —— 保证斑块之间至少留 1 格底色缝隙，
// 与原版一致（原版矿斑之间也常有缝隙）；缝隙过大会让高浓度档放不下而占比倒退。
function touches(occupied, x, y) {
  for (let dy = -1; dy <= 1; dy++) for (let dx = -1; dx <= 1; dx++) {
    const nx = x + dx, ny = y + dy;
    if (nx < 0 || ny < 0 || nx >= G || ny >= G) continue;
    if (occupied[ny * G + nx] === 1) return true;
  }
  return false;
}
// 在空白处试放一块矿斑。曾在这里加「放不下就按 0.85/0.7/0.55/0.45 依次缩小再试」，
// 结果高浓度档被压成十几个小碎点 —— 与「碎小石粒」的初衷相反。改为尺寸恒定、多试几次，
// 排不下就结束（占比达标即可，块数因此稳定在 6~9）。
function placePatch(rand, occupied, rx, ry, seed) {
  const m = 2;
  for (let attempt = 0; attempt < 400; attempt++) {
    const cx = m + rand() * (G - 2 * m), cy = m + rand() * (G - 2 * m);
    const cells = patchCells(cx, cy, rx, ry, seed + attempt * 13);
    if (cells.length < 3) continue;
    if (cells.some(([x, y]) => touches(occupied, x, y))) continue;
    return { cells, cx, cy, rx, ry };
  }
  return null;
}

// 按原版矿石结构计算矿斑着色（输出像素 idx → RGB）：
// 每块矿斑按「到轮廓的归一化距离」取五档色调 —— 外缘暗、中心亮，
// 再用散列抖动打散色带边界，得到原版那种「中心一小簇亮、外缘一圈暗」的天然过渡。
// 不画任何连续描边：原版最暗档只占 12% 且天然分布在外缘，另加一条下缘黑线就会变成黑框。
// 循环条件：先凑够 tier.min 块，再看占比是否达标 —— 两者都满足才停（保证单调且不像大补丁）。
// 只产出「矿斑像素」而不碰底材：主贴图与 overlay 共用同一结果，两层必然逐像素对齐。
function oreShading(tier, seed) {
  const rand = mulberry32(seed);
  const occupied = new Uint8Array(G * G);
  const patches = [];
  let covered = 0;
  // 块数上限 12；正常在达到 min + target 时 break
  for (let i = 0; i < 12; i++) {
    if (i >= tier.min && covered / (G * G) >= tier.target) break;
    // 大小抖动：同档里斑块不一，更像原版矿石（而不是一排等大的鹅卵石）
    const rx = tier.rx[0] + (tier.rx[1] - tier.rx[0]) * rand();
    const p = placePatch(rand, occupied, rx, rx * (0.5 + 0.22 * rand()), seed + i * 137);
    if (!p) break;
    for (const [x, y] of p.cells) occupied[y * G + x] = 1;
    covered += p.cells.length;
    patches.push(p);
  }
  const shade = new Map();
  for (const p of patches) for (const [x, y, t] of p.cells) {
    const dp = 1 - t + (hash(x, y, seed + 733) - 0.5) * 0.16;
    let k = 0;
    while (k < TONE_STEPS.length && dp >= TONE_STEPS[k]) k++;
    const c = TONE_LADDER[k];
    // 按 SCALE 放大成像素块：与底材（原版石头 ×SCALE）保持同一粒度。
    // 不做「子像素羽化」—— 试过按散列丢弃轮廓格的部分子像素，结果轮廓长出一圈
    // 断断续续的 1px 尖刺，像发霉的毛边；轮廓的不规则交给上面那层值噪声就够了。
    for (let sy = 0; sy < SCALE; sy++) for (let sx = 0; sx < SCALE; sx++) {
      shade.set((y * SCALE + sy) * W + (x * SCALE + sx), c);
    }
  }
  return shade;
}

// 环境序号 + 浓度序号组成的固定种子（用下标而非半径浮点数，避免同环境四档种子意外接近）
function seedOf(prefix, tier) {
  return 1000 + ENVS.findIndex(e => e.prefix === prefix) * 97 + TIERS.indexOf(tier) * 41;
}
// 矿斑着色缓存：预览图会多次取同一组合，避免重复放斑块（同时保证幂等）
const shadeCache = new Map();
function shadeOf(prefix, tier) {
  const key = prefix + '|' + tier.suffix;
  let s = shadeCache.get(key);
  if (!s) { s = oreShading(tier, seedOf(prefix, tier)); shadeCache.set(key, s); }
  return s;
}

const baseCache = new Map();
function baseOf(prefix) {
  if (!baseCache.has(prefix)) baseCache.set(prefix, vanillaBase(ENVS.find(e => e.prefix === prefix).tex));
  return baseCache.get(prefix);
}

// 主贴图 = 原版石头底材 + 矿斑（贴图为平面观感，正面与 overlay 完全重合）
function build(prefix, tier) {
  const px = Buffer.from(baseOf(prefix));
  for (const [idx, c] of shadeOf(prefix, tier)) put(px, idx % W, (idx / W) | 0, c);
  return px;
}
// overlay 贴图 = 透明底，只保留矿斑像素（含下缘落影），供模型 6 个外凸面叠用
function buildOverlay(prefix, tier) {
  const px = Buffer.alloc(W * H * 4);
  for (const [idx, c] of shadeOf(prefix, tier)) {
    const o = idx * 4;
    px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; px[o + 3] = 255;
  }
  return px;
}
// 矿石占比：与底材原像素不同的像素即为矿斑（底材直接来自原版，判定绝对可靠）
function coverage(px, prefix) {
  const base = baseOf(prefix);
  let n = 0;
  for (let i = 0; i < W * H; i++) {
    const o = i * 4;
    if (px[o] !== base[o] || px[o + 1] !== base[o + 1] || px[o + 2] !== base[o + 2]) n++;
  }
  return n / (W * H);
}

// 校验用：改前贴图（首次运行前为原图，之后为备份），统一重采样到当前 SIZE
function sourceOf(name) {
  const bak = path.join(BACKUP_DIR, name + '.png');
  return resizeNearest(readPng(fs.existsSync(bak) ? bak : path.join(DIR, name + '.png')), W, H);
}
// 原版参照行：把指定原版贴图（16×16）重采样到当前 SIZE 供并排比对
function vanillaRef(prefix, name) {
  if (name === ENVS.find(e => e.prefix === prefix).tex) return baseOf(prefix);
  return resizeNearest(decodePng(zipRead(JAR, 'assets/minecraft/textures/block/' + name + '.png')), W, H);
}

function sheet(f, out, afterOnly) {
  const cols = TIERS.length, pad = 6, cw = W * f + pad * 2;
  const rows = afterOnly ? ENVS.length : ENVS.length * 3;
  const PW = cw * cols, PH = cw * rows;
  const s = Buffer.alloc(PW * PH * 4);
  for (let y = 0; y < PH; y++) for (let x = 0; x < PW; x++) {
    const on = ((Math.floor(x / 8) + Math.floor(y / 8)) & 1) === 0, v = on ? 90 : 62, o = (y * PW + x) * 4;
    s[o] = v; s[o + 1] = v; s[o + 2] = v + 6; s[o + 3] = 255;
  }
  const blit = (src, ox, oy) => {
    for (let y = 0; y < H * f; y++) for (let x = 0; x < W * f; x++) {
      const so = (Math.floor(y / f) * W + Math.floor(x / f)) * 4;
      const d = ((oy + y) * PW + ox + x) * 4;
      s[d] = src[so]; s[d + 1] = src[so + 1]; s[d + 2] = src[so + 2]; s[d + 3] = 255;
    }
  };
  ENVS.forEach((env, r) => {
    TIERS.forEach((t, c) => {
      const name = env.prefix + 'akaishi_ore_' + t.suffix;
      blit(build(env.prefix, t), c * cw + pad, r * cw + pad);
      if (!afterOnly) {
        blit(sourceOf(name), c * cw + pad, (ENVS.length + r) * cw + pad);
        blit(vanillaRef(env.prefix, VANILLA_REF[ENVS.indexOf(env)]), c * cw + pad, (ENVS.length * 2 + r) * cw + pad);
      }
    });
  });
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, writePng(PW, PH, s));
  return out;
}

// overlay 预览：上 4 行 = overlay 贴图本身（棋盘格即透明区）；下 4 行 = 底材 + overlay 右下偏移 1px，
// 即模型「矿斑层外凸 1/16 格」时斜看会看到的错位效果（偏移量就是立体感的来源）。
function overlaySheet(f, out) {
  const cols = TIERS.length, pad = 6, cw = W * f + pad * 2;
  const PW = cw * cols, PH = cw * ENVS.length * 2;
  const s = Buffer.alloc(PW * PH * 4);
  for (let y = 0; y < PH; y++) for (let x = 0; x < PW; x++) {
    const on = ((Math.floor(x / 8) + Math.floor(y / 8)) & 1) === 0, v = on ? 90 : 62, o = (y * PW + x) * 4;
    s[o] = v; s[o + 1] = v; s[o + 2] = v + 6; s[o + 3] = 255;
  }
  // alpha 混合绘制：dx/dy 为放大后的像素级偏移，用来模拟外凸层错位
  const blend = (src, ox, oy, dx, dy) => {
    for (let y = 0; y < H * f; y++) for (let x = 0; x < W * f; x++) {
      const so = (Math.floor(y / f) * W + Math.floor(x / f)) * 4, a = src[so + 3] / 255;
      if (a === 0) continue;
      const X = ox + x + dx, Y = oy + y + dy;
      if (X < 0 || Y < 0 || X >= PW || Y >= PH) continue;
      const d = (Y * PW + X) * 4;
      s[d] = src[so] * a + s[d] * (1 - a);
      s[d + 1] = src[so + 1] * a + s[d + 1] * (1 - a);
      s[d + 2] = src[so + 2] * a + s[d + 2] * (1 - a);
    }
  };
  ENVS.forEach((env, r) => {
    TIERS.forEach((t, c) => {
      const ox = c * cw + pad;
      blend(buildOverlay(env.prefix, t), ox, r * cw + pad, 0, 0);
      const oy = (ENVS.length + r) * cw + pad;
      blend(baseOf(env.prefix), ox, oy, 0, 0);
      // 偏移量 = 1/16 格换算到当前贴图尺寸的像素数（32 尺寸下 = 2 px）
      blend(buildOverlay(env.prefix, t), ox, oy, SCALE * f, SCALE * f);
    });
  });
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, writePng(PW, PH, s));
  return out;
}

// 等轴测立体模拟（RULES §7「先图后码」的建模出稿）：
// 按 2:1 等轴测把方块画成「顶 + 右 + 左」三面的立方体，再叠上「六面各外凸 1/32 格
// （= 0.5 世界单位）」的矿斑层 —— 即方块模型在实机里的观感。
// 面明暗（顶 1.0 / 左 0.82 / 右 0.64）只为让立体读得出来；贴图本身不含明暗，实机由游戏光照负责。
function isoSheet(f, out) {
  const k = SCALE * f;            // 每个世界单位（1/16 格）在屏幕上的像素数
  const THICK = 0.5;              // 外凸厚度：0.5 世界单位 = 1/32 格（与 template_ore_overlay.json 一致）
  const cell = 34 * k, pad = 10;  // 34 = 方块本体 32 + 两侧各 1 的余量（外凸 0.5 时留有裕量，不裁切）
  const cw = cell + pad * 2;
  const PW = cw * TIERS.length, PH = cw * ENVS.length;
  const s = Buffer.alloc(PW * PH * 4);
  for (let y = 0; y < PH; y++) for (let x = 0; x < PW; x++) {
    const on = ((Math.floor(x / 8) + Math.floor(y / 8)) & 1) === 0, v = on ? 90 : 62, o = (y * PW + x) * 4;
    s[o] = v; s[o + 1] = v; s[o + 2] = v + 6; s[o + 3] = 255;
  }
  const LIGHT = { top: 1.0, right: 0.64, left: 0.82 };
  const texel = (img, u, v) => {
    const tu = Math.min(SIZE - 1, Math.max(0, Math.floor(u * SIZE)));
    const tv = Math.min(SIZE - 1, Math.max(0, Math.floor(v * SIZE)));
    return (tv * SIZE + tu) * 4;
  };
  // 逆投影：由屏幕局部像素与该面的外凸量 off（世界单位）解出命中面、贴图 uv、世界坐标深度
  const pick = (px, py, off) => {
    const A = (px - 17 * k) / k, S = (py - 17 * k) / k;
    const B = S + 16 + off, tx = B + A / 2, tz = B - A / 2;      // 顶面 y = 16 + off
    if (tx >= 0 && tx <= 16 && tz >= 0 && tz <= 16) return { face: 'top', u: tx / 16, v: tz / 16, d: tx + tz + 16 + off };
    const rz = 16 + off - A, ry = (16 + off + rz) / 2 - S;       // 右面 x = 16 + off
    if (rz >= 0 && rz <= 16 && ry >= 0 && ry <= 16) return { face: 'right', u: rz / 16, v: 1 - ry / 16, d: 16 + off + ry + rz };
    const lx = 16 + off + A, ly = (lx + 16 + off) / 2 - S;       // 左面 z = 16 + off
    if (lx >= 0 && lx <= 16 && ly >= 0 && ly <= 16) return { face: 'left', u: lx / 16, v: 1 - ly / 16, d: lx + ly + 16 + off };
    return null;
  };
  ENVS.forEach((env, r) => {
    const base = baseOf(env.prefix);
    TIERS.forEach((t, c) => {
      const ov = buildOverlay(env.prefix, t);
      const ox = c * cw + pad, oy = r * cw + pad;
      for (let py = 0; py < cell; py++) for (let px = 0; px < cell; px++) {
        const hb = pick(px + 0.5, py + 0.5, 0);
        let rgb = null;
        if (hb) {                                                  // 底材三面（面明暗区分朝向）
          const o = texel(base, hb.u, hb.v), L = LIGHT[hb.face];
          rgb = [base[o] * L, base[o + 1] * L, base[o + 2] * L];
        }
        const hv = pick(px + 0.5, py + 0.5, THICK);
        if (hv && (!hb || hv.d >= hb.d)) {                         // 凸层更靠近观察者才覆盖，凸起受光略亮
          const o = texel(ov, hv.u, hv.v);
          if (ov[o + 3] > 0) {
            const L = LIGHT[hv.face] * 1.06;
            rgb = [ov[o] * L, ov[o + 1] * L, ov[o + 2] * L];
          }
        }
        if (!rgb) continue;
        const d = ((oy + py) * PW + ox + px) * 4;
        s[d] = Math.min(255, rgb[0] | 0); s[d + 1] = Math.min(255, rgb[1] | 0); s[d + 2] = Math.min(255, rgb[2] | 0);
      }
    });
  });
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, writePng(PW, PH, s));
  return out;
}

function main() {
  const previewOnly = process.argv.includes('--preview');
  if (process.argv.includes('--iso')) {
    console.log('等轴测立体模拟（行=环境 主/深板岩/下界/末地，列=low→flawless）: '
      + path.relative(ROOT, isoSheet(SIZE === 16 ? 4 : 2, ISO)));
    return 0;
  }
  if (process.argv.includes('--overlay')) {
    console.log('微凸矿斑层预览（上 4 行 overlay 贴图 / 下 4 行 外凸错位合成；行=环境，列=low→flawless）: '
      + path.relative(ROOT, overlaySheet(SIZE === 16 ? 16 : 8, OVERLAY)));
    return 0;
  }
  if (process.argv.includes('--zoom')) {
    console.log('改后放大图（行=环境 主/深板岩/下界/末地，列=浓度 low→flawless）: ' + path.relative(ROOT, sheet(SIZE === 16 ? 16 : 8, ZOOM, true)));
    return 0;
  }
  fs.mkdirSync(BACKUP_DIR, { recursive: true });
  if (!previewOnly) {
    for (const env of ENVS) for (const t of TIERS) {
      const name = env.prefix + 'akaishi_ore_' + t.suffix;
      const live = path.join(DIR, name + '.png'), bak = path.join(BACKUP_DIR, name + '.png');
      if (!fs.existsSync(bak)) fs.copyFileSync(live, bak);
      fs.writeFileSync(live, writePng(W, H, build(env.prefix, t)));
      // 微凸矿斑层：同名 _overlay 透明底贴图（与主贴图共用同一次着色结果）
      fs.writeFileSync(path.join(DIR, name + '_overlay.png'), writePng(W, H, buildOverlay(env.prefix, t)));
    }
  }
  // 自检（预览模式也跑）：overlay 的不透明像素集合必须与「主贴图 − 底材」的差异集合完全一致
  for (const env of ENVS) for (const t of TIERS) {
    const main = build(env.prefix, t), base = baseOf(env.prefix), ov = buildOverlay(env.prefix, t);
    for (let i = 0; i < W * H; i++) {
      const o = i * 4;
      const diff = main[o] !== base[o] || main[o + 1] !== base[o + 1] || main[o + 2] !== base[o + 2];
      if (diff !== (ov[o + 3] === 255)) throw Error('主贴图与 overlay 未对齐: ' + env.prefix + t.suffix + ' @' + (i % W) + ',' + ((i / W) | 0));
    }
  }
  console.log('浓度（矿石占比 = 非底材像素 / 全图）:');
  for (const t of TIERS) {
    const line = ENVS.map(env => (coverage(build(env.prefix, t), env.prefix) * 100).toFixed(1) + '%').join('  ');
    console.log('  ' + t.suffix.padEnd(9) + ' ' + line);
  }
  console.log('对照图: ' + path.relative(ROOT, sheet(6, PREVIEW, false)));
  console.log('  行序: 上 4 行=改后（环境 主/深板岩/下界/末地）｜中 4 行=改前｜下 4 行=原版参照(iron_ore/deepslate_iron_ore/nether_quartz_ore/end_stone)');
  console.log('微凸矿斑层预览: node rework_ore_block_assets.js --overlay');
  console.log('等轴测立体模拟: node rework_ore_block_assets.js --iso');
  console.log(previewOnly ? '预览模式，未写盘' : '已写盘 主贴图 + overlay 各 16 张，原件已备份到 ' + path.relative(ROOT, BACKUP_DIR));
  return 0;
}

process.exit(main());
