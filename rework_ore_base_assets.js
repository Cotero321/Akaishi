// 「X矿石基底」11 张图标重制：统一为冷却基底同款金属底板 + 矿石色嵌块。
//
// 为什么改：原 11 张是平铺的 4 色方块（顶灰条 / 中矿石色 / 底暗条），无描边、无受光、
//   铺满整张 32x32 画布，与同族的 cooling_base（小巧的金属底板 + 蛇形冷媒管）完全不像一套。
// 怎么改：底板几何（rows 9~24 / cols 5~26）、四角螺栓、1px 暗色描边、左上受光右下背光、
//   细微颗粒，全部照搬 rework_misc_item_assets.js 的 coolingBase()。唯一的差别是中央那一块——
//   把蛇形冷媒管换成「矿石色嵌块」：1px 暗色嵌框 + 斜面矿块（上/左受光、下/右背光），
//   11 张之间只靠矿石色区分，从而与冷却基底同族同底、且互不撞脸。
// 幂等：首次运行备份原图到 gui_layouts/ore_base_backup/；重绘是从零生成，天然幂等。
// 用法：node rework_ore_base_assets.js             写入贴图 + 出预览
//       node rework_ore_base_assets.js --preview   仅出前后对照预览，不写贴图
//       node rework_ore_base_assets.js --zoom      放大成品对照（6x）
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/item');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/ore_base_backup');
const PREVIEW = path.join(ROOT, 'gui_layouts/ore_base_rework_preview.png');
const ZOOM = path.join(ROOT, 'gui_layouts/ore_base_rework_zoom.png');

const W = 32, H = 32;
const OUT = [18, 20, 24];                                          // 全模组统一暗色轮廓
// 底板配色直接引用 cooling_base，保证两张贴图的金属底板逐像素同色
const PLATE = { base: [110, 124, 138], light: [204, 220, 234], dark: [60, 70, 82] };

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
function readPng(file) {
  const buf = fs.readFileSync(file);
  if (buf.readUInt32BE(0) !== 0x89504e47) throw Error('PNG header: ' + file);
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
  if (depth !== 8 || (ctype !== 6 && ctype !== 2)) throw Error('expect RGBA8/RGB8: ' + file);
  const ch = ctype === 6 ? 4 : 3, stride = width * ch;
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

function put(px, x, y, c) { if (x < 0 || y < 0 || x >= W || y >= H) return; const o = (y * W + x) * 4; px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; px[o + 3] = 255; }
function mark(mask, x, y) { if (x < 0 || y < 0 || x >= W || y >= H) return; mask[y * W + x] = 1; }
function fillRect(px, mask, x0, y0, x1, y1, c) { for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) { put(px, x, y, c); mark(mask, x, y); } }
function outline(px, mask) {
  const add = [];
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    if (mask[y * W + x]) continue;
    for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
      const nx = x + dx, ny = y + dy;
      if (nx >= 0 && ny >= 0 && nx < W && ny < H && mask[ny * W + nx]) { add.push([x, y]); break; }
    }
  }
  for (const [x, y] of add) put(px, x, y, OUT);
}
// 细微颗粒，打散死平色
function grain(px, mask) {
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    if (!mask[y * W + x]) continue;
    const o = (y * W + x) * 4, n = (((x * 5 + y * 11) % 5) - 2) * 4;
    px[o] = Math.max(0, Math.min(255, px[o] + n));
    px[o + 1] = Math.max(0, Math.min(255, px[o + 1] + n));
    px[o + 2] = Math.max(0, Math.min(255, px[o + 2] + n));
  }
}

// 金属底板：与 cooling_base 逐像素同款（几何/螺栓/受光/颗粒一致，只是不画中央管路）
function plate(px, mask) {
  fillRect(px, mask, 5, 9, 26, 24, PLATE.base);
  for (let x = 5; x <= 26; x++) { put(px, x, 9, PLATE.light); put(px, x, 24, PLATE.dark); }
  for (let y = 9; y <= 24; y++) { put(px, 5, y, PLATE.light); put(px, 26, y, PLATE.dark); }
  for (const [bx, by] of [[7, 11], [24, 11], [7, 22], [24, 22]]) {
    put(px, bx, by, PLATE.dark); put(px, bx + 1, by, PLATE.dark);
    put(px, bx, by + 1, PLATE.dark); put(px, bx + 1, by + 1, PLATE.light);
  }
}

// 矿石色嵌块：占用的正是冷却基底那条蛇形冷媒管的位置（x9~22 / y12~21），
// 用 1px 暗色嵌框把它「嵌」进底板，内部做斜面（上/左受光、下/右背光）+ 两处高光/暗斑，
// 保证 11 张之间只有颜色差异，明暗结构完全一致。
function inlay(px, mask, pal) {
  fillRect(px, mask, 9, 12, 22, 21, pal.dark);        // 嵌框
  fillRect(px, mask, 10, 13, 21, 20, pal.base);       // 矿块本体
  for (let x = 10; x <= 21; x++) { put(px, x, 13, pal.light); put(px, x, 20, pal.dark); }
  for (let y = 13; y <= 20; y++) { put(px, 10, y, pal.light); put(px, 21, y, pal.dark); }
  put(px, 11, 14, pal.light); put(px, 12, 14, pal.light);   // 左上高光斑
  put(px, 11, 15, pal.light);
  put(px, 19, 19, pal.dark); put(px, 20, 19, pal.dark);     // 右下暗斑
}

