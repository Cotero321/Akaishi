// 剩余占位方块贴图重制（工业机械 + 生物科技风）。
// 覆盖 10 张（decay_stone 属原版石纹紫色重着色，列入白名单跳过）：
//   流体/等离子罐侧面 4 张 —— 金属罐身 + 竖向液位观察窗，配色与各自 _top 呼应
//   矿机升级块底面 3 张 —— 接口板 + 散热格栅 + 家族色铭牌
//   体扫描仪 2 张 —— 侧面扫描窗 / 顶面扫描镜头
//   晶体块 1 张 —— 竖向晶体棱柱（与 crystal_cluster 同族粉色）
// 幂等：首次运行备份原图到 gui_layouts/remaining_block_backup/，之后始终以备份为源。
// 用法：node rework_remaining_block_assets.js
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/block');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/remaining_block_backup');
const PREVIEW = path.join(ROOT, 'gui_layouts/remaining_block_rework_preview.png');

const CRC_TABLE = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1); t[n] = c; }
  return t;
})();
function crc32(buf) { let c = 0xFFFFFFFF; for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 255] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; }
function writePng(w, h, px) {
  const raw = Buffer.alloc(h * (1 + w * 4));
  let o = 0;
  for (let y = 0; y < h; y++) { raw[o++] = 0; px.copy(raw, o, y * w * 4, (y + 1) * w * 4); o += w * 4; }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8; ihdr[9] = 6;
  const chunk = (tag, data) => {
    const b = Buffer.alloc(12 + data.length);
    b.writeUInt32BE(data.length, 0); b.write(tag, 4, 'ascii'); data.copy(b, 8);
    b.writeUInt32BE(crc32(b.subarray(4, 8 + data.length)), 8 + data.length);
    return b;
  };
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]);
}

const cl = v => v < 0 ? 0 : v > 255 ? 255 : v | 0;
const sh = (c, d) => [cl(c[0] + d), cl(c[1] + d), cl(c[2] + d)];
const mix = (a, b, t) => [cl(a[0] + (b[0] - a[0]) * t), cl(a[1] + (b[1] - a[1]) * t), cl(a[2] + (b[2] - a[2]) * t)];
function h2(x, y, s) {
  let h = Math.imul(x, 374761393) ^ Math.imul(y, 668265263) ^ Math.imul(s, 362437);
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return ((h ^ (h >>> 16)) >>> 0) % 1000 / 1000;
}
function put(px, w, h, x, y, c) { if (x < 0 || y < 0 || x >= w || y >= h) return; const o = (y * w + x) * 4; px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; px[o + 3] = 255; }
function rect(px, w, h, x0, y0, x1, y1, c) { for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) put(px, w, h, x, y, c); }
function fill(px, w, h, c) { rect(px, w, h, 0, 0, w - 1, h - 1, c); }
function disc(px, w, h, cx, cy, r, c) { for (let y = cy - r; y <= cy + r; y++) for (let x = cx - r; x <= cx + r; x++) { const dx = x - cx, dy = y - cy; if (dx * dx + dy * dy <= r * r) put(px, w, h, x, y, c); } }
function ring(px, w, h, cx, cy, r0, r1, c) { for (let y = cy - r1; y <= cy + r1; y++) for (let x = cx - r1; x <= cx + r1; x++) { const d2 = (x - cx) ** 2 + (y - cy) ** 2; if (d2 >= r0 * r0 && d2 <= r1 * r1) put(px, w, h, x, y, c); } }
function line(px, w, h, x0, y0, x1, y1, c) {
  const dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
  let err = dx - dy, x = x0, y = y0;
  for (;;) { put(px, w, h, x, y, c); if (x === x1 && y === y1) break; const e2 = 2 * err; if (e2 > -dy) { err -= dy; x += sx; } if (e2 < dx) { err += dx; y += sy; } }
}
// 拉丝金属底板 + 四边倒角
function plate(px, w, h, base, seed) {
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const brush = (h2(x, y, seed) - 0.5) * 12 + (h2(x >> 2, 0, seed + 7) - 0.5) * 6;
    put(px, w, h, x, y, sh(base, brush));
  }
  for (let i = 0; i < w; i++) { put(px, w, h, i, 0, sh(base, 30)); put(px, w, h, i, h - 1, sh(base, -26)); }
  for (let i = 0; i < h; i++) { put(px, w, h, 0, i, sh(base, 30)); put(px, w, h, w - 1, i, sh(base, -26)); }
}
function border(px, w, h, x0, y0, x1, y1, pal, wd) {
  for (let x = x0; x <= x1; x++) for (let d = 0; d < wd; d++) { put(px, w, h, x, y0 + d, pal.light); put(px, w, h, x, y1 - d, pal.dark); }
  for (let y = y0; y <= y1; y++) for (let d = 0; d < wd; d++) { put(px, w, h, x0 + d, y, pal.light); put(px, w, h, x1 - d, y, pal.dark); }
}
// 内凹面板：上/左为阴影，下/右受光
function recess(px, w, h, x0, y0, x1, y1, c, pal) {
  rect(px, w, h, x0, y0, x1, y1, c);
  for (let x = x0; x <= x1; x++) { put(px, w, h, x, y0, pal.dark); put(px, w, h, x, y1, sh(c, 20)); }
  for (let y = y0; y <= y1; y++) { put(px, w, h, x0, y, pal.dark); put(px, w, h, x1, y, sh(c, 20)); }
}
function bolt(px, w, h, x, y, n, pal) {
  rect(px, w, h, x, y, x + n - 1, y + n - 1, pal.dark);
  rect(px, w, h, x + 1, y + 1, x + n - 2, y + n - 2, pal.light);
  const m = (n - 1) >> 1; put(px, w, h, x + m, y + m, pal.dark);
}
function bolts4(px, w, h, pal, n, inset) {
  bolt(px, w, h, inset, inset, n, pal);
  bolt(px, w, h, w - inset - n, inset, n, pal);
  bolt(px, w, h, inset, h - inset - n, n, pal);
  bolt(px, w, h, w - inset - n, h - inset - n, n, pal);
}
function grain(px, w, h, seed) {
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const o = (y * w + x) * 4, n = (h2(x, y, seed + 31) - 0.5) * 10;
    px[o] = cl(px[o] + n); px[o + 1] = cl(px[o + 1] + n); px[o + 2] = cl(px[o + 2] + n);
  }
}

