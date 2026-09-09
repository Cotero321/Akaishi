// 盔甲 layer 贴图重制：赤石动力装甲 / 生命融合装甲（工业机械 + 生物科技风）。
// 目标贴图 128x64（与 AkaishiMekaSuitArmorModel 的 LayerDefinition.create(mesh,128,64) 一致）。
// 原理：
//   1) 从原图提取家族识别色（饱和度最高的主色）与基色，保证两套护甲颜色可区分且与家族一致；
//   2) 按模型 texOffs 列出的 UV 矩形逐块上「装甲板 / 边框 / 核心」样式，细节贴合实际采样区；
//   3) 板面 = 金属底 + 面板缝 + 铆钉 + 家族强调条，工业机械感；核心/边框带发光强调，生物科技感。
// 幂等：首次运行备份原图到 gui_layouts/armor_backup/，之后始终以备份为源。
// 用法：node rework_armor_assets.js [--dump]
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/models/armor');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/armor_backup');
const PREVIEW = path.join(ROOT, 'gui_layouts/armor_rework_preview.png');
const DUMP = process.argv.includes('--dump');

const CRC_TABLE = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1); t[n] = c; }
  return t;
})();
function crc32(buf) { let c = 0xFFFFFFFF; for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 255] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; }

function readPng(buf) {
  if (buf.readUInt32BE(0) !== 0x89504e47) throw Error('PNG header');
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
  if (depth !== 8 || (ctype !== 6 && ctype !== 2)) throw Error('expect RGBA8/RGB8');
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

// ---- 从原图提取调色信息 ----
function analyze(px) {
  const hist = new Map();
  let n = 0, sr = 0, sg = 0, sb = 0;
  for (let i = 0; i < px.length; i += 4) {
    if (px[i + 3] < 128) continue;
    const r = px[i], g = px[i + 1], b = px[i + 2];
    sr += r; sg += g; sb += b; n++;
    hist.set((r << 16) | (g << 8) | b, (hist.get((r << 16) | (g << 8) | b) || 0) + 1);
  }
  let accent = 0xC4322B, bestSat = -1;
  for (const [k, c] of hist) {
    const r = (k >> 16) & 255, g = (k >> 8) & 255, b = k & 255;
    const max = Math.max(r, g, b), min = Math.min(r, g, b);
    const sat = (max - min) * Math.sqrt(c);        // 饱和度高且出现多 → 家族强调色
    if (sat > bestSat) { bestSat = sat; accent = k; }
  }
  const top = [...hist.entries()].sort((a, b) => b[1] - a[1]).slice(0, 6)
    .map(([k, c]) => '#' + k.toString(16).padStart(6, '0') + 'x' + c);
  return { accent: [(accent >> 16) & 255, (accent >> 8) & 255, accent & 255], avg: n ? [Math.round(sr / n), Math.round(sg / n), Math.round(sb / n)] : [128, 128, 128], top };
}

function dump() {
  for (const f of fs.readdirSync(DIR).filter(x => x.endsWith('.png')).sort()) {
    const img = readPng(fs.readFileSync(path.join(DIR, f)));
    const a = analyze(img.px);
    console.log('== ' + f + '  ' + img.width + 'x' + img.height);
    console.log('   accent rgb(' + a.accent.join(',') + ')  avg rgb(' + a.avg.join(',') + ')');
    console.log('   top: ' + a.top.join(' '));
    const step = 4;
    for (let y = 0; y < img.height; y += step) {
      let line = '   ';
      for (let x = 0; x < img.width; x += step) {
        const o = (y * img.width + x) * 4;
        if (img.px[o + 3] < 128) { line += '.'; continue; }
        const r = img.px[o], g = img.px[o + 1], b = img.px[o + 2];
        const max = Math.max(r, g, b), min = Math.min(r, g, b);
        if (max - min > 40) line += 'A';
        else if (max > 170) line += '*';
        else if (max > 90) line += '+';
        else line += '#';
      }
      console.log(line);
    }
  }
  return 0;
}

if (DUMP) process.exit(dump());

// ---- 家族调色板（识别色取自原图，金属底为工业机械灰）----
const PALETTES = {
  akaishi: { dark: [26, 30, 36], base: [58, 70, 78], mid: [90, 107, 116], light: [183, 199, 201], accent: [212, 61, 50], glow: [255, 176, 94] },
  life_fusion: { dark: [23, 40, 38], base: [47, 79, 72], mid: [74, 153, 135], light: [115, 194, 173], accent: [66, 208, 176], glow: [140, 235, 205] },
};

// 模型 texOffs + 盒尺寸（与 AkaishiMekaSuitArmorModel.createLayer 的 addBox 一一对应）
const RECTS = [
  { x: 0, y: 0, dx: 9, dy: 9, dz: 9, style: 'shell' },        // 头盔外壳
  { x: 36, y: 0, dx: 6, dy: 2, dz: 1, style: 'frame' },       // 面罩
  { x: 50, y: 0, dx: 1, dy: 4, dz: 5, style: 'frame' },       // 右耳罩
  { x: 50, y: 9, dx: 1, dy: 4, dz: 5, style: 'frame' },       // 左耳罩
  { x: 0, y: 18, dx: 5, dy: 1, dz: 4, style: 'frame' },       // 头顶
  { x: 18, y: 18, dx: 9.4, dy: 11.5, dz: 1.8, style: 'shell' }, // 躯干前壳
  { x: 0, y: 31, dx: 6.4, dy: 7.2, dz: 1.4, style: 'frame' }, // 胸甲
  { x: 0, y: 42, dx: 3.2, dy: 3.6, dz: 1.0, style: 'frame' }, // 胸甲芯
  { x: 18, y: 31, dx: 1.6, dy: 4.5, dz: 4.6, style: 'frame' },// 右侧挂
  { x: 30, y: 31, dx: 1.6, dy: 4.5, dz: 4.6, style: 'frame' },// 左侧挂
  { x: 40, y: 18, dx: 7.2, dy: 8.8, dz: 1.2, style: 'shell' },// 背壳
  { x: 40, y: 31, dx: 9.6, dy: 2.5, dz: 5.6, style: 'frame' },// 腰带
  { x: 0, y: 46, dx: 3.4, dy: 2.1, dz: 1.0, style: 'frame' }, // 腰扣
  { x: 32, y: 42, dx: 2.8, dy: 2.8, dz: 0.6, style: 'core' }, // 生物核心
  { x: 40, y: 42, dx: 1.2, dy: 2.0, dz: 0.5, style: 'core' }, // 核心指示
  { x: 60, y: 0, dx: 6, dy: 3.1, dz: 5.4, style: 'frame' },   // 肩甲
  { x: 60, y: 9, dx: 5.4, dy: 7.5, dz: 5.4, style: 'shell' }, // 上臂壳
  { x: 82, y: 0, dx: 1, dy: 4.0, dz: 4.2, style: 'frame' },   // 臂侧板
  { x: 82, y: 9, dx: 5.8, dy: 2.0, dz: 5.6, style: 'frame' }, // 腕环
  { x: 94, y: 0, dx: 2, dy: 1.0, dz: 4.4, style: 'frame' },   // 肩垫
  { x: 60, y: 22, dx: 5.3, dy: 8.4, dz: 5.3, style: 'shell' },// 大腿壳
  { x: 82, y: 18, dx: 4.5, dy: 4.8, dz: 1.1, style: 'frame' },// 腿前板
  { x: 94, y: 10, dx: 1, dy: 4.8, dz: 3.6, style: 'frame' },  // 腿侧板
  { x: 60, y: 37, dx: 5.6, dy: 5.8, dz: 5.8, style: 'frame' },// 靴壳
  { x: 82, y: 28, dx: 4.8, dy: 3.2, dz: 1.2, style: 'frame' },// 靴前板
  { x: 94, y: 20, dx: 6, dy: 1.2, dz: 6.4, style: 'frame' },  // 靴底环
];

// 各面亮度系数：顶亮、底暗，形成立体感
const FACE_FACTOR = { top: 1.22, bottom: 0.70, front: 1.0, back: 0.84, left: 0.93, right: 0.93 };

// 标准 MC 盒 UV 展开：顶/底一行，四侧一行
function faces(r) {
  const { x, y, dx, dy, dz } = r;
  return [
    { x: x + dz, y, w: dx, h: dz, kind: 'top' },
    { x: x + dz + dx, y, w: dx, h: dz, kind: 'bottom' },
    { x, y: y + dz, w: dz, h: dy, kind: 'right' },
    { x: x + dz, y: y + dz, w: dx, h: dy, kind: 'front' },
    { x: x + dz + dx, y: y + dz, w: dz, h: dy, kind: 'left' },
    { x: x + dz + dx + dz, y: y + dz, w: dx, h: dy, kind: 'back' },
  ];
}

const clamp = v => v < 0 ? 0 : v > 255 ? 255 : v;
const mix = (a, b, t) => [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t];
function hash(x, y, s) {
  let n = (x * 374761393 + y * 668265263 + s * 1442695041) | 0;
  n = (n ^ (n >> 13)) * 1274126177 | 0;
  return ((n ^ (n >> 16)) >>> 0) / 4294967295;
}

function put(px, W, H, mask, x, y, c) {
  if (x < 0 || y < 0 || x >= W || y >= H) return;
  const i = y * W + x;
  if (!mask[i]) return;
  const o = i * 4;
  px[o] = clamp(Math.round(c[0])); px[o + 1] = clamp(Math.round(c[1])); px[o + 2] = clamp(Math.round(c[2])); px[o + 3] = 255;
}

// 单面绘制：金属底 + 面板缝 + 高光边 + 样式叠加
function drawFace(px, W, H, mask, pal, style, f) {
  const x0 = Math.round(f.x), y0 = Math.round(f.y);
  const x1 = Math.round(f.x + f.w), y1 = Math.round(f.y + f.h);
  const fac = FACE_FACTOR[f.kind];
  for (let y = y0; y < y1; y++) {
    for (let x = x0; x < x1; x++) {
      const n = hash(x, y, 7);
      let c = mix(pal.base, pal.mid, 0.3 + 0.4 * n);
      c = [c[0] * fac, c[1] * fac, c[2] * fac];
      if (x === x0 || y === y0) c = mix(c, pal.dark, 0.72);          // 上/左缝
      if (x === x1 - 1 || y === y1 - 1) c = mix(c, pal.light, 0.30); // 下/右高光
      put(px, W, H, mask, x, y, c);
    }
  }
  const w = x1 - x0, h = y1 - y0;

  if (style === 'frame') {
    // 边框：压暗金属底 + 居中 1px 强调线（避免大面积色块）
    for (let y = y0; y < y1; y++) {
      for (let x = x0; x < x1; x++) {
        const n = hash(x, y, 11);
        let c = mix(pal.base, pal.dark, 0.35 + 0.3 * n);
        c = [c[0] * fac, c[1] * fac, c[2] * fac];
        if (x === x0 || y === y0) c = mix(c, pal.dark, 0.70);
        if (x === x1 - 1 || y === y1 - 1) c = mix(c, pal.light, 0.22);
        put(px, W, H, mask, x, y, c);
      }
    }
    if (w >= h) {
      const ym = y0 + Math.floor(h / 2);
      for (let x = x0 + 1; x < x1 - 1; x++) put(px, W, H, mask, x, ym, x % 4 === 0 ? pal.glow : pal.accent);
    } else {
      const xm = x0 + Math.floor(w / 2);
      for (let y = y0 + 1; y < y1 - 1; y++) put(px, W, H, mask, xm, y, y % 4 === 0 ? pal.glow : pal.accent);
    }
  } else if (style === 'core') {
    // 核心：中心发光，边缘压暗
    const cx = (x0 + x1 - 1) / 2, cy = (y0 + y1 - 1) / 2;
    const rad = Math.max(1, Math.min(w, h) / 2);
    for (let y = y0; y < y1; y++) for (let x = x0; x < x1; x++) {
      const d = Math.hypot(x - cx, y - cy) / rad;
      const c = d < 0.55 ? pal.glow : mix(pal.accent, pal.dark, Math.min(1, (d - 0.55) * 1.6));
      put(px, W, H, mask, x, y, c);
    }
  } else {
    // 壳板：中部面板线 + 四角铆钉
    if (w >= 6 && h >= 6) {
      const ym = y0 + Math.floor(h / 2);
      for (let x = x0 + 1; x < x1 - 1; x++) {
        put(px, W, H, mask, x, ym, mix(pal.dark, pal.base, 0.25));
        put(px, W, H, mask, x, ym + 1, mix(pal.light, pal.base, 0.35));
      }
    }
    if (w >= 5 && h >= 5) {
      for (const [rx, ry] of [[x0 + 1, y0 + 1], [x1 - 2, y0 + 1], [x0 + 1, y1 - 2], [x1 - 2, y1 - 2]]) {
        put(px, W, H, mask, rx, ry, pal.light);
        put(px, W, H, mask, rx, ry + 1, pal.dark);
      }
    }
  }
}

function buildArmor(pal, mask, W, H) {
  const px = Buffer.alloc(W * H * 4);
  for (const r of RECTS) for (const f of faces(r)) drawFace(px, W, H, mask, pal, r.style, f);
  return px;
}

// 预览：棋盘底 + 3x 最近邻放大
function checker(W, H, cell) {
  const px = Buffer.alloc(W * H * 4);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const on = ((Math.floor(x / cell) + Math.floor(y / cell)) & 1) === 0;
    const v = on ? 70 : 52, o = (y * W + x) * 4;
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
  const W = 128, H = 64, f = 3, pad = 8;
  const out = [];

  for (const family of ['akaishi', 'life_fusion']) {
    const pal = PALETTES[family];
    for (const l of ['_layer_1', '_layer_2']) {
      const bakPath = path.join(BACKUP_DIR, family + l + '.png');
      if (!fs.existsSync(bakPath)) fs.copyFileSync(path.join(DIR, family + l + '.png'), bakPath);
    }
    // 掩码直接由模型 UV 矩形生成，保证采样区无空洞
    const mask = new Uint8Array(W * H);
    let opaque = 0;
    for (const r of RECTS) {
      for (const f of faces(r)) {
        const x0 = Math.round(f.x), y0 = Math.round(f.y);
        const x1 = Math.round(f.x + f.w), y1 = Math.round(f.y + f.h);
        for (let y = y0; y < y1; y++) for (let x = x0; x < x1; x++) {
          if (x < 0 || y < 0 || x >= W || y >= H) continue;
          const i = y * W + x;
          if (!mask[i]) { mask[i] = 1; opaque++; }
        }
      }
    }

    const px = buildArmor(pal, mask, W, H);
    for (const l of ['_layer_1', '_layer_2']) {
      fs.writeFileSync(path.join(DIR, family + l + '.png'), writePng(W, H, px));
    }
    out.push({ family, px });
    console.log(family.padEnd(14) + ' accent rgb(' + pal.accent.join(',') + ')  覆盖像素 ' + opaque);
  }

  // 预览：每家族一张（layer_1/layer_2 内容一致），2 列 x 1 行
  const cellW = W * f + pad * 2, cellH = H * f + pad * 2;
  const PW = cellW * 2, PH = cellH;
  const sheet = checker(PW, PH, 8);
  out.forEach((o, i) => {
    const ox = (i % 2) * cellW + pad, oy = Math.floor(i / 2) * cellH + pad;
    blit(sheet, PW, o.px, W, H, ox, oy, f);
  });
  fs.writeFileSync(PREVIEW, writePng(PW, PH, sheet));
  console.log('共重制 4 张（2 套护甲 x layer_1/layer_2）');
  console.log('预览: ' + path.relative(ROOT, PREVIEW));
  return 0;
}

process.exit(main());
