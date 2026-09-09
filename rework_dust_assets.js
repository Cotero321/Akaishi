// 粉料家族物品图标重制（工业机械 + 生物科技风）。
// 目标：textures/item/*_dust.png 与 akaishi_dust.png 共 11 张，32x32 RGBA8。
// 原理：
//   1) 沿用项目既有 32x32 物品图标规范：透明底 + 厚暗色轮廓 + 左上受光/右下背光；
//   2) 粉堆 = 底部不规则土堆（横向行宽渐变近似圆丘）+ 顶部散落颗粒，表现「粉料」质感；
//   3) 每种材料仅换调色板，形制统一，避免撞脸也避免风格漂移。
// 幂等：首次运行备份原图到 gui_layouts/dust_backup/，之后始终以备份为源。
// 用法：node rework_dust_assets.js
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/item');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/dust_backup');
const PREVIEW = path.join(ROOT, 'gui_layouts/dust_rework_preview.png');

const W = 32, H = 32;
const OUT = [18, 20, 24];

// 材料调色：light 受光面 / base 主面 / dark 背光面
const MATS = {
  akaishi_dust: { light: [238, 150, 170], base: [204, 92, 118], dark: [138, 48, 72] },
  coal_dust: { light: [92, 92, 100], base: [52, 52, 58], dark: [28, 28, 32] },
  copper_dust: { light: [232, 168, 104], base: [198, 122, 62], dark: [134, 74, 32] },
  diamond_dust: { light: [196, 246, 246], base: [120, 220, 226], dark: [58, 148, 162] },
  emerald_dust: { light: [146, 240, 176], base: [72, 198, 108], dark: [32, 128, 66] },
  gold_dust: { light: [255, 234, 148], base: [246, 200, 72], dark: [178, 132, 24] },
  iron_dust: { light: [232, 232, 238], base: [186, 188, 196], dark: [124, 126, 136] },
  lapis_dust: { light: [116, 156, 246], base: [58, 96, 206], dark: [26, 50, 128] },
  netherite_dust: { light: [122, 108, 108], base: [74, 62, 64], dark: [40, 34, 36] },
  obsidian_dust: { light: [116, 84, 158], base: [64, 44, 96], dark: [34, 22, 56] },
  quartz_dust: { light: [248, 246, 242], base: [214, 210, 202], dark: [158, 154, 146] },
};

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

function mix(a, b, t) { return [Math.round(a[0] + (b[0] - a[0]) * t), Math.round(a[1] + (b[1] - a[1]) * t), Math.round(a[2] + (b[2] - a[2]) * t)]; }
function put(px, x, y, c) { if (x < 0 || y < 0 || x >= W || y >= H) return; const o = (y * W + x) * 4; px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; px[o + 3] = 255; }
function mark(mask, x, y) { if (x < 0 || y < 0 || x >= W || y >= H) return; mask[y * W + x] = 1; }
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

// 粉堆：行宽渐变近似圆丘
function dust(px, pal) {
  const mask = new Uint8Array(W * H);
  const cx = 16;
  // y -> 半宽
  const rows = { 9: 3, 10: 5, 11: 6, 12: 7, 13: 8, 14: 9, 15: 9, 16: 10, 17: 10, 18: 10, 19: 10, 20: 9, 21: 8, 22: 7, 23: 5 };
  for (const key of Object.keys(rows)) {
    const y = +key, hw = rows[key];
    for (let x = cx - hw; x <= cx + hw; x++) {
      const dx = x - cx, dy = y - 18;
      const d = Math.hypot(dx * 0.9, dy * 1.1);
      let c;
      if (dx <= -3 && dy <= -1) c = pal.light;          // 左上受光
      else if (dx >= 4 || dy >= 3) c = pal.dark;         // 右下背光
      else c = pal.base;
      // 颗粒噪点：用坐标哈希打散，避免死平色
      const n = ((x * 7 + y * 13) % 5) - 2;
      c = [c[0] + n * 3, c[1] + n * 3, c[2] + n * 3].map(v => Math.max(0, Math.min(255, v)));
      put(px, x, y, c); mark(mask, x, y);
    }
  }
  // 顶部散落颗粒
  const grains = [[9, 8], [11, 6], [14, 5], [18, 5], [21, 7], [23, 9], [7, 12], [25, 12], [12, 10], [20, 9]];
  for (const [gx, gy] of grains) { put(px, gx, gy, pal.light); mark(mask, gx, gy); }
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
  const names = Object.keys(MATS);
  const cells = [];
  for (const n of names) {
    const src = path.join(DIR, n + '.png');
    const bak = path.join(BACKUP_DIR, n + '.png');
    if (!fs.existsSync(bak)) fs.copyFileSync(src, bak);
    const px = Buffer.alloc(W * H * 4);
    dust(px, MATS[n]);
    fs.writeFileSync(src, writePng(W, H, px));
    const colors = new Set();
    for (let i = 0; i < W * H; i++) if (px[i * 4 + 3]) colors.add(`${px[i * 4]},${px[i * 4 + 1]},${px[i * 4 + 2]}`);
    console.log(n.padEnd(20) + ' colors=' + colors.size);
    cells.push(px);
  }

  const f = 4, pad = 6, cols = 6;
  const rows = Math.ceil(cells.length / cols);
  const cw = W * f + pad * 2, ch = H * f + pad * 2;
  const PW = cw * cols, PH = ch * rows;
  const sheet = checker(PW, PH, 8);
  cells.forEach((px, i) => blit(sheet, PW, px, W, H, (i % cols) * cw + pad, Math.floor(i / cols) * ch + pad, f));
  fs.writeFileSync(PREVIEW, writePng(PW, PH, sheet));
  console.log('共重制 ' + cells.length + ' 张粉料图标');
  console.log('预览: ' + path.relative(ROOT, PREVIEW));
  return 0;
}

process.exit(main());
