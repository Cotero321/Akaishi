// 合金锭 + 通用部件塑形模板 图标重制（去黑边 / 精致化 / 模板感）。
// 目标（8 张，32x32 RGBA8，均走 item/generated + layer0）：
//   akaishi_ingot / akaishi_redstone_alloy_ingot / akaishi_resistant_steel_ingot /
//   akaishi_precision_alloy_ingot / akaishi_alloy_steel_ingot /
//   akaishi_psionic_composite_ingot / akaishi_life_fusion_ingot
//   akaishi_generic_part_mould —— 通用部件塑形模板
// 原理：
//   1) 锭（7 张）：**不改几何、不动本体**，只把原图那圈近黑描边（L≈15~33 的蓝黑，与材质无关）
//      替换成「材质色由深到浅的渐变」——按每个近黑像素在其自身黑阶里的相对亮度插值出浓度 k，
//      生成 mix(材质本色, 黑, k)，最黑处最深、次黑处稍浅，接上原图已有的材质深色面，
//      从而得到原版锭那种「暗缘→本色」的连续过渡，而不是死黑硬边。
//      材质本色由贴图自身众数推出（取亮度 100~190 的最常见色），故边缘色永远跟本体同色系。
//   1b) 三种钢锭（resistant_steel / precision_alloy / alloy_steel）原图字节完全相同，
//      但代码里是三种不同材料，须可辨：在描边重着色后再叠「色调偏移 + 表面纹样」——
//      冷银蓝+稀疏蚀点 / 亮中性+中段短竖刻 / 暖褐+稀疏碳化物斑点；几何同样不动。
//   2) 模板（1 张）：不通用于上述后处理——它是另绘的等距方形薄板（板厚 5），板面完全留白（空白素板），
//      只有方形轮廓 + 棱线明暗 + 颗粒。等距正方形投影后必为 2:1 菱形，任何与板面同心且闭合的
//      刻线/角标/浅腔都会被读成「眼球」，故板面不落任何内部刻纹，这正是未开型腔的通用模板形态。
//   3) 透明背景；锭侧不额外加噪点，保持原版质感。
// 幂等：源图一律取自备份（gui_layouts/ingot_mould_backup/，首次运行时自动建立），
//       故重复运行结果一致，不会把「已改」当「原图」再改一次。
// 用法：node rework_ingot_mould_assets.js            写入贴图 + 出预览
//       node rework_ingot_mould_assets.js --preview  仅出前后对照预览，不写贴图
//       --zoom [--mould] [--one=<name>]               放大自查
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/item');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/ingot_mould_backup');
const PREVIEW = path.join(ROOT, 'gui_layouts/ingot_mould_rework_preview.png');

const W = 32, H = 32, WHITE = [255, 255, 255], BLACK = [0, 0, 0];

// ---------- PNG 编码 ----------
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

