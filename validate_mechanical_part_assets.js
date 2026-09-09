'use strict';
// 校验机械部件贴图：PNG 头 + 尺寸 + BEWLR 引用可达 + 无孤儿文件
// material 16x16（BEWLR 按 x%mw 平铺取样，必须可无缝平铺）；shape 灰度遮罩 64x64（organ_default 32x32）
const fs = require('node:fs'), path = require('node:path');
const root = __dirname;
const base = path.join(root, 'common/src/main/resources/assets/akaishi');
const matDir = path.join(base, 'textures/mechanical_part/material');
const shpDir = path.join(base, 'textures/mechanical_part/shape');
const renderer = path.join(root, 'forge/src/main/java/com/example/akaishi/forge/client/mechanical/MechanicalPartRenderer.java');

// 与 MechanicalMaterial.registerDefaults() 的 10 个 id 对齐
const materials = ['iron', 'redstone_alloy', 'ceramic_composite', 'resistant_steel',
  'precision_alloy', 'polymerized_redstone', 'bio_ceramic', 'refined_core',
  'alloy_steel', 'psionic_composite'];
// 与 MechanicalPartType 4 类 + organShapes 9 项 + 兜底 organ_default 对齐
const shapes = ['core', 'module', 'shell', 'cooling',
  'organ_eye', 'organ_heart', 'organ_lung', 'organ_viscera', 'organ_kidney',
  'organ_left_arm', 'organ_right_arm', 'organ_left_leg', 'organ_right_leg', 'organ_default'];

const errors = [];
const PNG = Buffer.from('89504e470d0a1a0a', 'hex');

function check(file, w, h) {
  const b = fs.readFileSync(file);
  if (!b.subarray(0, 8).equals(PNG)) throw Error('PNG header');
  const aw = b.readUInt32BE(16), ah = b.readUInt32BE(20);
  if (aw !== w || ah !== h) throw Error('size ' + aw + 'x' + ah + ' expect ' + w + 'x' + h);
}

function orphans(dir, expected) {
  return fs.readdirSync(dir).filter(f => f.endsWith('.png'))
    .map(f => f.slice(0, -4)).filter(n => !expected.includes(n));
}

// 1. material
for (const n of materials) {
  try { check(path.join(matDir, n + '.png'), 16, 16); console.log('OK material/' + n); }
  catch (x) { errors.push('material/' + n + ': ' + x.message); }
}
// 2. shape
for (const n of shapes) {
  const size = n === 'organ_default' ? 32 : 64;
  try { check(path.join(shpDir, n + '.png'), size, size); console.log('OK shape/' + n); }
  catch (x) { errors.push('shape/' + n + ': ' + x.message); }
}
// 3. 孤儿文件
for (const o of orphans(matDir, materials)) errors.push('orphan material/' + o);
for (const o of orphans(shpDir, shapes)) errors.push('orphan shape/' + o);

// 4. 渲染器引用可达：每个 shape 名必须在渲染器中作为字符串出现
try {
  const src = fs.readFileSync(renderer, 'utf8');
  for (const n of shapes) {
    // 部件类经 partType.name().toLowerCase() 取形；器官类在 organShapes 数组内
    const key = n.startsWith('organ_') ? '"' + n.slice(6) + '"' : n.toUpperCase();
    if (!src.includes(key)) errors.push('renderer 未引用 ' + n);
  }
  for (const m of materials) {
    if (!fs.existsSync(path.join(matDir, m + '.png'))) errors.push('material 缺失 ' + m);
  }
} catch (x) { errors.push('renderer 读取失败: ' + x.message); }

console.log(JSON.stringify({ materials: materials.length, shapes: shapes.length, errors: errors.length }, null, 2));
if (errors.length) { errors.forEach(x => console.log('ERR ' + x)); process.exitCode = 1; }
