// 校验流体罐 / 等离子罐家族：PNG 合法性、六面尺寸一致、模型引用完整。
const fs = require('fs');
const path = require('path');
const root = __dirname;
const assets = path.join(root, 'common', 'src', 'main', 'resources', 'assets', 'akaishi');
const texDir = path.join(assets, 'textures', 'block');
const check = process.argv.includes('--check-output');

// 名称 -> 期望边长（侧面已重制：流体罐 32，等离子 64）
const targets = {
  akaishi_fluid_tank_bottom: 32,
  akaishi_fluid_tank_basic_top: 32,
  akaishi_fluid_tank_basic_side: 32,
  akaishi_fluid_tank_advanced_top: 32,
  akaishi_fluid_tank_advanced_side: 32,
  akaishi_fluid_tank_super_top: 32,
  akaishi_fluid_tank_super_side: 32,
  akaishi_plasma_tank_top: 64,
  akaishi_plasma_tank_bottom: 64,
  akaishi_plasma_tank_side: 64,
  akaishi_plasma_filler_side: 64,
  akaishi_plasma_filler_top: 64,
  akaishi_plasma_filler_bottom: 64,
};

function pngSize(file) {
  const b = fs.readFileSync(file);
  if (!b.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))) throw Error('Not a PNG: ' + file);
  return [b.readUInt32BE(16), b.readUInt32BE(20)];
}

function walk(dir) {
  const out = [];
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) out.push(...walk(p));
    else if (e.name.endsWith('.json')) out.push(p);
  }
  return out;
}

const jsonText = walk(assets).map(f => fs.readFileSync(f, 'utf8'));

for (const [name, size] of Object.entries(targets)) {
  const file = path.join(texDir, name + '.png');
  if (!fs.existsSync(file)) {
    if (check) throw Error('Missing texture: ' + name);
    continue;
  }
  const [w, h] = pngSize(file);
  if (check && (w !== size || h !== size)) throw Error(`Wrong dimensions ${name}: ${w}x${h}, expected ${size}x${size}`);
  if (!jsonText.some(s => s.includes('akaishi:block/' + name))) throw Error('Unreferenced texture: ' + name);
  if (fs.existsSync(file + '.mcmeta')) throw Error('Animated target requires explicit frame handling: ' + name);
}

console.log('fluid/plasma tank assets ok:', Object.keys(targets).length, 'targets,', check ? 'output verified' : 'preflight');