// 矿石配色：base 沿用原图实测的矿石主色（保证与矿粉/矿晶同色），light/dark 由它拉开明暗
const ORES = {
  coal_ore_base: { base: [40, 44, 48], light: [96, 102, 112], dark: [20, 22, 26] },
  iron_ore_base: { base: [150, 145, 140], light: [206, 202, 196], dark: [92, 88, 84] },
  copper_ore_base: { base: [160, 82, 48], light: [226, 142, 96], dark: [96, 44, 24] },
  gold_ore_base: { base: [210, 170, 55], light: [252, 224, 124], dark: [138, 104, 24] },
  redstone_ore_base: { base: [150, 45, 42], light: [226, 86, 80], dark: [84, 22, 20] },
  lapis_ore_base: { base: [48, 78, 160], light: [108, 144, 232], dark: [24, 40, 96] },
  diamond_ore_base: { base: [70, 180, 190], light: [138, 238, 242], dark: [30, 108, 118] },
  emerald_ore_base: { base: [45, 165, 92], light: [118, 232, 156], dark: [20, 90, 48] },
  quartz_ore_base: { base: [210, 190, 165], light: [248, 240, 226], dark: [140, 122, 102] },
  netherite_ore_base: { base: [110, 75, 82], light: [168, 124, 126], dark: [54, 36, 40] },
  akaishi_ore_base: { base: [165, 65, 50], light: [232, 124, 104], dark: [90, 28, 20] },
};
const NAMES = Object.keys(ORES);

function build(name) {
  const px = Buffer.alloc(W * H * 4);
  const mask = new Uint8Array(W * H);
  plate(px, mask);
  inlay(px, mask, ORES[name]);
  outline(px, mask);
  grain(px, mask);
  return px;
}

// 改前 = 该物品当前原件（首次运行前即原图，备份后即备份），供直接比对「变没变」
function sourceOf(name) {
  const bak = path.join(BACKUP_DIR, name + '.png');
  return readPng(fs.existsSync(bak) ? bak : path.join(DIR, name + '.png')).px;
}

function checker(w, h, cell) {
  const px = Buffer.alloc(w * h * 4);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const on = ((Math.floor(x / cell) + Math.floor(y / cell)) & 1) === 0;
    const v = on ? 70 : 52, o = (y * w + x) * 4;
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
// 预览第 0 列固定放 cooling_base 作基准，其余 11 张为新矿石基底：上排改前 / 下排改后
function sheet(f, out) {
  const cols = NAMES.length + 1, pad = 6, cw = W * f + pad * 2, ch = H * f + pad * 2;
  const PW = cw * cols, PH = ch * 2;
  const s = checker(PW, PH, 8);
  const cool = readPng(path.join(DIR, 'cooling_base.png')).px;
  blit(s, PW, cool, W, H, pad, pad, f); blit(s, PW, cool, W, H, pad, ch + pad, f);
  NAMES.forEach((n, i) => {
    blit(s, PW, sourceOf(n), W, H, (i + 1) * cw + pad, pad, f);
    blit(s, PW, build(n), W, H, (i + 1) * cw + pad, ch + pad, f);
  });
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, writePng(PW, PH, s));
  return out;
}

function main() {
  if (process.argv.includes('--zoom')) {
    console.log('放大自查（左1=冷却基底基准 / 上排改前、下排改后）: ' + path.relative(ROOT, sheet(6, ZOOM)));
    return 0;
  }

  const previewOnly = process.argv.includes('--preview');
  fs.mkdirSync(BACKUP_DIR, { recursive: true });

  if (!previewOnly) {
    for (const n of NAMES) {
      const live = path.join(DIR, n + '.png'), bak = path.join(BACKUP_DIR, n + '.png');
      if (!fs.existsSync(bak)) fs.copyFileSync(live, bak);           // 仅首次备份原件，保证可回溯
      fs.writeFileSync(live, writePng(W, H, build(n)));
    }
  }

  for (const n of NAMES) {
    const px = build(n), colors = new Set();
    for (let i = 0; i < W * H; i++) if (px[i * 4 + 3]) colors.add(`${px[i * 4]},${px[i * 4 + 1]},${px[i * 4 + 2]}`);
    console.log(n.padEnd(22) + ' 色数 ' + String(colors.size).padStart(3) + '  实心 ' + alphaCount(px));
  }
  console.log('预览: ' + path.relative(ROOT, sheet(6, PREVIEW)));
  console.log(previewOnly ? '预览模式，未写盘' : '已写盘 ' + NAMES.length + ' 张，原件已备份到 ' + path.relative(ROOT, BACKUP_DIR));
  return 0;
}
function alphaCount(px) { let n = 0; for (let i = 0; i < W * H; i++) if (px[i * 4 + 3]) n++; return n; }

process.exit(main());
