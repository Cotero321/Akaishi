// 赤石/生命装备图标「去模糊」+ 赤石工具「对齐原版工具换色调优」。
//
// 诊断依据（原图 HIST/alpha 实测）：
//   装备 8 张图标含 48~116 个半透明抗锯齿像素（全模组 129 张里 115 张软边=0，装备 8 张是唯一异常源），
//   且内部是平滑灰阶渐变 → 缩进 16px 物品栏格必然糊成一片。工具 4 张则相反：0 软边但含 109~112 个
//   25,33,45 纯蓝黑厚描边，且头部为钢蓝 `99,127,140`/`175,200,199`，与赤石家族赤红完全脱节。
//
// 处理原理（不动机位、不描新几何，只重映射「色」与「边」）：
//   1) 装备：alpha 二值化（<128 透明 / >=128 实心）消除软边；再按部件分组——
//      彩度 chroma>=63 的宝石/能源饰件走家族发光色，其余灰阶本体走家族金属色阶，
//      每组按图像内亮度百分位铺满整条色带，得到「深描边→中间调→高光」都拉得开的原版级有限调色板。
//      另外头盔原图外圈多画了一圈装饰性长方框，按成品要求整圈剔除（见 stripHelmetFrame），
//      并清掉框线右侧单侧的半透明抗锯齿列、把左半镜像到右半，得到严格左右对称的盔体（见 mirrorHelmet）。
//   2) 工具：直接采用原版下界合金工具贴图为底（gui_layouts/netherite_ref/，16x16），
//      按亮度百分位换色为赤石金属色带，再 2x 最近邻升到 32x32（保持像素块，与全模组 32x32 规范一致），
//      最后做一次内倒角细化（见 bevelDetail），得到「下界工具形状 + 赤石换色 + 自带细节」的成品。
// 手持外观与物品栏共用同一贴图（models/item/akaishi_*.json 用 item/handheld），贴图更新即同步。
//
// 幂等：首次运行备份原图到 gui_layouts/equipment_tool_backup/；工具以 netherite_ref 为源，天然幂等。
// 用法：node rework_equipment_tool_assets.js [--preview] [--one=a,b] [--dump]
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/item');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/equipment_tool_backup');
const PREVIEW_EQ = path.join(ROOT, 'gui_layouts/equipment_rework_preview.png');
const PREVIEW_TL = path.join(ROOT, 'gui_layouts/tool_rework_preview.png');

const W = 32, H = 32;

const CRC_TABLE = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1); t[n] = c; }
  return t;
})();
function crc32(buf) { let c = 0xFFFFFFFF; for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 255] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; }

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

// ---------- 颜色度量 ----------
const lum = (r, g, b) => (r * 299 + g * 587 + b * 114) / 1000;

// ---------- 家族台阶色带（深 → 浅） ----------
// 金属阶直接搬用赤石锭 / 生命融合锭的「实测 8 阶调色板」，装备与工具因此与锭同族同色，
// 不会再出现自造过渡色导致的偏粉 / 偏灰。关键：只给「颜色」不给「亮度锚点」，
// 分档改由图像内亮度百分位决定（见 bandMap），否则大片中间调会被最近锚点吸到同一档而塌陷。
const RAMP = {
  akaishi_metal: [[68, 42, 45], [89, 49, 54], [101, 54, 60], [131, 64, 73], [171, 83, 94], [219, 107, 121], [227, 140, 150], [235, 174, 181]],
  life_fusion_metal: [[43, 58, 50], [45, 79, 63], [46, 92, 70], [67, 104, 84], [87, 136, 109], [111, 174, 140], [143, 192, 165], [176, 210, 192]],
  akaishi_gem: [[110, 24, 28], [150, 34, 38], [190, 48, 50], [224, 72, 66], [255, 124, 104]],   // 目镜/面罩发光件：赤红系（原蓝件被染成橙黄，按成品要求改红）
  life_fusion_gem: [[30, 104, 86], [46, 150, 124], [66, 208, 176], [104, 226, 192], [140, 235, 205]],
};

