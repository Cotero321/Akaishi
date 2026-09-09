const fs = require('fs'), path = require('path');
const root = path.join(__dirname, 'common/src/main/resources/assets/akaishi');

// 自持三面贴图的机器
const SELF_TEX = ['activated_fractionator', 'energy_liquefier', 'energy_processor'];
// 单贴图机器
const SINGLE_TEX = ['super_generator_core'];
// 模型复用其他批次贴图的机器（仅校验引用可达）
const REUSED = ['energy_aggregator', 'equipment_forger'];

function png(p) {
  const b = fs.readFileSync(p);
  if (b.subarray(0, 8).toString('hex') !== '89504e470d0a1a0a' || b.toString('ascii', 12, 16) !== 'IHDR') throw Error('PNG ' + p);
  return [b.readUInt32BE(16), b.readUInt32BE(20)];
}
function json(p) { return JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, '')); }

let pngCount = 0;
for (const n of ['fuel_canner', 'fuel_mixer', 'pulverizer', 'transformer']) {
  json(path.join(root, 'models/block/akaishi_' + n + '.json'));
  json(path.join(root, 'blockstates/akaishi_' + n + '.json'));
}
for (const n of SELF_TEX) {
  for (const f of ['top', 'side', 'bottom']) {
    const s = png(path.join(root, 'textures/block', `akaishi_${n}_${f}.png`));
    if (s[0] !== 64 || s[1] !== 64) throw Error('size ' + n + '_' + f);
    pngCount++;
  }
}
for (const n of SINGLE_TEX) {
  const s = png(path.join(root, 'textures/block', `akaishi_${n}.png`));
  if (s[0] !== 64 || s[1] !== 64) throw Error('size ' + n);
  pngCount++;
}
// 复用型机器：模型引用的贴图必须真实存在（防死链）
for (const n of REUSED) {
  const model = json(path.join(root, 'models/block/akaishi_' + n + '.json'));
  for (const ref of Object.values(model.textures || {})) {
    const rel = String(ref).replace(/^akaishi:/, '');
    const p = path.join(root, 'textures', rel + '.png');
    if (!fs.existsSync(p)) throw Error('死链 ' + n + ' -> ' + ref);
    pngCount++;
  }
}
console.log(JSON.stringify({ json: 8, png: pngCount, errors: 0 }, null, 2));
