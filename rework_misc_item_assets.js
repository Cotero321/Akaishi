// 零散物品图标重制（工业机械 + 生物科技风）。
// 目标（8 张，32x32 RGBA8）：
//   dragon_mixture / end_mixture —— 密封试剂瓶（瓶身玻璃 + 有色液体 + 瓶塞）
//   fuel_cell —— 燃料罐（金属罐体 + 观察窗液位）
//   portable_akaishi_cell_basic/advanced/super —— 便携能量单元（电池 + 发光核心窗）
//   cooling_base —— 冷却基底（底板 + 蛇形冷却管）
//   akaishi_redstone_alloy_ingot —— 红石合金锭（斜面锭体 + 红石纹路）
// 统一规范：透明底 + 厚暗色轮廓 + 左上受光/右下背光 + 细微颗粒。
// 幂等：首次运行备份原图到 gui_layouts/misc_item_backup/，之后始终以备份为源。
// 用法：node rework_misc_item_assets.js
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/item');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/misc_item_backup');
const PREVIEW = path.join(ROOT, 'gui_layouts/misc_item_rework_preview.png');

const W = 32, H = 32;
const OUT = [18, 20, 24];
const GLASS = [206, 224, 236];

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

// 试剂瓶：瓶塞 + 玻璃瓶身 + 有色液体
function vial(px, pal) {
  const mask = new Uint8Array(W * H);
  fillRect(px, mask, 12, 6, 19, 9, pal.dark);          // 瓶塞
  fillRect(px, mask, 13, 6, 18, 8, pal.light);
  fillRect(px, mask, 11, 10, 20, 25, GLASS);           // 玻璃瓶身
  fillRect(px, mask, 12, 12, 19, 24, pal.base);        // 液体
  fillRect(px, mask, 12, 12, 19, 13, pal.light);       // 液面
  for (let y = 15; y <= 22; y++) put(px, 12, y, pal.light);   // 玻璃高光
  put(px, 12, 15, [255, 255, 255]); put(px, 13, 15, [255, 255, 255]);
  outline(px, mask); grain(px, mask);
}

// 燃料罐：罐体 + 顶部注口 + 观察窗液位
function canister(px, pal) {
  const mask = new Uint8Array(W * H);
  fillRect(px, mask, 10, 8, 21, 26, pal.base);         // 罐体
  fillRect(px, mask, 12, 6, 19, 8, pal.dark);          // 注口
  fillRect(px, mask, 13, 5, 18, 6, pal.base);
  for (let y = 8; y <= 26; y++) { put(px, 10, y, pal.light); put(px, 21, y, pal.dark); }
  fillRect(px, mask, 13, 12, 18, 23, pal.dark);        // 观察窗底
  fillRect(px, mask, 13, 16, 18, 23, pal.liquid);      // 液位
  fillRect(px, mask, 13, 16, 18, 16, pal.light);
  put(px, 13, 12, [255, 255, 255]);                    // 窗反光
  outline(px, mask); grain(px, mask);
}

// 便携能量单元：电池 + 正极头 + 发光核心窗
function battery(px, pal) {
  const mask = new Uint8Array(W * H);
  fillRect(px, mask, 9, 8, 22, 26, pal.base);          // 外壳
  fillRect(px, mask, 13, 5, 18, 7, pal.dark);          // 正极头
  fillRect(px, mask, 14, 5, 17, 6, pal.light);
  for (let y = 8; y <= 26; y++) { put(px, 9, y, pal.light); put(px, 22, y, pal.dark); }
  fillRect(px, mask, 12, 11, 19, 22, OUT);             // 核心窗凹槽
  fillRect(px, mask, 13, 12, 18, 21, pal.core);        // 发光核心
  fillRect(px, mask, 13, 12, 18, 13, pal.light);
  put(px, 14, 14, [255, 255, 255]); put(px, 15, 14, [255, 255, 255]);
  fillRect(px, mask, 10, 24, 21, 25, pal.dark);        // 底部加强筋
  outline(px, mask); grain(px, mask);
}