// 按「同组内亮度百分位」把像素铺满整条色带：深描边必落在最暗档、高光必落在最亮档，
// 中间调也能拉开 3~4 档，等价于给每个部件做一次对比度归一，杜绝塌陷。
// topOnly：只允许使用色带上半段的比例（0~1）。小面积发光饰件（宝石/护目镜/工具镶嵌）
// 用它避免本来就偏暗的像素落进色带最暗档、褪成一块脏棕，保证始终「亮得起来」。
function bandMap(list, ramp, topOnly = 0) {
  if (!list.length) return;
  const n = ramp.length;
  const ls = list.map(p => p.L).sort((a, b) => a - b);
  let lo = ls[Math.floor(ls.length * 0.02)], hi = ls[Math.min(ls.length - 1, Math.floor(ls.length * 0.98))];
  const span = Math.max(40, hi - lo);            // 单色调部件（如纯色手柄）也居中落到中档，而不是被压成最深
  lo -= (span - (hi - lo)) / 2;
  const base = Math.min(n - 1, Math.floor(n * topOnly));
  for (const p of list) {
    const t = Math.min(1, Math.max(0, (p.L - lo) / span));
    p.band = Math.min(n - 1, base + Math.floor(t * (n - base)));
    p.rgb = ramp[p.band];
  }
}

// ---------- 头盔：剔除原图外圈的装饰性长方框 ----------
// 原图在盔体外圈另画了一圈 1px 方框（与盔体之间还留有空隙），去模糊后它变成一根硬边暗框、
// 像个方盒子。坐标取自原图实测（两张头盔几何完全一致）：上下边线 rows 4/22、左右边线 cols 6/25。
// 先摘框再分档，避免框的暗像素污染金属阶百分位。
const HELMET_FRAME = { x0: 6, x1: 25, y0: 4, y1: 22 };
function stripHelmetFrame(px) {
  const { x0, x1, y0, y1 } = HELMET_FRAME;
  const clear = (x, y) => { const o = (y * W + x) * 4; px[o] = px[o + 1] = px[o + 2] = px[o + 3] = 0; };
  for (let x = x0; x <= x1; x++) { clear(x, y0); clear(x, y1); }
  for (let y = y0; y <= y1; y++) { clear(x0, y); clear(x1, y); }
}

// 头盔左半镜像到右半。原图左右是各画各的：内衬竖线在 col 10 / col 22（正确镜像应为 10↔21），
// 框右线外还多出一条只有右侧才有的半透明抗锯齿列 col 26 —— 去模糊后肉眼就是「右边多一条、缺一块」。
// 按成品要求以左半为准，关于 x=16（col c ↔ col 31-c，与框线 6↔25 同一根轴）整体镜像，
// 右半一律改写为左半的镜像，两半因此严格一致。镜像放在色阶分档之前，保证百分位也对称。
function mirrorHelmet(px) {
  for (let y = 0; y < H; y++) for (let x = 0; x < W / 2; x++) {
    const s = (y * W + x) * 4, d = (y * W + (W - 1 - x)) * 4;
    px[d] = px[s]; px[d + 1] = px[s + 1]; px[d + 2] = px[s + 2]; px[d + 3] = px[s + 3];
  }
}

// 摘框后盔体外沿就没了，放进一套里会显得「少画了一圈」——其余 6 件都带家族暗色轮廓。
// 这里按「正交相邻只要有一处实心就补暗色」补一圈 1px 贴合轮廓，得到原版装备那种干净的单层描边。
// 注意必须以补边前的副本判邻，否则新补的像素会连锁外扩成厚边。
function outlineSilhouette(px, rgb) {
  const prev = Buffer.from(px);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const o = (y * W + x) * 4;
    if (prev[o + 3]) continue;
    const near = (x > 0 && prev[o - 1]) || (x < W - 1 && prev[o + 7]) || (y > 0 && prev[o - W * 4 + 3]) || (y < H - 1 && prev[o + W * 4 + 3]);
    if (near) { px[o] = rgb[0]; px[o + 1] = rgb[1]; px[o + 2] = rgb[2]; px[o + 3] = 255; }
  }
}