// ---------- PNG 解码（仅 RGBA/RGB，用于预览里的「改前」对照） ----------
function readPng(file) {
  const b = fs.readFileSync(file);
  let o = 8, w = 0, h = 0, ct = 6, bd = 8;
  const idat = [];
  while (o + 8 <= b.length) {
    const len = b.readUInt32BE(o), tag = b.toString('ascii', o + 4, o + 8);
    const data = b.subarray(o + 8, o + 8 + len);
    if (tag === 'IHDR') { w = data.readUInt32BE(0); h = data.readUInt32BE(4); bd = data[8]; ct = data[9]; }
    else if (tag === 'IDAT') idat.push(data);
    else if (tag === 'IEND') break;
    o += 12 + len;
  }
  if (bd !== 8) throw new Error('仅支持 8bit PNG: ' + file);
  const bpp = ct === 6 ? 4 : ct === 2 ? 3 : 1;
  const stride = w * bpp;
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const out = Buffer.alloc(w * h * 4);
  const prev = Buffer.alloc(stride), cur = Buffer.alloc(stride);
  for (let y = 0; y < h; y++) {
    const ft = raw[y * (stride + 1)];
    raw.copy(cur, 0, y * (stride + 1) + 1, y * (stride + 1) + 1 + stride);
    for (let i = 0; i < stride; i++) {
      const a = i >= bpp ? cur[i - bpp] : 0, bb = prev[i], c = i >= bpp ? prev[i - bpp] : 0;
      let v = cur[i];
      if (ft === 1) v += a;
      else if (ft === 2) v += bb;
      else if (ft === 3) v += (a + bb) >> 1;
      else if (ft === 4) { const p = a + bb - c, pa = Math.abs(p - a), pb = Math.abs(p - bb), pc = Math.abs(p - c); v += (pa <= pb && pa <= pc) ? a : (pb <= pc ? bb : c); }
      cur[i] = v & 255;
    }
    for (let x = 0; x < w; x++) {
      const so = x * bpp, dof = (y * w + x) * 4;
      if (bpp >= 3) { out[dof] = cur[so]; out[dof + 1] = cur[so + 1]; out[dof + 2] = cur[so + 2]; out[dof + 3] = bpp === 4 ? cur[so + 3] : 255; }
      else { const g = cur[so]; out[dof] = g; out[dof + 1] = g; out[dof + 2] = g; out[dof + 3] = bpp === 2 ? cur[so + 1] : 255; }
    }
    prev.set(cur);
  }
  return { w, h, px: out };
}

// ---------- 基础绘制 ----------
function mix(a, b, t) { return [Math.round(a[0] + (b[0] - a[0]) * t), Math.round(a[1] + (b[1] - a[1]) * t), Math.round(a[2] + (b[2] - a[2]) * t)]; }
function put(px, x, y, c) { if (x < 0 || y < 0 || x >= W || y >= H) return; const o = (y * W + x) * 4; px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; px[o + 3] = 255; }
function mark(mask, x, y) { if (x < 0 || y < 0 || x >= W || y >= H) return; mask[y * W + x] = 1; }
function inPoly(pts, x, y) {
  let inside = false;
  for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
    const [xi, yi] = pts[i], [xj, yj] = pts[j];
    if ((yi > y) !== (yj > y) && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi) inside = !inside;
  }
  return inside;
}
function fillPoly(px, mask, pts, c) { for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) if (inPoly(pts, x, y)) { put(px, x, y, c); mark(mask, x, y); } }
// Bresenham 线段
function line(px, x0, y0, x1, y1, c) {
  x0 = Math.round(x0); y0 = Math.round(y0); x1 = Math.round(x1); y1 = Math.round(y1);
  const dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
  const sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
  let err = dx + dy;
  for (;;) {
    put(px, x0, y0, c);
    if (x0 === x1 && y0 === y1) break;
    const e2 = 2 * err;
    if (e2 >= dy) { err += dy; x0 += sx; }
    if (e2 <= dx) { err += dx; y0 += sy; }
  }
}
// 细微颗粒：打散死平色（金属质感），幅度小以免脏
function grain(px, mask, amp) {
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    if (!mask[y * W + x]) continue;
    const o = (y * W + x) * 4, n = (((x * 7 + y * 13) % 5) - 2) * amp;
    px[o] = Math.max(0, Math.min(255, px[o] + n));
    px[o + 1] = Math.max(0, Math.min(255, px[o + 1] + n));
    px[o + 2] = Math.max(0, Math.min(255, px[o + 2] + n));
  }
}

