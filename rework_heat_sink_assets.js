// 散热器家族物品图标重制（工业机械 + 生物科技风）。
// 目标：textures/item/heat_sink_*.png 与 fusion_heat_sink_*.png 共 12 张，32x32 RGBA8。
// 原理：
//   1) 形制 = 金属底板 + 竖直鳍片阵列 + 上下安装凸缘与螺栓，体现「散热片」语义；
//   2) 普通散热器按品质给不同金属调色（灰→钢→铜→黄铜→银蓝→紫科技）；
//   3) 聚变散热器在鳍片中段加入发光横槽（tier1~5 沿用同序品质色，life 为生命青绿）。
// 幂等：首次运行备份原图到 gui_layouts/heat_sink_backup/，之后始终以备份为源。
// 用法：node rework_heat_sink_assets.js
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/item');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/heat_sink_backup');
const PREVIEW = path.join(ROOT, 'gui_layouts/heat_sink_rework_preview.png');

const W = 32, H = 32;
const OUT = [18, 20, 24];

// base 底板 / fin 鳍片 / light 高光 / dark 阴影 / glow 发光槽
const PALS = {
  poor: { base: [120, 122, 130], fin: [146, 148, 156], light: [180, 182, 190], dark: [76, 78, 86], glow: [180, 200, 220] },
  normal: { base: [146, 152, 164], fin: [176, 182, 194], light: [210, 216, 226], dark: [94, 100, 112], glow: [190, 214, 238] },
  good: { base: [172, 120, 74], fin: [202, 150, 96], light: [234, 192, 142], dark: [116, 74, 40], glow: [255, 176, 96] },
  fine: { base: [188, 152, 80], fin: [220, 184, 104], light: [250, 224, 162], dark: [126, 96, 42], glow: [255, 214, 118] },
  exquisite: { base: [156, 186, 214], fin: [190, 216, 238], light: [232, 246, 255], dark: [96, 124, 154], glow: [160, 224, 255] },
  ultimate: { base: [150, 116, 206], fin: [182, 150, 232], light: [224, 202, 255], dark: [92, 64, 142], glow: [214, 150, 255] },
  life: { base: [86, 168, 152], fin: [116, 204, 184], light: [178, 242, 226], dark: [42, 102, 92], glow: [132, 255, 214] },
};

const ITEMS = [
  { name: 'heat_sink_poor', pal: 'poor', fusion: false },
  { name: 'heat_sink_normal', pal: 'normal', fusion: false },
  { name: 'heat_sink_good', pal: 'good', fusion: false },
  { name: 'heat_sink_fine', pal: 'fine', fusion: false },
  { name: 'heat_sink_exquisite', pal: 'exquisite', fusion: false },
  { name: 'heat_sink_ultimate', pal: 'ultimate', fusion: false },
  { name: 'fusion_heat_sink_tier1', pal: 'poor', fusion: true },
  { name: 'fusion_heat_sink_tier2', pal: 'normal', fusion: true },
  { name: 'fusion_heat_sink_tier3', pal: 'good', fusion: true },
  { name: 'fusion_heat_sink_tier4', pal: 'fine', fusion: true },
  { name: 'fusion_heat_sink_tier5', pal: 'exquisite', fusion: true },
  { name: 'fusion_heat_sink_life', pal: 'life', fusion: true },
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

function heatsink(px, pal, fusion) {
  const mask = new Uint8Array(W * H);
  const x0 = 4, x1 = 27, y0 = 5, y1 = 26;

  // 底板 + 斜角
  fillRect(px, mask, x0, y0, x1, y1, pal.base);
  for (let x = x0; x <= x1; x++) { put(px, x, y0, pal.light); put(px, x, y1, pal.dark); }
  for (let y = y0; y <= y1; y++) { put(px, x0, y, pal.light); put(px, x1, y, pal.dark); }

  // 上下安装凸缘
  fillRect(px, mask, x0 + 1, y0 + 1, x1 - 1, y0 + 2, pal.fin);
  fillRect(px, mask, x0 + 1, y1 - 2, x1 - 1, y1 - 1, pal.fin);

  // 竖直鳍片：2px 宽，步长 4
  for (let fx = x0 + 3; fx <= x1 - 4; fx += 4) {
    fillRect(px, mask, fx, y0 + 3, fx + 1, y1 - 3, pal.fin);
    for (let y = y0 + 3; y <= y1 - 3; y++) {
      put(px, fx, y, pal.light);          // 鳍片受光边
      put(px, fx + 1, y, pal.dark);       // 鳍片背光边
    }
  }

  // 聚变发光横槽
  if (fusion) {
    fillRect(px, mask, x0 + 1, 15, x1 - 1, 16, pal.glow);
    for (let x = x0 + 1; x <= x1 - 1; x++) put(px, x, 15, [255, 255, 255]);
  }

  // 四角螺栓
  for (const [bx, by] of [[x0 + 1, y0 + 1], [x1 - 1, y0 + 1], [x0 + 1, y1 - 1], [x1 - 1, y1 - 1]]) {
    put(px, bx, by, OUT); put(px, bx, by - 1 > y0 ? by - 1 : by, pal.light);
  }

  // 细微颗粒：打散死平色，提升调色板层次
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    if (!mask[y * W + x]) continue;
    const o = (y * W + x) * 4, n = (((x * 5 + y * 11) % 5) - 2) * 4;
    px[o] = Math.max(0, Math.min(255, px[o] + n));
    px[o + 1] = Math.max(0, Math.min(255, px[o + 1] + n));
    px[o + 2] = Math.max(0, Math.min(255, px[o + 2] + n));
  }
  outline(px, mask);
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

function main() {
  fs.mkdirSync(BACKUP_DIR, { recursive: true });
  const cells = [];
  for (const it of ITEMS) {
    const src = path.join(DIR, it.name + '.png');
    const bak = path.join(BACKUP_DIR, it.name + '.png');
    if (!fs.existsSync(bak)) fs.copyFileSync(src, bak);
    const px = Buffer.alloc(W * H * 4);
    heatsink(px, PALS[it.pal], it.fusion);
    fs.writeFileSync(src, writePng(W, H, px));
    const colors = new Set();
    for (let i = 0; i < W * H; i++) if (px[i * 4 + 3]) colors.add(`${px[i * 4]},${px[i * 4 + 1]},${px[i * 4 + 2]}`);
    console.log(it.name.padEnd(24) + ' fusion=' + it.fusion + '  colors=' + colors.size);
    cells.push(px);
  }

  const f = 4, pad = 6, cols = 6;
  const rows = Math.ceil(cells.length / cols);
  const cw = W * f + pad * 2, ch = H * f + pad * 2;
  const PW = cw * cols, PH = ch * rows;
  const sheet = checker(PW, PH, 8);
  cells.forEach((px, i) => blit(sheet, PW, px, W, H, (i % cols) * cw + pad, Math.floor(i / cols) * ch + pad, f));
  fs.writeFileSync(PREVIEW, writePng(PW, PH, sheet));
  console.log('共重制 ' + cells.length + ' 张散热器图标');
  console.log('预览: ' + path.relative(ROOT, PREVIEW));
  return 0;
}

process.exit(main());
