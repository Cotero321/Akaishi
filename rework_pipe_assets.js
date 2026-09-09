// 管道家族贴图重制（工业机械 + 生物科技风）。
// 目标：block/akaishi_*pipe*.png 共 12 张，保留原有尺寸（32 或 64）。
// 原理：
//   1) 管道核心是 6/16 立方体（见 models/block/pipe/*），六面共用同一贴图，
//      因此图案必须中心对称、轮廓粗壮，缩小后仍可辨识；
//   2) 结构 = 金属外框(斜面高光/阴影) + 内凹面板 + 四角螺栓 + 中央能量核心(同心环 + 高光)；
//   3) 中央核心用「热芯/主环/中环/暗环」四段近似径向渐变，左上打高光；
//   4) 金属面叠加确定性噪声，避免死平色（同时提升调色板丰富度）；
//   5) 等级用面板底部指示块数量区分（1~4）。
// 幂等：首次运行备份原图到 gui_layouts/pipe_backup/，之后始终以备份为源。
// 用法：node rework_pipe_assets.js
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/block');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/pipe_backup');
const PREVIEW = path.join(ROOT, 'gui_layouts/pipe_rework_preview.png');

// 家族调色：metal 外壳基色，core 系列为核心同心环（由外到内）
const PALETTES = {
  energy:    { metal: [58, 60, 68], coreRim: [110, 28, 22], coreMid: [196, 58, 38], core: [255, 112, 56], hot: [255, 214, 150] },
  exhausted: { metal: [50, 48, 62], coreRim: [66, 32, 104], coreMid: [124, 66, 184], core: [196, 132, 255], hot: [240, 214, 255] },
  fluid:     { metal: [54, 60, 72], coreRim: [20, 66, 116], coreMid: [40, 120, 198], core: [92, 190, 255], hot: [206, 240, 255] },
  item:      { metal: [62, 58, 50], coreRim: [116, 78, 20], coreMid: [196, 138, 40], core: [255, 200, 92], hot: [255, 240, 190] },
  waste:     { metal: [54, 60, 54], coreRim: [38, 88, 34], coreMid: [82, 158, 58], core: [150, 220, 110], hot: [222, 255, 196] },
  plasma:    { metal: [58, 52, 66], coreRim: [108, 20, 88], coreMid: [198, 50, 158], core: [255, 122, 220], hot: [255, 210, 246] },
};

const ITEMS = [
  { name: 'akaishi_energy_pipe', pal: 'energy', tier: 1 },
  { name: 'akaishi_energy_pipe_advanced', pal: 'energy', tier: 2 },
  { name: 'akaishi_energy_pipe_elite', pal: 'energy', tier: 3 },
  { name: 'akaishi_energy_pipe_ultimate', pal: 'energy', tier: 4 },
  { name: 'akaishi_exhausted_pipe', pal: 'exhausted', tier: 1 },
  { name: 'akaishi_fluid_pipe', pal: 'fluid', tier: 1 },
  { name: 'akaishi_item_pipe', pal: 'item', tier: 1 },
  { name: 'akaishi_item_pipe_advanced', pal: 'item', tier: 2 },
  { name: 'akaishi_item_pipe_elite', pal: 'item', tier: 3 },
  { name: 'akaishi_item_pipe_ultimate', pal: 'item', tier: 4 },
  { name: 'akaishi_multi_fluid_waste_pipe', pal: 'waste', tier: 1 },
  { name: 'akaishi_plasma_pipe', pal: 'plasma', tier: 1 },
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

function mix(a, b, t) { return [Math.round(a[0] + (b[0] - a[0]) * t), Math.round(a[1] + (b[1] - a[1]) * t), Math.round(a[2] + (b[2] - a[2]) * t)]; }
function clamp(c) { return [Math.max(0, Math.min(255, c[0])), Math.max(0, Math.min(255, c[1])), Math.max(0, Math.min(255, c[2]))]; }
function shade(c, d) { return clamp([c[0] + d, c[1] + d, c[2] + d]); }
// 确定性噪声：让金属面有细微颗粒，避免死平色
function hash(x, y, s) {
  let h = Math.imul(x, 374761393) ^ Math.imul(y, 668265263) ^ Math.imul(s, 362437);
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return ((h ^ (h >>> 16)) >>> 0) % 9 - 4;
}

function put(buf, S, x, y, c, a = 255) {
  if (x < 0 || y < 0 || x >= S || y >= S) return;
  const o = (y * S + x) * 4; buf[o] = c[0]; buf[o + 1] = c[1]; buf[o + 2] = c[2]; buf[o + 3] = a;
}
function rect(buf, S, x0, y0, x1, y1, c) { for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) put(buf, S, x, y, c); }