// ---------- 锭：保留原版几何，仅把近黑描边换成材质色渐变 ----------
// 原图每枚锭由两族色调构成：
//   本体族 —— 受光面/本色/亮面/中暗面/深暗面（本身已是材质色系）；
//   描边族 —— 一圈近黑（L≈15~33 的蓝黑，各材质共用，与本体色无关），正是「死黑硬边」的来源。
// 只动描边族：把它的若干黑阶按相对亮度映射到一条「材质色深→浅」渐变——
//   最深端 = 本体主色压深 K_DEEP（同色系的暗缘），
//   次深端 = 本体自身最暗的那一阶（于是与本体无缝衔接，不会出现「边缘比内侧还亮」的断层）。
const EDGE_L = 60;        // 亮度低于此值即判为描边族；本体最暗面 L≈85，安全隔离
const K_DEEP = 0.70;      // 最黑处相对本体主色的压深浓度
function lumOf(px, i) { const o = i * 4; return (px[o] * 299 + px[o + 1] * 587 + px[o + 2] * 114) / 1000; }
// 取本体两阶：主色（非描边像素中出现最多者，代表该材质本色）与最暗面（亮度最低者，用作渐变内端）
function bodyTones(src) {
  const cnt = new Map();
  let dark = null, darkL = Infinity;
  for (let i = 0; i < W * H; i++) {
    if (!src[i * 4 + 3]) continue;
    const L = lumOf(src, i);
    if (L < EDGE_L) continue;
    const c = [src[i * 4], src[i * 4 + 1], src[i * 4 + 2]];
    const k = c.join(',');
    cnt.set(k, (cnt.get(k) || 0) + 1);
    if (L < darkL) { darkL = L; dark = c; }
  }
  let base = null, n = -1;
  for (const [k, v] of cnt) if (v > n) { n = v; base = k.split(',').map(Number); }
  return { base: base || [128, 128, 128], dark: dark || [96, 96, 96] };   // 兜底：极端贴图
}
function recolorIngot(src) {
  const px = Buffer.from(src);
  const { base, dark } = bodyTones(src);
  const deep = mix(base, BLACK, K_DEEP);
  // 描边族的亮度上下界 → 渐变的两个端点
  let lo = Infinity, hi = -Infinity;
  for (let i = 0; i < W * H; i++) {
    if (!src[i * 4 + 3]) continue;
    const L = lumOf(src, i);
    if (L < EDGE_L) { if (L < lo) lo = L; if (L > hi) hi = L; }
  }
  for (let i = 0; i < W * H; i++) {
    if (!src[i * 4 + 3]) continue;
    const L = lumOf(src, i);
    if (L >= EDGE_L) continue;
    const t = hi > lo ? (L - lo) / (hi - lo) : 1;    // t=0 最黑（外缘）→ t=1 次黑（贴本体）
    const c = mix(deep, dark, t);
    const o = i * 4;
    px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2];
  }
  return { px, base };
}

// ---------- 三种钢锭差异化：色调 + 表面纹样（几何不变） ----------
// 耐蚀钢/精密合金/合金钢在代码里是三种不同材料，原图却字节相同、无法分辨。
// 分两步：① 整张（含描边）做同色系色调偏移；② 只在本体内缩区域叠表面纹样。
// 纹样限定在内缩本体，避免啃掉剪影；三者纹样形态不同（蚀点/刻线/碳斑），缩小后仍可辨。
const STEEL_VARIANTS = {
  akaishi_resistant_steel_ingot: { gain: [0.93, 1.00, 1.07], detail: pits },     // 冷银蓝：蚀点
  akaishi_precision_alloy_ingot: { gain: [1.07, 1.07, 1.05], detail: engrave }, // 亮中性：细刻线
  akaishi_alloy_steel_ingot: { gain: [1.08, 0.99, 0.87], detail: carbon },      // 暖褐：碳斑
};
function tint(px, gain) {
  for (let i = 0; i < W * H; i++) {
    if (!px[i * 4 + 3]) continue;
    const o = i * 4;
    px[o] = Math.min(255, Math.round(px[o] * gain[0]));
    px[o + 1] = Math.min(255, Math.round(px[o + 1] * gain[1]));
    px[o + 2] = Math.min(255, Math.round(px[o + 2] * gain[2]));
  }
}
// 内缩本体掩码：仅当自身与上下左右皆为「不透明且非描边」时才落纹样，保住剪影
function innerMask(px) {
  const m = new Uint8Array(W * H);
  const body = i => px[i * 4 + 3] !== 0 && lumOf(px, i) >= EDGE_L;
  for (let y = 1; y < H - 1; y++) for (let x = 1; x < W - 1; x++) {
    const i = y * W + x;
    if (body(i) && body(i - 1) && body(i + 1) && body(i - W) && body(i + W)) m[i] = 1;
  }
  return m;
}
function shade(px, i, k) { const o = i * 4, c = mix([px[o], px[o + 1], px[o + 2]], BLACK, k); px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; }
function bright(px, i, k) { const o = i * 4, c = mix([px[o], px[o + 1], px[o + 2]], WHITE, k); px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; }
function solid(m, x, y) { return x >= 0 && y >= 0 && x < W && y < H && m[y * W + x] === 1; }

