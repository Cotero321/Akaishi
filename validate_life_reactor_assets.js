// 生命/反应堆机器贴图校验：PNG 头 + 64x64 尺寸 + 模型引用可达
const fs = require('fs');
const path = require('path');
const root = __dirname;
const assets = path.join(root, 'common', 'src', 'main', 'resources', 'assets', 'akaishi');
const texRoot = path.join(assets, 'textures');

const targets = [
  'akaishi_reactor_controller_top', 'akaishi_reactor_controller_front',
  'akaishi_reactor_controller_side', 'akaishi_reactor_controller_bottom',
  'akaishi_life_activator_top', 'akaishi_life_activator_side', 'akaishi_life_activator_bottom',
  'akaishi_life_breeder_top', 'akaishi_life_breeder_side',
  'akaishi_life_centrifuge_top', 'akaishi_life_centrifuge_side', 'akaishi_life_centrifuge_bottom',
  'akaishi_life_fusion_anvil_top', 'akaishi_life_fusion_anvil_side', 'akaishi_life_fusion_anvil_bottom',
  'akaishi_life_struct_top', 'akaishi_life_struct_side',
  'akaishi_transgene_factory_top', 'akaishi_transgene_factory_side',
  'akaishi_life_conversion_architecture_top', 'akaishi_life_conversion_architecture_front',
  'akaishi_life_conversion_architecture_side', 'akaishi_life_conversion_architecture_bottom',
  'akaishi_life_conversion_architecture_formed_top', 'akaishi_life_conversion_architecture_formed_front',
  'akaishi_life_conversion_architecture_formed_side', 'akaishi_life_conversion_architecture_formed_bottom',
  'akaishi_life_energy_cell_serializer_formed_top', 'akaishi_life_energy_cell_serializer_formed_front',
  'akaishi_life_energy_cell_serializer_formed_side', 'akaishi_life_energy_cell_serializer_formed_bottom',
  'machine_bottom'
];

// 收集模型 JSON 中 textures 块的引用
function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out);
    else if (e.name.endsWith('.json')) out.push(p);
  }
  return out;
}
const referenced = new Set();
for (const f of walk(assets)) {
  let j;
  try { j = JSON.parse(fs.readFileSync(f, 'utf8').replace(/^\uFEFF/, '')); } catch { continue; }
  const visit = (o) => {
    if (!o || typeof o !== 'object') return;
    for (const [k, v] of Object.entries(o)) {
      if (k === 'textures' && v && typeof v === 'object') {
        for (const val of Object.values(v)) {
          if (typeof val === 'string' && val.startsWith('akaishi:')) referenced.add(val.slice('akaishi:'.length));
        }
      } else if (typeof v === 'object') visit(v);
    }
  };
  visit(j);
}

let fail = 0;
for (const name of targets) {
  const p = path.join(texRoot, 'block', name + '.png');
  if (!fs.existsSync(p)) { console.log('MISSING ' + name); fail++; continue; }
  const b = fs.readFileSync(p);
  const sig = b.slice(0, 8).toString('hex');
  if (sig !== '89504e470d0a1a0a') { console.log('BADPNG  ' + name); fail++; continue; }
  const w = b.readUInt32BE(16), h = b.readUInt32BE(20);
  if (w !== 64 || h !== 64) { console.log('SIZE ' + w + 'x' + h + '  ' + name); fail++; continue; }
  if (!referenced.has('block/' + name)) { console.log('ORPHAN  ' + name); fail++; continue; }
  console.log('OK      ' + name);
}
console.log(fail === 0 ? 'ALL GREEN (' + targets.length + ')' : 'FAILED ' + fail + '/' + targets.length);
process.exit(fail === 0 ? 0 : 1);