// ---------- 装备图标：软边二值化 + 家族台阶量化 ----------
// chroma>=63 视为宝石/能源饰件（青绿面罩等），走家族发光色；其余灰阶本体走金属色阶。
function reworkEquipment(src0, family, stripFrame) {
  const src = Buffer.from(src0);
  if (stripFrame) { stripHelmetFrame(src); mirrorHelmet(src); }
  const px = Buffer.from(src);
  const groups = { metal: [], gem: [] };
  for (let i = 0; i < W * H; i++) {
    const o = i * 4;
    if (src[o + 3] < 128) { px[o] = px[o + 1] = px[o + 2] = px[o + 3] = 0; continue; }   // 软边/透明 → 彻底透明
    px[o + 3] = 255;
    const r = src[o], g = src[o + 1], b = src[o + 2];
    const chroma = Math.max(r, g, b) - Math.min(r, g, b);
    groups[chroma >= 63 ? 'gem' : 'metal'].push({ i, L: lum(r, g, b) });
  }
  bandMap(groups.metal, RAMP[family + '_metal']);
  bandMap(groups.gem, RAMP[family + '_gem'], 0.4);       // 宝石只走亮半段，避免褪成脏棕
  for (const k of ['metal', 'gem']) for (const p of groups[k]) {
    px[p.i * 4] = p.rgb[0]; px[p.i * 4 + 1] = p.rgb[1]; px[p.i * 4 + 2] = p.rgb[2];
  }
  if (stripFrame) outlineSilhouette(px, RAMP[family + '_metal'][0]);   // 摘框后补一圈贴合轮廓的家族暗色描边
  return px;
}

// ---------- 工具图标：下界合金工具换色 + 内倒角补细节 ----------
// 底图为原版下界合金工具（16x16，取自 client-extra.jar 的 assets/minecraft/textures/item），
// 它自带的「暗描边 + 中间调本体 + 亮高光」三层结构正好能被亮度百分位完整映射到赤石金属色带，
// 因此换色后保留下界工具的全部明暗层次，只是色相归到赤石家族。
const REF_DIR = path.join(ROOT, 'gui_layouts/netherite_ref');
// 32x32 下横向/纵向连续宽度不足此值的视为「细杆」（手柄、护手），跳过倒角，避免整条被压成一色。
const BEVEL_MIN_RUN = 4;

function readToolRef(name) {
  const img = readPng(path.join(REF_DIR, 'netherite_' + name.replace('akaishi_', '') + '.png'));
  if (img.width !== img.height) throw Error('下界工具底图应为正方形: ' + name);
  return img;
}

// 在 32x32 的「色带档位图」上做内倒角：朝向光照的上/左内侧提亮一档，背光的下/右内侧压暗一档。
// 升采样后每个源像素是 2x2 纯色块，倒角在块内切出 1px 转折面，立体感立刻起来，
// 同时档位仍取自有限色带，不会引入新的中间色（16px 物品栏里依旧锐利）。
function bevelDetail(g, top) {
  const src = Int16Array.from(g);
  const at = (x, y) => (x < 0 || x >= W || y < 0 || y >= H) ? -1 : src[y * W + x];
  const body = (x, y) => at(x, y) >= 1;                       // 1 以上=本体，0=描边，-1=空
  const run = (x, y, dx, dy) => {
    let c = 1;
    for (const s of [1, -1]) for (let k = 1; body(x + dx * k * s, y + dy * k * s); k++) c++;
    return c;
  };
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const i = y * W + x, v = src[i];
    if (v < 1) continue;
    const hRun = run(x, y, 1, 0), vRun = run(x, y, 0, 1);
    const lit = (at(x, y - 1) <= 0 && hRun >= BEVEL_MIN_RUN) || (at(x - 1, y) <= 0 && vRun >= BEVEL_MIN_RUN);
    const shade = (at(x, y + 1) <= 0 && hRun >= BEVEL_MIN_RUN) || (at(x + 1, y) <= 0 && vRun >= BEVEL_MIN_RUN);
    g[i] = Math.min(top, Math.max(1, v + (lit ? 1 : shade ? -1 : 0)));   // 夹在 [1, 顶档]：不并进描边、不爆白
  }
}

function reworkTool(img) {
  const { width: w, height: h, px: src } = img;
  const list = [];
  for (let i = 0; i < w * h; i++) {
    const o = i * 4;
    if (src[o + 3] < 128) continue;
    list.push({ i, L: lum(src[o], src[o + 1], src[o + 2]) });
  }
  bandMap(list, RAMP.akaishi_metal);
  const small = new Int16Array(w * h).fill(-1);
  for (const p of list) small[p.i] = p.band;

  const big = new Int16Array(W * H).fill(-1);                 // 2x 最近邻升采样，保持像素块
  const k = W / w;
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const v = small[y * w + x];
    for (let dy = 0; dy < k; dy++) for (let dx = 0; dx < k; dx++) big[(y * k + dy) * W + x * k + dx] = v;
  }
  bevelDetail(big, RAMP.akaishi_metal.length - 1);

  const ramp = RAMP.akaishi_metal, out = Buffer.alloc(W * H * 4);
  for (let i = 0; i < W * H; i++) {
    const b = big[i];
    if (b < 0) continue;
    const o = i * 4, c = ramp[b];
    out[o] = c[0]; out[o + 1] = c[1]; out[o + 2] = c[2]; out[o + 3] = 255;
  }
  return out;
}