// 二维整数哈希：给纹样定址，取代取模——取模在有限坐标域里会排出可见的斜向规律，哈希才是「无规则麻点」
function h2(x, y) {
  let h = Math.imul(x + 1013, 374761393) ^ Math.imul(y + 2027, 668265263);
  h = Math.imul(h ^ (h >>> 15), 1274126177);
  return (h ^ (h >>> 16)) >>> 0;
}
// 蚀点：稀疏 2x2 暗坑（哈希定址、无规则排布），耐蚀钢的锈蚀麻点
function pits(px, m) {
  for (let y = 1; y < H - 1; y++) for (let x = 1; x < W - 1; x++) {
    if (!solid(m, x, y) || !solid(m, x + 1, y + 1)) continue;
    if (h2(x, y) % 100 >= 3) continue;
    for (let dy = 0; dy < 2; dy++) for (let dx = 0; dx < 2; dx++) if (solid(m, x + dx, y + dy)) shade(px, (y + dy) * W + x + dx, 0.30);
  }
}
// 细刻线：本体中段四道短竖刻（阴刻），右缘留 1px 弱高光做倒角——竖向加工纹，区别于横向纹理
function engrave(px, m) {
  let x0 = W, y0 = H, x1 = -1, y1 = -1;
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) if (m[y * W + x]) {
    if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y;
  }
  if (x1 - x0 < 12 || y1 - y0 < 8) return;
  const cy = (y0 + y1) / 2, half = Math.max(2, Math.round((y1 - y0) * 0.27));
  for (const f of [0.22, 0.40, 0.60, 0.78]) {
    const x = Math.round(x0 + (x1 - x0) * f);
    for (let y = Math.round(cy - half); y <= Math.round(cy + half); y++) {
      if (!solid(m, x, y)) continue;
      shade(px, y * W + x, 0.24);
      if (solid(m, x + 1, y)) bright(px, y * W + x + 1, 0.08);
    }
  }
}
// 碳斑：稀疏 3x3 柔和暗斑（哈希定址、单向下压），体现合金钢的碳偏析又不破坏金属面
function carbon(px, m) {
  for (let y = 1; y < H - 1; y++) for (let x = 1; x < W - 1; x++) {
    if (!solid(m, x, y)) continue;
    if (h2(x, y) % 100 >= 4) continue;
    for (let dy = 0; dy < 3; dy++) for (let dx = 0; dx < 3; dx++) {
      if (solid(m, x + dx, y + dy)) shade(px, (y + dy) * W + x + dx, 0.10);
    }
  }
}

// ---------- 模板：等距方形薄板 · 空白素板（与锭同风格，厚度更薄） ----------
const MOULD = { base: [99, 127, 140] };
// 等距映射：u,v ∈ [-1,1] 为板面方形坐标；IX:IY 取 2:1 保证世界坐标下的正方形在投影后仍为正方菱形。
// TH 为板厚，取 5 使板「像锭但明显更薄」，四角落在 32x32 内且留出边距。
const ICX = 16, ICY = 13.5, IX = 7, IY = 3.5, TH = 5;
function iso(u, v) { return [ICX + (u - v) * IX, ICY + (u + v) * IY]; }
// 挤出：把顶面棱 a→b 向下拉 dy，构成侧面四边形（须传两个不同点，否则退化为线）
function extrude(px, mask, a, b, dy, c) { fillPoly(px, mask, [a, b, [b[0], b[1] + dy], [a[0], a[1] + dy]], c); }