// —— 调色板 ——
const TANK_METAL = { base: [92, 96, 104], light: [152, 156, 164], dark: [44, 46, 52], panel: [56, 60, 68] };
const TANK_ACCENT = {
  basic: { accent: [72, 196, 170], accentHi: [156, 244, 224], accentDk: [32, 106, 92] },
  advanced: { accent: [132, 112, 232], accentHi: [190, 176, 255], accentDk: [62, 50, 138] },
  super: { accent: [238, 176, 60], accentHi: [255, 224, 152], accentDk: [148, 96, 20] },
  plasma: { accent: [230, 84, 232], accentHi: [255, 172, 255], accentDk: [128, 30, 132] },
};
const MINER_METAL = { base: [100, 102, 106], light: [154, 156, 160], dark: [50, 52, 56], panel: [58, 60, 64] };
const MINER_ACCENT = {
  fortune: { accent: [66, 140, 235], accentHi: [152, 202, 255], accentDk: [30, 74, 138] },
  speed: { accent: [240, 205, 60], accentHi: [255, 240, 150], accentDk: [150, 118, 18] },
  storage: { accent: [72, 205, 140], accentHi: [152, 245, 202], accentDk: [26, 112, 74] },
};
const SCAN_METAL = { base: [78, 82, 90], light: [142, 148, 158], dark: [38, 40, 46], panel: [48, 52, 60] };
const SCAN_ACCENT = { accent: [78, 168, 240], accentHi: [176, 222, 255], accentDk: [26, 76, 136] };
const CRYSTAL = { dark: [82, 32, 42], deep: [128, 50, 62], mid: [186, 92, 104], light: [224, 140, 150], hi: [246, 196, 202] };

// —— 形制 ——

// 流体/等离子罐侧面：金属罐身 + 竖向液位观察窗
function tankSide(px, w, h, a) {
  const M = TANK_METAL;
  plate(px, w, h, M.base, 13);
  border(px, w, h, 0, 0, w - 1, h - 1, M, 2);
  // 上下加强带
  rect(px, w, h, 2, 2, w - 3, 4, M.light);
  for (let x = 2; x <= w - 3; x++) put(px, w, h, x, 5, M.dark);
  rect(px, w, h, 2, h - 5, w - 3, h - 3, sh(M.base, -16));
  for (let x = 2; x <= w - 3; x++) put(px, w, h, x, h - 6, M.dark);
  // 观察窗
  const x0 = 8, y0 = 6, x1 = w - 9, y1 = h - 7;
  const gx0 = x0 + 1, gy0 = y0 + 1, gx1 = x1 - 1, gy1 = y1 - 1;
  const liqTop = gy0 + 5;
  for (let y = gy0; y <= gy1; y++) for (let x = gx0; x <= gx1; x++) {
    if (y < liqTop) put(px, w, h, x, y, mix([26, 30, 38], [14, 16, 22], (y - gy0) / 16));
    else put(px, w, h, x, y, mix(sh(a.accent, 20), a.accentDk, ((y - liqTop) / (gy1 - liqTop)) * 0.8));
  }
  for (let x = gx0; x <= gx1; x++) put(px, w, h, x, liqTop, a.accentHi);          // 液面
  for (let y = gy0 + 1; y <= gy1 - 1; y++) put(px, w, h, gx0 + 1, y, mix(a.accentHi, [255, 255, 255], 0.35)); // 玻璃高光
  border(px, w, h, x0, y0, x1, y1, M, 1);
  bolts4(px, w, h, M, 3, 3);
}

