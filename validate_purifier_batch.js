const fs=require('fs'),path=require('path');
const root=__dirname,assets=path.join(root,'common/src/main/resources/assets/akaishi');
const names=['purifier','life_purifier','advanced_purifier'].flatMap(f=>['side','top','bottom'].map(s=>`akaishi_${f}_${s}`)).concat(['matrix_casing','matrix_controller','matrix_controller_formed','matrix_structure_glass','energy_input','item_input','item_output'].map(s=>'akaishi_purifier_'+s));
const seen=new Set(),textures=new Set();
function json(p){return JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));}
function png(p){const b=fs.readFileSync(p);if(b.subarray(0,8).toString('hex')!=='89504e470d0a1a0a'||b.toString('ascii',12,16)!=='IHDR')throw Error('Invalid PNG '+p);return [b.readUInt32BE(16),b.readUInt32BE(20)];}
function model(ref){if(!ref.startsWith('akaishi:')||seen.has(ref))return;seen.add(ref);const m=json(path.join(assets,'models',ref.split(':')[1]+'.json'));if(m.parent)model(m.parent);for(const t of Object.values(m.textures||{})){if(t.startsWith('akaishi:')){png(path.join(assets,'textures',t.split(':')[1]+'.png'));textures.add(t);}}}
function walk(v){if(Array.isArray(v))v.forEach(walk);else if(v&&typeof v==='object'){if(v.model)model(v.model);Object.values(v).forEach(walk);}}
let states=0;for(const f of fs.readdirSync(path.join(assets,'blockstates')).filter(f=>f.includes('purifier'))){walk(json(path.join(assets,'blockstates',f)));model('akaishi:item/'+f.slice(0,-5));states++;}
for(const name of names){if(!/^[a-z0-9_]+$/.test(name))throw Error('Invalid name');const p=path.join(assets,'textures/block',name+'.png');const size=png(p);if(!textures.has('akaishi:block/'+name))throw Error('Unreferenced '+name);if(fs.existsSync(p+'.mcmeta'))throw Error('Animated asset '+name);if(process.argv.includes('--check-output')&&size.some(x=>x!==64))throw Error('Not 64x64 '+name);}
console.log(JSON.stringify({states,models:seen.size,referencedTextures:textures.size,targets:names.length,remainingReferenced:[...textures].filter(t=>!names.includes(t.split('/').pop()))},null,2));
