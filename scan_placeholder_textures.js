// 扫描资源目录下所有 PNG，统计唯一颜色数，定位疑似「平色占位」贴图。
// 用法: node .\scan_placeholder_textures.js [阈值，默认 4]
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const root = path.join(__dirname, 'common/src/main/resources/assets/akaishi');
const LIMIT = Number(process.argv[2] || 4);

function readPng(file) {
  const b = fs.readFileSync(file);
  if (b.readUInt32BE(0) !== 0x89504e47) throw Error('PNG header');
  const w = b.readUInt32BE(16), h = b.readUInt32BE(20), depth = b[24], ctype = b[25];
  if (depth !== 8 || ctype !== 6) throw Error('expect RGBA8');
  const idat = [];
  let i = 8;
  while (i + 8 <= b.length) {
    const len = b.readUInt32BE(i), tag = b.toString('ascii', i + 4, i + 8);
    if (tag === 'IDAT') idat.push(b.subarray(i + 8, i + 8 + len));
    i += 12 + len;
  }
  const ch = 4, stride = w * ch;
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const px = Buffer.alloc(w * h * ch);
  let prev = Buffer.alloc(stride), off = 0;
  for (let y = 0; y < h; y++) {
    const ft = raw[off++];
    const line = Buffer.from(raw.subarray(off, off + stride)); off += stride;
    if (ft === 1) { for (let x = ch; x < stride; x++) line[x] = (line[x] + line[x - ch]) & 255; }
    else if (ft === 2) { for (let x = 0; x < stride; x++) line[x] = (line[x] + prev[x]) & 255; }
    else if (ft === 3) { for (let x = 0; x < stride; x++) line[x] = (line[x] + (((x >= ch ? line[x - ch] : 0) + prev[x]) >> 1)) & 255; }
    else if (ft === 4) {
      for (let x = 0; x < stride; x++) {
        const a = x >= ch ? line[x - ch] : 0, bb = prev[x], c = x >= ch ? prev[x - ch] : 0;
        const p = a + bb - c, pa = Math.abs(p - a), pb = Math.abs(p - bb), pc = Math.abs(p - c);
        line[x] = (line[x] + ((pa <= pb && pa <= pc) ? a : (pb <= pc ? bb : c))) & 255;
      }
    }
    line.copy(px, y * stride); prev = line;
  }
  return { w, h, px };
}

function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out);
    else if (e.name.toLowerCase().endsWith('.png')) out.push(p);
  }
  return out;
}

const files = walk(root);
const low = [];
let total = 0;
for (const f of files) {
  total++;
  try {
    const img = readPng(f);
    const colors = new Set();
    for (let i = 0; i < img.w * img.h; i++) {
      const a = img.px[i * 4 + 3];
      colors.add(a === 0 ? 'T' : `${img.px[i * 4]},${img.px[i * 4 + 1]},${img.px[i * 4 + 2]},${a}`);
    }
    if (colors.size <= LIMIT) low.push({ rel: path.relative(root, f), w: img.w, h: img.h, colors: colors.size });
  } catch (x) { low.push({ rel: path.relative(root, f), error: x.message }); }
}

low.sort((a, b) => (a.colors ?? 0) - (b.colors ?? 0));
for (const l of low) console.log(l.error ? `ERR  ${l.rel}  ${l.error}` : `${String(l.colors).padStart(3)}  ${l.w}x${l.h}  ${l.rel}`);
console.log(`---- scanned ${total}, suspicious(<=${LIMIT} colors): ${low.length}`);
process.exit(low.length ? 1 : 0);
