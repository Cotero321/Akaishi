// 校验矿石 / 晶体 / 晶洞 / 催化剂 / 收集器家族：PNG 合法性、32x32 尺寸、模型引用完整。
const fs = require('fs');
const path = require('path');
const root = __dirname;
const assets = path.join(root, 'common', 'src', 'main', 'resources', 'assets', 'akaishi');
const texDir = path.join(assets, 'textures', 'block');
const check = process.argv.includes('--check-output');

// 名称 -> 期望边长（本家族统一 32x32 普通方块资产）
const targets = {
  akaishi_ore_low: 32,
  akaishi_ore_medium: 32,
  akaishi_ore_perfect: 32,
  akaishi_ore_flawless: 32,
  deepslate_akaishi_ore_low: 32,
  deepslate_akaishi_ore_medium: 32,
  deepslate_akaishi_ore_perfect: 32,
  deepslate_akaishi_ore_flawless: 32,
  nether_akaishi_ore_low: 32,
  nether_akaishi_ore_medium: 32,
  nether_akaishi_ore_perfect: 32,
  nether_akaishi_ore_flawless: 32,
  end_akaishi_ore_low: 32,
  end_akaishi_ore_medium: 32,
  end_akaishi_ore_perfect: 32,
  end_akaishi_ore_flawless: 32,
  akaishi_geode_flawed: 32,
  akaishi_geode_normal: 32,
  akaishi_geode_perfect: 32,
  akaishi_geode_pristine: 32,
  akaishi_crystal_block: 32,
  akaishi_crystal_cluster: 32,
  raw_akaishi_block: 32,
  akaishi_essence_block: 32,
  akaishi_catalyst_basic: 32,
  akaishi_catalyst_medium: 32,
  akaishi_catalyst_advanced: 32,
  akaishi_catalyst_ultimate: 32,
  akaishi_collector_basic: 32,
  akaishi_collector_medium: 32,
  akaishi_collector_advanced: 32,
  akaishi_collector_ultimate: 32,
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

console.log('ore/crystal assets ok:', Object.keys(targets).length, 'targets,', check ? 'output verified' : 'preflight');