function mould(px, pal) {
  const mask = new Uint8Array(W * H);
  const topCol = mix(pal.base, WHITE, 0.24);                              // 板面：受光（留本色，避免发灰）
  const darkFace = mix(pal.base, BLACK, 0.30);                            // 右侧面：背光
  const A = iso(-1, -1), B = iso(1, -1), C = iso(1, 1), D = iso(-1, 1);   // 上/右/下/左 顶点

  // 板体：可见的两个侧面（左下本色受光、右下背光）+ 顶面；先侧面后顶面，接缝由顶面覆盖
  extrude(px, mask, D, C, TH, pal.base);
  extrude(px, mask, C, B, TH, darkFace);
  fillPoly(px, mask, [A, B, C, D], topCol);

  // 侧面自身明暗收边（无中性黑描边）：底缘压暗、立柱棱分层
  line(px, D[0], D[1] + TH, C[0], C[1] + TH, mix(pal.base, BLACK, 0.50));
  line(px, C[0], C[1] + TH, B[0], B[1] + TH, mix(darkFace, BLACK, 0.30));
  line(px, C[0], C[1], C[0], C[1] + TH, mix(pal.base, BLACK, 0.45));     // 两侧面之间的竖棱
  line(px, D[0], D[1], D[0], D[1] + TH, mix(pal.base, BLACK, 0.30));
  line(px, B[0], B[1], B[0], B[1] + TH, mix(darkFace, BLACK, 0.20));
  // 顶面棱：近光边高光 / 远光边过渡
  line(px, A[0], A[1], D[0], D[1], mix(pal.base, WHITE, 0.55));
  line(px, A[0], A[1], B[0], B[1], mix(pal.base, WHITE, 0.35));
  line(px, D[0], D[1], C[0], C[1], mix(pal.base, BLACK, 0.20));          // 下左缘＝顶面折向侧面的折线，同锭口径压暗
  line(px, B[0], B[1], C[0], C[1], mix(pal.base, BLACK, 0.32));

  // 板面颗粒：打散死平色，幅度小以免脏
  grain(px, mask, 2);
  // 空白素板：板面不落任何内部刻纹。
  // 教训：等距正方形在屏幕上必为 2:1 菱形，凡与板面同心、封闭或近似封闭的刻线/角标/浅腔，
  // 都会被读成「眼球」「十字」或「虚线环」。故模板只靠「方形薄板 + 棱线明暗 + 颗粒」立形，
  // 板面留白——这正是「空白素板」的字面形态，也符合未开型腔的通用模板语义。
}

// ---------- 预览：上排改前 / 下排改后 ----------
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

// 预览顺序：模板置首（方形），随后 7 枚锭
const NAMES = ['akaishi_generic_part_mould', 'akaishi_ingot', 'akaishi_redstone_alloy_ingot',
  'akaishi_resistant_steel_ingot', 'akaishi_precision_alloy_ingot', 'akaishi_alloy_steel_ingot',
  'akaishi_psionic_composite_ingot', 'akaishi_life_fusion_ingot'];

// 源图：优先取备份（＝首次运行前的真原件），避免重复运行把「已改」当「原图」再改一次
function sourceOf(name) {
  const live = path.join(DIR, name + '.png'), bak = path.join(BACKUP_DIR, name + '.png');
  return readPng(fs.existsSync(bak) ? bak : live).px;
}

// 生成改后贴图：模板另绘，锭＝原图 + 描边重着色（三种钢锭再叠色调+纹样）；返回 base 供日志核对
function render(name, src) {
  if (name === 'akaishi_generic_part_mould') {
    const px = Buffer.alloc(W * H * 4);
    mould(px, MOULD);
    return { px, base: MOULD.base };
  }
  const r = recolorIngot(src);
  const v = STEEL_VARIANTS[name];
  if (!v) return r;
  tint(r.px, v.gain);
  v.detail(r.px, innerMask(r.px));
  return { px: r.px, base: r.base.map((c, i) => Math.min(255, Math.round(c * v.gain[i]))) };
}

