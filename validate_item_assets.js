'use strict';
// 校验 15 张物品图标：PNG 头 + 32x32 尺寸 + item/generated 模型引用可达
const fs = require('node:fs'), path = require('node:path');
const root = __dirname, base = path.join(root, 'common/src/main/resources/assets/akaishi');
const names = ['akaishi_helmet','akaishi_chestplate','akaishi_leggings','akaishi_boots',
  'akaishi_life_fusion_helmet','akaishi_life_fusion_chestplate','akaishi_life_fusion_leggings','akaishi_life_fusion_boots','akaishi_life_fusion_ingot',
  'akaishi_debug_tool','akaishi_diary','akaishi_machine_energy_upgrade','akaishi_machine_speed_upgrade','akaishi_speed_upgrade','sculk_lifeform'];
let e = [];
for (const n of names) {
  try {
    const m = JSON.parse(fs.readFileSync(path.join(base, 'models/item/' + n + '.json')));
    const l0 = m.textures && m.textures.layer0;
    if (!m.parent || !l0) throw Error('model/layer0 missing');
    const rel = l0.replace('akaishi:', '');
    const b = fs.readFileSync(path.join(base, 'textures/' + rel + '.png'));
    if (!b.subarray(0, 8).equals(Buffer.from('89504e470d0a1a0a', 'hex'))) throw Error('PNG header');
    if (b.readUInt32BE(16) !== 32 || b.readUInt32BE(20) !== 32) throw Error('size ' + b.readUInt32BE(16) + 'x' + b.readUInt32BE(20));
    console.log('OK ' + n);
  } catch (x) { e.push(n + ': ' + x.message); }
}
console.log(JSON.stringify({ targets: names.length, errors: e.length }, null, 2));
if (e.length) { e.forEach(x => console.log('ERR ' + x)); process.exitCode = 1; }