// 冷却基底：底板 + 蛇形冷却管
function coolingBase(px, pal) {
  const mask = new Uint8Array(W * H);
  fillRect(px, mask, 5, 9, 26, 24, pal.base);          // 底板
  for (let x = 5; x <= 26; x++) { put(px, x, 9, pal.light); put(px, x, 24, pal.dark); }
  for (let y = 9; y <= 24; y++) { put(px, 5, y, pal.light); put(px, 26, y, pal.dark); }
  // 四角螺栓
  for (const [bx, by] of [[7, 11], [24, 11], [7, 22], [24, 22]]) {
    put(px, bx, by, pal.dark); put(px, bx + 1, by, pal.dark);
    put(px, bx, by + 1, pal.dark); put(px, bx + 1, by + 1, pal.light);
  }
  // 蛇形冷却管（2px，冷媒青色）
  const hseg = (x0, x1, y) => { for (let x = x0; x <= x1; x++) { put(px, x, y, pal.pipe); put(px, x, y + 1, pal.pipeDark); mark(mask, x, y); mark(mask, x, y + 1); } };
  const vseg = (x, y0, y1) => { for (let y = y0; y <= y1; y++) { put(px, x, y, pal.pipe); put(px, x + 1, y, pal.pipeDark); mark(mask, x, y); mark(mask, x + 1, y); } };
  hseg(9, 22, 12);
  vseg(21, 13, 16);
  hseg(9, 22, 17);
  vseg(9, 18, 21);
  hseg(9, 22, 21);
  for (let x = 9; x <= 22; x++) put(px, x, 12, pal.pipeHi);   // 管口高光
  outline(px, mask); grain(px, mask);
}

// 红石合金锭：斜面锭体 + 红石纹路
function ingot(px, pal) {
  const mask = new Uint8Array(W * H);
  // 顶面（向右上倾斜的平行四边形）
  for (let y = 10; y <= 14; y++) { const off = 14 - y; for (let x = 9 + off; x <= 23 + off; x++) { put(px, x, y, pal.light); mark(mask, x, y); } }
  // 正面
  fillRect(px, mask, 8, 15, 24, 22, pal.base);
  // 底边阴影
  fillRect(px, mask, 9, 23, 23, 23, pal.dark);
  // 侧面受光/背光
  for (let y = 15; y <= 23; y++) { put(px, 8, y, pal.light); put(px, 24, y, pal.dark); }
  // 红石纹路
  for (let x = 11; x <= 21; x += 2) { put(px, x, 17, pal.glow); put(px, x + 1, 20, pal.glow); }
  outline(px, mask); grain(px, mask);
}

const PALS = {
  dragon_mixture: { base: [168, 44, 120], light: [240, 116, 194], dark: [86, 18, 66] },
  end_mixture: { base: [86, 70, 190], light: [162, 152, 255], dark: [42, 32, 108] },
  fuel_cell: { base: [98, 98, 106], liquid: [236, 168, 56], light: [255, 216, 124], dark: [52, 52, 58] },
  portable_akaishi_cell_basic: { base: [96, 100, 110], core: [220, 64, 56], light: [255, 142, 122], dark: [48, 50, 56] },
  portable_akaishi_cell_advanced: { base: [86, 94, 112], core: [72, 150, 240], light: [152, 208, 255], dark: [42, 48, 62] },
  portable_akaishi_cell_super: { base: [104, 88, 124], core: [176, 110, 240], light: [228, 182, 255], dark: [54, 42, 70] },
  cooling_base: { base: [110, 124, 138], fin: [156, 174, 190], light: [204, 220, 234], dark: [60, 70, 82], pipe: [64, 176, 190], pipeDark: [32, 104, 120], pipeHi: [156, 236, 240] },
  akaishi_redstone_alloy_ingot: { base: [150, 44, 40], light: [216, 78, 64], dark: [84, 20, 20], glow: [255, 122, 92] },
};
const KINDS = {
  dragon_mixture: vial, end_mixture: vial,
  fuel_cell: canister,
  portable_akaishi_cell_basic: battery, portable_akaishi_cell_advanced: battery, portable_akaishi_cell_super: battery,
  cooling_base: coolingBase,
  akaishi_redstone_alloy_ingot: ingot,
};

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
  const names = Object.keys(KINDS);
  const cells = [];
  for (const n of names) {
    const src = path.join(DIR, n + '.png');
    const bak = path.join(BACKUP_DIR, n + '.png');
    if (!fs.existsSync(bak)) fs.copyFileSync(src, bak);
    const px = Buffer.alloc(W * H * 4);
    KINDS[n](px, PALS[n]);
    fs.writeFileSync(src, writePng(W, H, px));
    const colors = new Set();
    for (let i = 0; i < W * H; i++) if (px[i * 4 + 3]) colors.add(`${px[i * 4]},${px[i * 4 + 1]},${px[i * 4 + 2]}`);
    console.log(n.padEnd(32) + ' colors=' + colors.size);
    cells.push(px);
  }

  const f = 4, pad = 6, cols = 4;
  const rows = Math.ceil(cells.length / cols);
  const cw = W * f + pad * 2, ch = H * f + pad * 2;
  const PW = cw * cols, PH = ch * rows;
  const sheet = checker(PW, PH, 8);
  cells.forEach((px, i) => blit(sheet, PW, px, W, H, (i % cols) * cw + pad, Math.floor(i / cols) * ch + pad, f));
  fs.writeFileSync(PREVIEW, writePng(PW, PH, sheet));
  console.log('共重制 ' + cells.length + ' 张图标');
  console.log('预览: ' + path.relative(ROOT, PREVIEW));
  return 0;
}

process.exit(main());
