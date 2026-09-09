// activated 混合物「组件 / 晶体」物品图标重制（工业机械 + 生物科技风）。
// 目标贴图 32x32 RGBA8（与 models/item/akaishi_activated_*.json 的 item/generated 一致）。
// 原理：
//   1) 7 个家族各持「暗色底 + 亮色强调」一对识别色，与早期预览配色保持一致；
//   2) 晶体 = 多面宝石（暗色轮廓 + 受光面 + 高光条 + 核心宝石 + 底部碎屑）；
//   3) 组件 = 电路板（板底 + 走线 + 中央芯片 + 铜引脚），延续 akaishi_advanced_component 的形制；
//   4) 透明背景 + 厚暗色轮廓，与项目内既有 32x32 物品图标风格统一。
// 幂等：首次运行备份原图到 gui_layouts/activated_mixture_backup/，之后始终以备份为源。
// 用法：node rework_activated_mixture_assets.js
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/item');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/activated_mixture_backup');
const PREVIEW = path.join(ROOT, 'gui_layouts/activated_mixture_rework_preview.png');

const W = 32, H = 32;
const OUT = [18, 20, 24];            // 统一暗色轮廓
const COPPER = [201, 138, 66];       // 铜引脚
const SILICON = [24, 27, 32];        // 芯片基体

// 家族识别色：dark 为底/暗面，light 为受光面/核心
const FAMILIES = {
  advanced_mixture: { dark: [42, 143, 155], light: [84, 214, 220] },
  dragon: { dark: [111, 30, 69], light: [196, 76, 255] },
  end_mixture: { dark: [77, 89, 184], light: [169, 156, 255] },
  nether_compound: { dark: [169, 53, 38], light: [255, 107, 53] },
  pure: { dark: [91, 159, 114], light: [217, 245, 197] },
  sculk: { dark: [57, 77, 85], light: [136, 217, 232] },
  ultimate_mixture: { dark: [135, 77, 181], light: [243, 184, 255] },
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
function fillRect(px, mask, x0, y0, x1, y1, c) { for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) { put(px, x, y, c); mark(mask, x, y); } }
// 沿实体轮廓外扩 1px 描边
function outline(px, mask) {
  const add = [];
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    if (mask[y * W + x]) continue;
    let adj = false;
    for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
      const nx = x + dx, ny = y + dy;
      if (nx >= 0 && ny >= 0 && nx < W && ny < H && mask[ny * W + nx]) { adj = true; break; }
    }
    if (adj) add.push([x, y]);
  }
  for (const [x, y] of add) put(px, x, y, OUT);
}

// 晶体：多面宝石
function crystal(px, pal) {
  const mask = new Uint8Array(W * H);
  const cx = 16;
  const rows = { 4: 2, 5: 3, 6: 4, 7: 5, 8: 6, 9: 7, 10: 7, 11: 8, 12: 8, 13: 8, 14: 8, 15: 8, 16: 8, 17: 8, 18: 8, 19: 7, 20: 7, 21: 6, 22: 6, 23: 5, 24: 4, 25: 3, 26: 2, 27: 1 };
  for (const key of Object.keys(rows)) {
    const y = +key, hw = rows[key];
    for (let x = cx - hw; x < cx + hw; x++) {
      let c;
      if (x <= cx - 3) c = mix(pal.dark, pal.light, 0.55);        // 受光面
      else if (x <= cx + 1) c = pal.dark;                          // 主面
      else c = mix(pal.dark, OUT, 0.35);                           // 背光面
      put(px, x, y, c); mark(mask, x, y);
    }
  }
  for (let y = 8; y <= 18; y++) { put(px, cx - 5, y, pal.light); put(px, cx - 4, y, mix(pal.light, [255, 255, 255], 0.25)); }
  // 核心宝石 + 高光
  fillRect(px, mask, cx - 1, 13, cx + 1, 17, pal.light);
  put(px, cx - 1, 14, mix(pal.light, [255, 255, 255], 0.55));
  // 底部碎屑
  fillRect(px, mask, 4, 23, 5, 24, mix(pal.dark, OUT, 0.2));
  fillRect(px, mask, 26, 22, 27, 23, mix(pal.dark, OUT, 0.2));
  outline(px, mask);
}

