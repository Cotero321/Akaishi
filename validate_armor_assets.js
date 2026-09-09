'use strict';
// 校验盔甲 layer 贴图：128x64 + RGBA8 + layer_1/layer_2 一致 + UV 采样区无空洞 + 家族识别色存在 + 无孤儿
const fs = require('node:fs'), path = require('node:path'), zlib = require('node:zlib');
const root = __dirname;
const DIR = path.join(root, 'common/src/main/resources/assets/akaishi/textures/models/armor');
const PNG = Buffer.from('89504e470d0a1a0a', 'hex');

// 与 AkaishiMekaSuitArmorModel.createLayer 的 addBox(texOffs, 尺寸) 一致
const RECTS = [
  [0, 0, 9, 9, 9], [36, 0, 6, 2, 1], [50, 0, 1, 4, 5], [50, 9, 1, 4, 5], [0, 18, 5, 1, 4],
  [18, 18, 9.4, 11.5, 1.8], [0, 31, 6.4, 7.2, 1.4], [0, 42, 3.2, 3.6, 1], [18, 31, 1.6, 4.5, 4.6],
  [30, 31, 1.6, 4.5, 4.6], [40, 18, 7.2, 8.8, 1.2], [40, 31, 9.6, 2.5, 5.6], [0, 46, 3.4, 2.1, 1],
  [32, 42, 2.8, 2.8, 0.6], [40, 42, 1.2, 2, 0.5],
  [60, 0, 6, 3.1, 5.4], [60, 9, 5.4, 7.5, 5.4], [82, 0, 1, 4, 4.2], [82, 9, 5.8, 2, 5.6], [94, 0, 2, 1, 4.4],
  [60, 22, 5.3, 8.4, 5.3], [82, 18, 4.5, 4.8, 1.1], [94, 10, 1, 4.8, 3.6],
  [60, 37, 5.6, 5.8, 5.8], [82, 28, 4.8, 3.2, 1.2], [94, 20, 6, 1.2, 6.4],
];

// 家族 -> 期望强调色（取自重制调色板）
const FAMILIES = {
  akaishi: [212, 61, 50],
  life_fusion: [66, 208, 176],
};

const errors = [], warns = [];

function readPng(file) {
  const b = fs.readFileSync(file);
  if (!b.subarray(0, 8).equals(PNG)) throw Error('PNG header');
  const w = b.readUInt32BE(16), h = b.readUInt32BE(20), depth = b[24], ctype = b[25];
  if (depth !== 8 || ctype !== 6) throw Error('expect RGBA8');
  let pos = 8; const idat = [];
  while (pos + 8 <= b.length) {
    const len = b.readUInt32BE(pos), tag = b.toString('ascii', pos + 4, pos + 8);
    if (tag === 'IDAT') idat.push(b.subarray(pos + 8, pos + 8 + len));
    pos += 12 + len;
  }
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const stride = w * 4, px = Buffer.alloc(w * h * 4);
  let i = 0, o = 0, prev = Buffer.alloc(stride);
  for (let y = 0; y < h; y++) {
    const ft = raw[i++];
    const line = Buffer.from(raw.subarray(i, i + stride)); i += stride;
    if (ft === 1) { for (let x = 4; x < stride; x++) line[x] = (line[x] + line[x - 4]) & 255; }
    else if (ft === 2) { for (let x = 0; x < stride; x++) line[x] = (line[x] + prev[x]) & 255; }
    else if (ft === 3) { for (let x = 0; x < stride; x++) line[x] = (line[x] + (((x >= 4 ? line[x - 4] : 0) + prev[x]) >> 1)) & 255; }
    else if (ft === 4) {
      for (let x = 0; x < stride; x++) {
        const a = x >= 4 ? line[x - 4] : 0, b2 = prev[x], c = x >= 4 ? prev[x - 4] : 0;
        const p = a + b2 - c, pa = Math.abs(p - a), pb = Math.abs(p - b2), pc = Math.abs(p - c);
        line[x] = (line[x] + ((pa <= pb && pa <= pc) ? a : (pb <= pc ? b2 : c))) & 255;
      }
    } else if (ft !== 0) throw Error('filter ' + ft);
    line.copy(px, o); o += stride;
    prev = line;
  }
  return { w, h, px };
}

function near(a, b, tol) { return Math.abs(a[0] - b[0]) <= tol && Math.abs(a[1] - b[1]) <= tol && Math.abs(a[2] - b[2]) <= tol; }

for (const [family, accent] of Object.entries(FAMILIES)) {
  let img1, img2;
  try { img1 = readPng(path.join(DIR, family + '_layer_1.png')); }
  catch (x) { errors.push(family + '_layer_1: ' + x.message); continue; }
  try { img2 = readPng(path.join(DIR, family + '_layer_2.png')); }
  catch (x) { errors.push(family + '_layer_2: ' + x.message); continue; }

  for (const [n, img] of [['layer_1', img1], ['layer_2', img2]]) {
    if (img.w !== 128 || img.h !== 64) errors.push(family + '_' + n + ': 尺寸 ' + img.w + 'x' + img.h + '，应为 128x64');
  }
  if (img1.w !== img2.w || img1.h !== img2.h) { errors.push(family + ': 两 layer 尺寸不一致'); continue; }

  // layer_1 与 layer_2 应完全一致（模型对四槽共用同一 UV）
  if (!img1.px.equals(img2.px)) errors.push(family + ': layer_1 与 layer_2 像素不一致');

  // UV 采样区无空洞
  let holes = 0, accentHit = 0;
  for (const [x, y, dx, dy, dz] of RECTS) {
    const faces = [
      [x + dz, y, dx, dz], [x + dz + dx, y, dx, dz],
      [x, y + dz, dz, dy], [x + dz, y + dz, dx, dy],
      [x + dz + dx, y + dz, dz, dy], [x + dz + dx + dz, y + dz, dx, dy],
    ];
    for (const [fx, fy, fw, fh] of faces) {
      for (let py = Math.round(fy); py < Math.round(fy + fh); py++) {
        for (let px = Math.round(fx); px < Math.round(fx + fw); px++) {
          if (px < 0 || py < 0 || px >= img1.w || py >= img1.h) { errors.push(family + ': UV 越界 ' + px + ',' + py); continue; }
          const o = (py * img1.w + px) * 4;
          if (img1.px[o + 3] < 255) holes++;
          else if (near([img1.px[o], img1.px[o + 1], img1.px[o + 2]], accent, 30)) accentHit++;
        }
      }
    }
  }
  if (holes > 0) errors.push(family + ': UV 采样区存在 ' + holes + ' 个非不透明像素（空洞）');
  if (accentHit < 40) errors.push(family + ': 家族识别色像素过少（' + accentHit + '），可能丢色');
  if (!errors.some(e => e.startsWith(family))) console.log('OK ' + family + '  accent命中 ' + accentHit + ' 像素');
}

// 孤儿检查
const expected = new Set(Object.keys(FAMILIES).flatMap(f => [f + '_layer_1', f + '_layer_2']));
for (const f of fs.readdirSync(DIR)) {
  if (f.endsWith('.png') && !expected.has(f.slice(0, -4))) errors.push('孤儿 ' + f);
  if (f.endsWith('.mcmeta')) warns.push('存在动画元数据 ' + f);
}

console.log(JSON.stringify({ families: Object.keys(FAMILIES).length, textures: expected.size, warns: warns.length, errors: errors.length }, null, 2));
warns.forEach(w => console.log('WARN ' + w));
errors.forEach(e => console.log('ERR ' + e));
if (errors.length) process.exitCode = 1;