// 矿机升级块底面：接口板 + 散热格栅 + 家族色铭牌
function minerBottom(px, w, h, a) {
  const M = MINER_METAL;
  plate(px, w, h, M.base, 19);
  border(px, w, h, 0, 0, w - 1, h - 1, M, 2);
  recess(px, w, h, 8, 8, w - 9, h - 9, M.panel, M);
  // 散热格栅 4 条
  for (let i = 0; i < 4; i++) {
    const y = 14 + i * 8;
    rect(px, w, h, 14, y, w - 15, y + 3, sh(M.panel, -18));
    for (let x = 14; x <= w - 15; x++) put(px, w, h, x, y, sh(M.panel, 18));
  }
  // 中央家族色铭牌
  rect(px, w, h, 24, 46, w - 25, 52, a.accentDk);
  rect(px, w, h, 25, 47, w - 26, 51, a.accent);
  rect(px, w, h, 28, 48, w - 29, 49, a.accentHi);
  bolts4(px, w, h, M, 5, 6);
}

// 体扫描仪侧面：蓝色扫描窗 + 扫描线
function scannerSide(px, w, h) {
  const M = SCAN_METAL, a = SCAN_ACCENT;
  plate(px, w, h, M.base, 23);
  border(px, w, h, 0, 0, w - 1, h - 1, M, 2);
  // 左右导轨
  rect(px, w, h, 5, 6, 10, h - 7, M.light);
  rect(px, w, h, w - 11, 6, w - 6, h - 7, sh(M.base, -18));
  for (let y = 6; y <= h - 7; y++) { put(px, w, h, 10, y, M.dark); put(px, w, h, w - 11, y, M.dark); }
  // 扫描窗
  const x0 = 14, y0 = 10, x1 = w - 15, y1 = h - 11;
  rect(px, w, h, x0, y0, x1, y1, a.accentDk);
  rect(px, w, h, x0 + 1, y0 + 1, x1 - 1, y1 - 1, a.accent);
  const gx0 = x0 + 3, gy0 = y0 + 3, gx1 = x1 - 3, gy1 = y1 - 3;
  for (let y = gy0; y <= gy1; y++) for (let x = gx0; x <= gx1; x++)
    put(px, w, h, x, y, mix([30, 58, 96], [16, 30, 54], (y - gy0) / (gy1 - gy0)));
  for (let y = gy0; y <= gy1; y += 5) for (let x = gx0; x <= gx1; x++) put(px, w, h, x, y, sh(a.accent, -20)); // 扫描线
  line(px, w, h, gx0 + 1, gy1 - 2, gx1 - 1, gy0 + 2, mix(a.accentHi, [255, 255, 255], 0.25));              // 玻璃反光
  border(px, w, h, gx0 - 1, gy0 - 1, gx1 + 1, gy1 + 1, M, 1);
  bolts4(px, w, h, M, 5, 6);
}

// 体扫描仪顶面：中央扫描镜头 + 四角指示灯
function scannerTop(px, w, h) {
  const M = SCAN_METAL, a = SCAN_ACCENT;
  plate(px, w, h, M.base, 29);
  border(px, w, h, 0, 0, w - 1, h - 1, M, 2);
  recess(px, w, h, 6, 6, w - 7, h - 7, M.panel, M);
  const cx = w >> 1, cy = h >> 1;
  disc(px, w, h, cx, cy, 22, M.dark);
  disc(px, w, h, cx, cy, 20, sh(M.base, 12));
  for (let k = 0; k < 8; k++) {                                          // 环向卡榫
    const rad = k * Math.PI / 4;
    bolt(px, w, h, cx + Math.round(Math.cos(rad) * 18) - 2, cy + Math.round(Math.sin(rad) * 18) - 2, 4, M);
  }
  ring(px, w, h, cx, cy, 13, 16, a.accentDk);
  ring(px, w, h, cx, cy, 10, 13, a.accent);
  disc(px, w, h, cx, cy, 9, [24, 40, 64]);
  ring(px, w, h, cx, cy, 5, 7, a.accentHi);
  disc(px, w, h, cx, cy, 3, [255, 255, 255]);
  // 四角指示灯
  for (const [sx, sy] of [[11, 11], [w - 13, 11], [11, h - 13], [w - 13, h - 13]]) {
    rect(px, w, h, sx, sy, sx + 1, sy + 1, a.accentHi);
  }
  bolts4(px, w, h, M, 5, 6);
}