// 绘制管道核心
function drawPipe(S, pal, tier) {
  const px = Buffer.alloc(S * S * 4);
  const cx = (S - 1) / 2, cy = (S - 1) / 2;
  const t = Math.max(2, Math.round(S / 16));        // 外框厚度
  const inset = t + Math.max(1, Math.round(S / 32)); // 内面板内缩
  const dark = [24, 26, 30];                         // 轮廓/接缝
  const bolt = [34, 36, 42];                         // 螺栓

  // 1) 金属外壳（带噪声）
  for (let y = 0; y < S; y++) for (let x = 0; x < S; x++) {
    const edge = Math.min(x, y, S - 1 - x, S - 1 - y);
    let c = shade(pal.metal, hash(x, y, S));
    if (edge < t) { // 外框斜面：左上受光，右下背光
      const lit = (x < y) || (x + y < S - 1);
      c = shade(pal.metal, lit ? 14 : -16);
    }
    put(px, S, x, y, c);
  }
  // 外框轮廓线
  rect(px, S, 0, 0, S - 1, 0, dark); rect(px, S, 0, S - 1, S - 1, S - 1, dark);
  rect(px, S, 0, 0, 0, S - 1, dark); rect(px, S, S - 1, 0, S - 1, S - 1, dark);

  // 2) 内凹面板
  const p0 = inset, p1 = S - 1 - inset;
  rect(px, S, p0, p0, p1, p1, shade(pal.metal, -6));
  // 面板凹陷阴影（左上）与反光（右下）
  rect(px, S, p0, p0, p1, p0, shade(pal.metal, -22));
  rect(px, S, p0, p0, p0, p1, shade(pal.metal, -22));
  rect(px, S, p0, p1, p1, p1, shade(pal.metal, 12));
  rect(px, S, p1, p0, p1, p1, shade(pal.metal, 12));

  // 3) 四角螺栓
  const bs = Math.max(1, Math.round(S / 32));
  const bpos = [p0 + bs, p1 - bs];
  for (const bx of bpos) for (const by of bpos) {
    rect(px, S, bx - bs, by - bs, bx + bs, by + bs, dark);
    rect(px, S, bx - bs + 1, by - bs + 1, bx + bs - 1, by + bs - 1, bolt);
    put(px, S, bx - bs + 1, by - bs + 1, shade(bolt, 30));
  }

  // 4) 中央能量核心（同心环 + 左上高光）
  const r = Math.round(S * 0.22);
  for (let dy = -r - 2; dy <= r + 2; dy++) for (let dx = -r - 2; dx <= r + 2; dx++) {
    const d = Math.hypot(dx, dy);
    if (d > r + 1.5) continue;
    let c;
    if (d <= r * 0.32) c = pal.hot;
    else if (d <= r * 0.6) c = pal.core;
    else if (d <= r * 0.85) c = pal.coreMid;
    else c = pal.coreRim;
    // 高光：左上扇区提亮
    if (d <= r * 0.62 && dx <= 0 && dy <= 0) c = mix(c, [255, 255, 255], 0.32);
    // 轮廓
    if (d > r + 0.4) c = dark;
    put(px, S, Math.round(cx + dx), Math.round(cy + dy), c);
  }

  // 5) 等级指示块（面板底部中央）
  const pip = Math.max(1, Math.round(S / 32));
  const gap = pip + Math.max(1, Math.round(S / 32));
  const totalW = tier * pip + (tier - 1) * gap;
  let sx = Math.round(cx - totalW / 2);
  const py = p1 - pip * 2;
  for (let i = 0; i < tier; i++) {
    rect(px, S, sx, py, sx + pip - 1, py + pip - 1, dark);
    rect(px, S, sx, py, sx + pip - 1, py + pip - 2, pal.core);
    sx += pip + gap;
  }
  return px;
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
    const dof = ((oy + y) * DW + ox + x) * 4;
    dst[dof] = src[so]; dst[dof + 1] = src[so + 1]; dst[dof + 2] = src[so + 2]; dst[dof + 3] = 255;
  }
}

function pngSize(file) {
  const b = fs.readFileSync(file);
  return { w: b.readUInt32BE(16), h: b.readUInt32BE(20) };
}

function main() {
  fs.mkdirSync(BACKUP_DIR, { recursive: true });
  const cells = [];
  for (const it of ITEMS) {
    const src = path.join(DIR, it.name + '.png');
    const bak = path.join(BACKUP_DIR, it.name + '.png');
    if (!fs.existsSync(bak)) fs.copyFileSync(src, bak);
    const { w: S } = pngSize(bak);
    const px = drawPipe(S, PALETTES[it.pal], it.tier);
    fs.writeFileSync(src, writePng(S, S, px));
    const colors = new Set();
    for (let i = 0; i < S * S; i++) colors.add(`${px[i * 4]},${px[i * 4 + 1]},${px[i * 4 + 2]}`);
    console.log(it.name.padEnd(34) + ' ' + S + 'x' + S + '  tier=' + it.tier + '  colors=' + colors.size);
    cells.push({ px, S });
  }

  const f = 3, pad = 6;
  const cols = 6, rows = Math.ceil(cells.length / cols);
  const cw = 64 * f + pad * 2, ch = 64 * f + pad * 2;
  const PW = cw * cols, PH = ch * rows;
  const sheet = checker(PW, PH, 8);
  cells.forEach((c, i) => {
    const ox = (i % cols) * cw + pad + Math.floor((64 * f - c.S * f) / 2);
    const oy = Math.floor(i / cols) * ch + pad + Math.floor((64 * f - c.S * f) / 2);
    blit(sheet, PW, c.px, c.S, c.S, ox, oy, f);
  });
  fs.writeFileSync(PREVIEW, writePng(PW, PH, sheet));
  console.log('共重制 ' + cells.length + ' 张管道贴图');
  console.log('预览: ' + path.relative(ROOT, PREVIEW));
  return 0;
}

process.exit(main());
