const fs = require('fs'), path = require('path'), zlib = require('zlib');
const root = path.join(__dirname, 'common/src/main/resources/assets/akaishi');
const names = ['advanced_mixture', 'dragon', 'end_mixture', 'nether_compound', 'pure', 'sculk', 'ultimate_mixture']
  .flatMap(x => [`akaishi_activated_${x}_component`, `akaishi_activated_${x}_crystal`]);

// 仅接受 RGBA8，filter 0~4
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

let e = 0;
for (const n of names) {
  try {
    const m = path.join(root, 'models/item', n + '.json');
    const t = path.join(root, 'textures/item', n + '.png');
    const j = JSON.parse(fs.readFileSync(m));
    if (j.parent !== 'minecraft:item/generated' || j.textures?.layer0 !== `akaishi:item/${n}`) throw Error('model consumer mismatch');
    if (!fs.existsSync(t)) throw Error('missing png');
    const img = readPng(t);
    if (img.w !== 32 || img.h !== 32) throw Error('not 32x32');
    const colors = new Set();
    let transparent = 0;
    for (let i = 0; i < img.w * img.h; i++) {
      const a = img.px[i * 4 + 3];
      if (a === 0) { transparent++; continue; }
      colors.add(`${img.px[i * 4]},${img.px[i * 4 + 1]},${img.px[i * 4 + 2]},${a}`);
    }
    if (transparent === 0) throw Error('无透明背景（疑似占位平色）');
    if (colors.size < 6) throw Error('调色板过少（' + colors.size + ' 色，疑似占位平色）');
  } catch (x) { console.error(n + ': ' + x.message); e++; }
}
if (e) process.exit(1);
console.log('OK: 14 JSON models consume matching 32x32 RGBA8 icons (透明底 + 调色板 >= 6)');
