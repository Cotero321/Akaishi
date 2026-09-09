// 扫描 assets 下未被任何模型 JSON 的 textures 引用的贴图（排除 Java 侧引用的 fluid / armor / gui 等目录）。
// 用法: node scan_orphan_textures.js [--include-java-dirs]
const fs = require('fs');
const path = require('path');
const root = __dirname;
const assets = path.join(root, 'common', 'src', 'main', 'resources', 'assets', 'akaishi');
const texRoot = path.join(assets, 'textures');
const includeJavaDirs = process.argv.includes('--include-java-dirs');

// 由 Java 代码直接引用的目录，跳过（非模型消费）
const javaDirs = ['block/fluid', 'gui', 'mob_effect', 'models/armor', 'mechanical_part'];

function walk(dir, ext, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, ext, out);
    else if (e.name.endsWith(ext)) out.push(p);
  }
  return out;
}

// 只收集模型/方块状态里 "textures" 块中的贴图路径
const referenced = new Set();
for (const f of walk(assets, '.json')) {
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

const orphans = [];
for (const p of walk(texRoot, '.png')) {
  const rel = path.relative(texRoot, p).split(path.sep).join('/').replace(/\.png$/, '');
  if (!includeJavaDirs && javaDirs.some(d => rel.startsWith(d + '/'))) continue;
  if (!referenced.has(rel)) orphans.push(rel);
}

console.log('referenced textures:', referenced.size);
console.log('orphans:', orphans.length);
console.log(orphans.sort().join('\n'));
