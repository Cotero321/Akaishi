const fs = require('fs');
const path = require('path');
const root = __dirname;
const assets = path.join(root, 'common', 'src', 'main', 'resources', 'assets', 'akaishi');
const textureDir = path.join(assets, 'textures', 'block');
const names = ['akaishi_fluid_tank_basic_side','akaishi_fluid_tank_advanced_side','akaishi_fluid_tank_super_side'];
function pngInfo(file) { const b=fs.readFileSync(file); const ok=b.subarray(0,8).equals(Buffer.from([137,80,78,71,13,10,26,10])); return {ok,width:b.readUInt32BE(16),height:b.readUInt32BE(20)}; }
function walk(dir) { let out=[]; for(const e of fs.readdirSync(dir,{withFileTypes:true})){const p=path.join(dir,e.name); if(e.isDirectory()) out.push(...walk(p)); else if(e.name.endsWith('.json')) out.push(p);} return out; }
const jsonFiles=walk(assets); const text=jsonFiles.map(f=>[f,fs.readFileSync(f,'utf8')]);
for(const n of names){ const png=path.join(textureDir,n+'.png'); const hits=text.filter(([,s])=>s.includes('akaishi:block/'+n)); console.log(JSON.stringify({name:n,png:fs.existsSync(png)?pngInfo(png):null,consumers:hits.map(([f])=>path.relative(root,f))})); }
console.log('forge_override', JSON.stringify(names.map(n=>({name:n,forge:fs.existsSync(path.join(root,'forge','src','main','resources','assets','akaishi','textures','block',n+'.png'))}))));