// 晶体块：菱形切面棱柱（与 crystal_cluster 同族粉色）
function crystalBlock(px, w, h) {
  const P = CRYSTAL;
  fill(px, w, h, P.deep);
  for (const [cx, cy] of [[8, 8], [24, 8], [8, 24], [24, 24]]) {
    const r = 8;
    for (let y = cy - r; y <= cy + r; y++) for (let x = cx - r; x <= cx + r; x++) {
      const dx = x - cx, dy = y - cy;
      if (Math.abs(dx) + Math.abs(dy) > r) continue;
      put(px, w, h, x, y, mix(P.hi, P.dark, ((dx - dy) / (2 * r) + 0.5) * 0.85)); // 左上亮、右下暗
    }
    const r2 = 4;                                                                 // 内层折射切面
    for (let y = cy - r2; y <= cy + r2; y++) for (let x = cx - r2; x <= cx + r2; x++) {
      const dx = x - cx, dy = y - cy;
      if (Math.abs(dx) + Math.abs(dy) > r2) continue;
      put(px, w, h, x, y, mix(P.light, P.mid, ((dx - dy) / (2 * r2) + 0.5) * 0.7));
    }
    for (let t = 0; t <= r; t++) {                                                // 上左棱高光 / 右下棱暗面
      put(px, w, h, cx - t, cy - r + t, P.hi);
      put(px, w, h, cx + r - t, cy + t, P.dark);
    }
  }
  for (const [sx, sy] of [[8, 8], [24, 8], [8, 24], [24, 24], [16, 16]]) put(px, w, h, sx, sy, [255, 236, 240]); // 晶体火花
}

// —— 任务表 ——
const ITEMS = [
  { n: 'akaishi_fluid_tank_basic_side', w: 32, h: 32, f: (px, w, h) => tankSide(px, w, h, TANK_ACCENT.basic) },
  { n: 'akaishi_fluid_tank_advanced_side', w: 32, h: 32, f: (px, w, h) => tankSide(px, w, h, TANK_ACCENT.advanced) },
  { n: 'akaishi_fluid_tank_super_side', w: 32, h: 32, f: (px, w, h) => tankSide(px, w, h, TANK_ACCENT.super) },
  { n: 'akaishi_plasma_tank_side', w: 32, h: 32, f: (px, w, h) => tankSide(px, w, h, TANK_ACCENT.plasma) },
  { n: 'akaishi_miner_fortune_upgrade_block_bottom', w: 64, h: 64, f: (px, w, h) => minerBottom(px, w, h, MINER_ACCENT.fortune) },
  { n: 'akaishi_miner_speed_upgrade_block_bottom', w: 64, h: 64, f: (px, w, h) => minerBottom(px, w, h, MINER_ACCENT.speed) },
  { n: 'akaishi_miner_storage_upgrade_block_bottom', w: 64, h: 64, f: (px, w, h) => minerBottom(px, w, h, MINER_ACCENT.storage) },
  { n: 'akaishi_body_scanner_side', w: 64, h: 64, f: scannerSide },
  { n: 'akaishi_body_scanner_top', w: 64, h: 64, f: scannerTop },
  { n: 'akaishi_crystal_block', w: 32, h: 32, f: crystalBlock },
];

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
    const src = path.join(DIR, it.n + '.png');
    const bak = path.join(BACKUP_DIR, it.n + '.png');
    if (!fs.existsSync(src)) { console.log('MISS ' + it.n); continue; }
    if (!fs.existsSync(bak)) fs.copyFileSync(src, bak);
    const px = Buffer.alloc(it.w * it.h * 4);
    it.f(px, it.w, it.h);
    grain(px, it.w, it.h, it.n.length);
    fs.writeFileSync(src, writePng(it.w, it.h, px));
    const colors = new Set();
    for (let i = 0; i < it.w * it.h; i++) colors.add(`${px[i * 4]},${px[i * 4 + 1]},${px[i * 4 + 2]}`);
    console.log(it.n.padEnd(46) + it.w + 'x' + it.h + ' colors=' + colors.size);
    cells.push({ px, w: it.w, h: it.h });
  }

  const CELL = 128, pad = 6, cols = 5;
  const rows = Math.ceil(cells.length / cols);
  const cw = CELL + pad * 2, ch = CELL + pad * 2;
  const PW = cw * cols, PH = ch * rows;
  const sheet = checker(PW, PH, 8);
  cells.forEach((c, i) => {
    const f = CELL / c.w;
    blit(sheet, PW, c.px, c.w, c.h, (i % cols) * cw + pad, Math.floor(i / cols) * ch + pad, f);
  });
  fs.writeFileSync(PREVIEW, writePng(PW, PH, sheet));
  console.log('共重制 ' + cells.length + ' 张剩余方块贴图');
  console.log('预览: ' + path.relative(ROOT, PREVIEW));
  return 0;
}

process.exit(main());