// 不透明像素的包围盒（用于核对新旧图标占位是否一致）
function bbox(px) {
  let x0 = W, y0 = H, x1 = -1, y1 = -1;
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) if (px[(y * W + x) * 4 + 3]) {
    if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y;
  }
  return `${x1 - x0 + 1}x${y1 - y0 + 1} @${x0},${y0}`;
}

function main() {
  const previewOnly = process.argv.includes('--preview');
  fs.mkdirSync(path.dirname(PREVIEW), { recursive: true });

  const srcs = NAMES.map(sourceOf);
  const results = NAMES.map((n, i) => render(n, srcs[i]));

  NAMES.forEach((n, i) => {
    const { px, base } = results[i];
    let opaque = 0;
    const colors = new Set();
    for (let j = 0; j < W * H; j++) if (px[j * 4 + 3]) { opaque++; colors.add(`${px[j * 4]},${px[j * 4 + 1]},${px[j * 4 + 2]}`); }
    console.log(n.padEnd(34) + ' opaque=' + String(opaque).padStart(4) + ' colors=' + colors.size + ' base=rgb(' + base.join(',') + ')  改前 ' + bbox(srcs[i]) + ' -> 改后 ' + bbox(px));
  });

  if (!previewOnly) {
    fs.mkdirSync(BACKUP_DIR, { recursive: true });
    NAMES.forEach((n, i) => {
      const live = path.join(DIR, n + '.png'), bak = path.join(BACKUP_DIR, n + '.png');
      if (!fs.existsSync(bak)) fs.copyFileSync(live, bak);       // 仅首次备份原图，保证可回溯
      fs.writeFileSync(live, writePng(W, H, results[i].px));
    });
  }

  if (process.argv.includes('--zoom')) {         // 放大自查用：仅改后，--one=a,b 可指定多张
    const one = (process.argv.find(a => a.startsWith('--one=')) || '').split('=')[1];
    const list = one ? one.split(',') : (process.argv.includes('--mould')
      ? ['akaishi_generic_part_mould']
      : ['akaishi_generic_part_mould', 'akaishi_resistant_steel_ingot', 'akaishi_psionic_composite_ingot']);
    const f = list.length === 1 ? 14 : 8;
    const pad = 6, cw = W * f + pad * 2, ch = H * f + pad * 2;
    const PW = cw * list.length, PH = ch;
    const sheet = checker(PW, PH, 8);
    list.forEach((n, i) => blit(sheet, PW, render(n, sourceOf(n)).px, W, H, i * cw + pad, pad, f));
    const zp = PREVIEW.replace('.png', '_zoom.png');
    fs.writeFileSync(zp, writePng(PW, PH, sheet));
    console.log('放大自查: ' + path.relative(ROOT, zp));
    return 0;
  }

  const f = 4, pad = 6, cols = NAMES.length;
  const cw = W * f + pad * 2, ch = H * f + pad * 2;
  const PW = cw * cols, PH = ch * 2;
  const sheet = checker(PW, PH, 8);
  NAMES.forEach((n, i) => {
    blit(sheet, PW, srcs[i], W, H, i * cw + pad, pad, f);
    blit(sheet, PW, results[i].px, W, H, i * cw + pad, ch + pad, f);
  });
  fs.writeFileSync(PREVIEW, writePng(PW, PH, sheet));
  console.log('重制 ' + NAMES.length + ' 张贴图' + (previewOnly ? '（预览模式，未写盘）' : '（已写盘，原件已备份）'));
  console.log('预览（上排=改前 / 下排=改后）: ' + path.relative(ROOT, PREVIEW));
  return 0;
}

process.exit(main());