// 组件：电路板
function component(px, pal) {
  const mask = new Uint8Array(W * H);
  const bx0 = 6, bx1 = 25, by0 = 7, by1 = 24;
  fillRect(px, mask, bx0, by0, bx1, by1, pal.dark);
  // 板面受光/背光边
  for (let x = bx0; x <= bx1; x++) { put(px, x, by0, mix(pal.dark, pal.light, 0.45)); put(px, x, by1, mix(pal.dark, OUT, 0.3)); }
  for (let y = by0; y <= by1; y++) { put(px, bx0, y, mix(pal.dark, pal.light, 0.45)); put(px, bx1, y, mix(pal.dark, OUT, 0.3)); }
  // 走线（L 形）
  for (let x = 8; x <= 14; x++) put(px, x, 10, pal.light);
  for (let y = 10; y <= 16; y++) put(px, 8, y, pal.light);
  for (let x = 17; x <= 23; x++) put(px, x, 20, mix(pal.dark, pal.light, 0.7));
  for (let y = 13; y <= 20; y++) put(px, 23, y, mix(pal.dark, pal.light, 0.7));
  // 中央芯片 + 核心
  fillRect(px, mask, 11, 12, 20, 19, SILICON);
  fillRect(px, mask, 14, 14, 17, 17, pal.light);
  put(px, 14, 14, mix(pal.light, [255, 255, 255], 0.5));
  // 铜引脚
  for (const y of [10, 13, 16, 19]) {
    fillRect(px, mask, 4, y, 5, y, COPPER);
    fillRect(px, mask, 26, y, 27, y, COPPER);
  }
  for (const x of [9, 13, 17, 21]) fillRect(px, mask, x, 25, x, 26, COPPER);
  outline(px, mask);
}

// 预览：棋盘底 + 4x 最近邻放大，上排晶体 / 下排组件
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
  const fams = Object.keys(FAMILIES);
  const cells = [];
  for (const fam of fams) {
    const pal = FAMILIES[fam];
    const pair = {};
    for (const kind of ['component', 'crystal']) {
      const name = `akaishi_activated_${fam}_${kind}`;
      const bak = path.join(BACKUP_DIR, name + '.png');
      if (!fs.existsSync(bak)) fs.copyFileSync(path.join(DIR, name + '.png'), bak);
      const px = Buffer.alloc(W * H * 4);
      if (kind === 'crystal') crystal(px, pal); else component(px, pal);
      fs.writeFileSync(path.join(DIR, name + '.png'), writePng(W, H, px));
      pair[kind] = px;
      let opaque = 0;
      for (let i = 3; i < px.length; i += 4) if (px[i] === 255) opaque++;
      console.log(name.padEnd(46) + ' dark rgb(' + pal.dark.join(',') + ')  light rgb(' + pal.light.join(',') + ')  覆盖 ' + opaque);
    }
    cells.push(pair);
  }

  const f = 4, pad = 6;
  const cw = W * f + pad * 2, ch = H * f + pad * 2;
  const PW = cw * fams.length, PH = ch * 2;
  const sheet = checker(PW, PH, 8);
  cells.forEach((pair, i) => {
    blit(sheet, PW, pair.crystal, W, H, i * cw + pad, pad, f);
    blit(sheet, PW, pair.component, W, H, i * cw + pad, ch + pad, f);
  });
  fs.writeFileSync(PREVIEW, writePng(PW, PH, sheet));
  console.log('共重制 ' + (fams.length * 2) + ' 张（7 家族 x 组件/晶体）');
  console.log('预览: ' + path.relative(ROOT, PREVIEW));
  return 0;
}

process.exit(main());
