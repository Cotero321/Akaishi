const fs = require('fs'), path = require('path');
const root = path.join(__dirname, 'common/src/main/resources/assets/akaishi');
const blockstates = path.join(root, 'blockstates');
const models = path.join(root, 'models');
const textures = path.join(root, 'textures');
const errors = [];
const miner = name => name.includes('miner');
const jsonFiles = [];
for (const dir of [blockstates, path.join(models, 'block') , path.join(models, 'item')]) {
  for (const name of fs.readdirSync(dir)) if (miner(name) && name.endsWith('.json')) jsonFiles.push(path.join(dir, name));
}
const pngFiles = [];
for (const sub of ['block', 'item']) {
  const dir = path.join(textures, sub);
  for (const name of fs.readdirSync(dir)) {
    if (miner(name) && name.endsWith('.png') && !name.endsWith('.tmp.png')) pngFiles.push(path.join(dir, name));
  }
}
const json = file => { try { return JSON.parse(fs.readFileSync(file, 'utf8')); } catch { errors.push(`${path.relative(__dirname, file)}: invalid JSON`); return null; } };
for (const file of jsonFiles) {
  const raw = fs.readFileSync(file, 'utf8');
  if (raw.includes('.tmp.png') || raw.includes('_tmp')) errors.push(`${path.relative(__dirname, file)}: temporary texture reference`);
  const value = json(file); if (!value) continue;
  const refs = [];
  const visit = node => { if (!node || typeof node !== 'object') return; for (const [key, val] of Object.entries(node)) { if ((key === 'model' || key === 'parent' || key === 'texture' || key === 'textures') && typeof val === 'string') refs.push(val); else visit(val); } };
  visit(value);
  for (let ref of refs) {
    if (ref.startsWith('minecraft:')) continue;
    if (ref.startsWith('akaishi:')) ref = ref.slice(8);
    let target;
    if (ref.includes('/')) {
      const modelRef = ref.endsWith('.json') ? ref : `${ref}.json`;
      target = path.join(root, 'models', modelRef);
    } else {
      target = path.join(textures, `${ref}.png`);
    }
    if (!fs.existsSync(target)) errors.push(`${path.relative(__dirname, file)} -> missing ${ref}`);
  }
}
for (const file of pngFiles) {
  const b = fs.readFileSync(file);
  const width = b.readUInt32BE(16), height = b.readUInt32BE(20);
  if (b.readUInt32BE(0) !== 0x89504e47 || width !== 64 || height !== 64) errors.push(`${path.relative(__dirname, file)}: PNG must be real 64x64`);
}
console.log(`miner scoped files: ${jsonFiles.length} JSON, ${pngFiles.length} PNG`);
console.log(errors.length ? errors.join('\n') : 'miner resource links and PNG dimensions: OK');
process.exitCode = errors.length ? 1 : 0;