const EQ_NAMES = ['akaishi_helmet', 'akaishi_chestplate', 'akaishi_leggings', 'akaishi_boots',
  'akaishi_life_fusion_helmet', 'akaishi_life_fusion_chestplate', 'akaishi_life_fusion_leggings', 'akaishi_life_fusion_boots'];
const TL_NAMES = ['akaishi_pickaxe', 'akaishi_axe', 'akaishi_shovel', 'akaishi_sword'];

const familyOf = n => n.includes('life_fusion') ? 'life_fusion' : 'akaishi';
function sourceOf(name) {
  const bak = path.join(BACKUP_DIR, name + '.png');
  return readPng(fs.existsSync(bak) ? bak : path.join(DIR, name + '.png')).px;
}
function render(name) {
  const src = sourceOf(name);                                // 改前 = 该物品当前贴图原件，用户能直接看出「变没变」
  if (TL_NAMES.includes(name)) return { src, px: reworkTool(readToolRef(name)) };
  return { src, px: reworkEquipment(src, familyOf(name), name.endsWith('helmet')) };
}

function bbox(px) {
  let x0 = W, y0 = H, x1 = -1, y1 = -1;
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) if (px[(y * W + x) * 4 + 3]) {
    if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y;
  }
  return `${x1 - x0 + 1}x${y1 - y0 + 1}@${x0},${y0}`;
}
function alphaStat(px) {
  let soft = 0, solid = 0, t0 = 0;
  for (let i = 0; i < W * H; i++) { const a = px[i * 4 + 3]; if (a < 8) t0++; else if (a < 200) soft++; else solid++; }
  return { soft, solid, t0 };
}
function colorCount(px) {
  const s = new Set();
  for (let i = 0; i < W * H; i++) if (px[i * 4 + 3]) s.add(`${px[i * 4]},${px[i * 4 + 1]},${px[i * 4 + 2]}`);
  return s.size;
}

// ---------- 字符画自查（--art）：逐像素对照改前 / 改后轮廓，定位「哪一侧缺了」 ----------
function art(px, tag) {
  console.log('--- ' + tag);
  for (let y = 0; y < H; y++) {
    let s = '';
    for (let x = 0; x < W; x++) {
      const o = (y * W + x) * 4, a = px[o + 3];
      if (a < 8) { s += '.'; continue; }
      const l = lum(px[o], px[o + 1], px[o + 2]);
      s += a < 200 ? '+' : l < 70 ? '#' : l < 130 ? 'o' : l < 195 ? 'x' : 'W';
    }
    console.log(String(y).padStart(2) + ' ' + s);
  }
}

// ---------- 预览 ----------
function checker(w, h, cell) {
  const px = Buffer.alloc(w * h * 4);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const on = ((Math.floor(x / cell) + Math.floor(y / cell)) & 1) === 0;
    const v = on ? 92 : 62, o = (y * w + x) * 4;
    px[o] = v; px[o + 1] = v; px[o + 2] = v + 4; px[o + 3] = 255;
  }
  return px;
}
// alpha 混合贴图：改前贴图带软边，必须真实混合才能看出来「糊」
function blit(dst, DW, src, SW, SH, ox, oy, f) {
  for (let y = 0; y < SH * f; y++) for (let x = 0; x < SW * f; x++) {
    const so = (Math.floor(y / f) * SW + Math.floor(x / f)) * 4;
    const a = src[so + 3] / 255;
    if (a <= 0.03) continue;
    const dof = ((oy + y) * DW + ox + x) * 4;
    dst[dof] = Math.round(src[so] * a + dst[dof] * (1 - a));
    dst[dof + 1] = Math.round(src[so + 1] * a + dst[dof + 1] * (1 - a));
    dst[dof + 2] = Math.round(src[so + 2] * a + dst[dof + 2] * (1 - a));
    dst[dof + 3] = 255;
  }
}
function sheetOf(names, f, out) {
  const pad = 6, cw = W * f + pad * 2, ch = H * f + pad * 2;
  const PW = cw * names.length, PH = ch * 2;
  const sheet = checker(PW, PH, 8);
  names.forEach((n, i) => {
    const r = render(n);
    blit(sheet, PW, r.src, W, H, i * cw + pad, pad, f);                 // 上排：改前
    blit(sheet, PW, r.px, W, H, i * cw + pad, ch + pad, f);             // 下排：改后
  });
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, writePng(PW, PH, sheet));
  return out;
}

