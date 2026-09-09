const fs=require('fs'),path=require('path');
const root=__dirname, assets=path.join(root,'common/src/main/resources/assets/akaishi');
const seen=new Set(), textures=new Set();
function json(p){return JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));}
function png(p){const b=fs.readFileSync(p);if(b.subarray(0,8).toString('hex')!=='89504e470d0a1a0a'||b.toString('ascii',12,16)!=='IHDR')throw Error('Invalid PNG '+p);return [b.readUInt32BE(16),b.readUInt32BE(20)];}
function model(ref){if(!ref.startsWith('akaishi:'))return;if(seen.has(ref))return;seen.add(ref);const m=json(path.join(assets,'models',ref.split(':')[1]+'.json'));if(m.parent)model(m.parent);for(const t of Object.values(m.textures||{})){if(t.startsWith('akaishi:')){const p=path.join(assets,'textures',t.split(':')[1]+'.png');png(p);textures.add(t);}}}
function walk(v){if(Array.isArray(v))v.forEach(walk);else if(v&&typeof v==='object'){if(v.model)model(v.model);Object.values(v).forEach(walk);}}
let states=0;for(const f of fs.readdirSync(path.join(assets,'blockstates'))){if(/purifier|decay/.test(f)){walk(json(path.join(assets,'blockstates',f)));states++;}}
const fixed=['purifier','life_purifier'].flatMap(n=>['side','top','bottom'].map(s=>'akaishi_'+n+'_'+s));
const vanilla=['decay_purifier'].flatMap(n=>['side','top','bottom'].map(s=>'akaishi_'+n+'_'+s)).concat(['akaishi_decay_stone','akaishi_decay_cobblestone','akaishi_decay_stone_bricks','akaishi_decay_sand','akaishi_decay_soil','akaishi_decay_gravel','akaishi_decay_grass_block_top','akaishi_decay_grass_block_side','akaishi_decay_log','akaishi_decay_log_top','akaishi_decay_planks','akaishi_decay_door_top','akaishi_decay_door_bottom','akaishi_decay_trapdoor']);
const names=fixed.concat(vanilla);
for(const n of fixed){const p=path.join(assets,'textures/block',n+'.png');const size=png(p);if(process.argv.includes('--check-output')&&(size[0]!==64||size[1]!==64))throw Error('Wrong dimensions '+p);if(!textures.has('akaishi:block/'+n))throw Error('Unreferenced '+n);if(fs.existsSync(p+'.mcmeta'))throw Error('Animated target requires explicit frame handling '+n);}
for(const n of vanilla){const p=path.join(assets,'textures/block',n+'.png');png(p);if(!textures.has('akaishi:block/'+n))throw Error('Unreferenced '+n);if(fs.existsSync(p+'.mcmeta'))throw Error('Animated target requires explicit frame handling '+n);}
console.log(JSON.stringify({states,models:seen.size,referencedTextures:textures.size,targets:names.length,remainingReferenced:[...textures].filter(t=>!names.includes(t.split('/').pop()))},null,2));