function main() {
  const artArg = process.argv.find(x => x.startsWith('--art'));
  if (artArg) {
    const names = artArg.includes('=') ? artArg.split('=')[1].split(',') : [...EQ_NAMES, ...TL_NAMES];
    for (const n of names) {
      const { src, px } = render(n);
      console.log('=== ' + n);
      art(src, '改前'); art(px, '改后');
    }
    return 0;
  }

  if (process.argv.includes('--dump')) {
    for (const n of [...EQ_NAMES, ...TL_NAMES]) {
      const { src, px } = render(n);
      const b = alphaStat(src), a = alphaStat(px);
      console.log(n.padEnd(34) + ' 软边 ' + String(b.soft).padStart(3) + ' -> ' + String(a.soft).padStart(2)
        + '  实心 ' + String(b.solid).padStart(3) + ' -> ' + String(a.solid).padStart(3)
        + '  色数 ' + String(colorCount(src)).padStart(3) + ' -> ' + String(colorCount(px)).padStart(3)
        + '  ' + bbox(src) + ' -> ' + bbox(px));
    }
    return 0;
  }

  // --export=name：把某张的渲染结果单独落盘到 gui_layouts/_rework_<name>.png，供外部逐像素比对
  const exp = process.argv.find(x => x.startsWith('--export='));
  if (exp) {
    for (const n of exp.split('=')[1].split(',')) {
      const p = path.join(ROOT, 'gui_layouts/_rework_' + n + '.png');
      fs.writeFileSync(p, writePng(W, H, render(n).px));
      console.log('导出: ' + path.relative(ROOT, p));
    }
    return 0;
  }

  const one = (process.argv.find(x => x.startsWith('--one=')) || '').split('=')[1];
  if (one) {
    const list = one.split(',');
    const f = list.length <= 2 ? 16 : list.length <= 4 ? 10 : 6;
    const p = sheetOf(list, f, path.join(ROOT, 'gui_layouts/_rework_zoom_' + list[0] + '.png'));
    console.log('放大自查（上=改前 / 下=改后）: ' + path.relative(ROOT, p));
    return 0;
  }

  const previewOnly = process.argv.includes('--preview');
  fs.mkdirSync(BACKUP_DIR, { recursive: true });

  if (!previewOnly) {
    for (const n of [...EQ_NAMES, ...TL_NAMES]) {
      const live = path.join(DIR, n + '.png'), bak = path.join(BACKUP_DIR, n + '.png');
      if (!fs.existsSync(bak)) fs.copyFileSync(live, bak);             // 仅首次备份原件，保证可回溯
      fs.writeFileSync(live, writePng(W, H, render(n).px));
    }
  }

  for (const n of [...EQ_NAMES, ...TL_NAMES]) {
    const { src, px } = render(n);
    const b = alphaStat(src), a = alphaStat(px);
    console.log(n.padEnd(34) + ' 软边 ' + String(b.soft).padStart(3) + ' -> ' + String(a.soft).padStart(2)
      + '  实心 ' + String(b.solid).padStart(3) + ' -> ' + String(a.solid).padStart(3)
      + '  色数 ' + String(colorCount(src)).padStart(3) + ' -> ' + String(colorCount(px)).padStart(3));
  }
  console.log('预览: ' + path.relative(ROOT, sheetOf(EQ_NAMES, 6, PREVIEW_EQ)));
  console.log('预览: ' + path.relative(ROOT, sheetOf(TL_NAMES, 10, PREVIEW_TL)));
  console.log((previewOnly ? '预览模式，未写盘' : '已写盘 ' + (EQ_NAMES.length + TL_NAMES.length) + ' 张，原件已备份到 ' + path.relative(ROOT, BACKUP_DIR)));
  return 0;
}

process.exit(main());
