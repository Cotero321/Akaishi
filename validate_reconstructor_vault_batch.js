const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, 'common/src/main/resources/assets/akaishi');
const names = ['item_reconstructor', 'plant_cultivator', 'cultivator', 'surgery', 'trait_reforger', 'upgrade_station', 'potion_table', 'potion_cabinet', 'sample_vault', 'organ_vault'];
const errors = [];
const used = new Set();
let jsonCount = 0;
let pngCount = 0;
function check(condition, message) {
    if (!condition) errors.push(message);
}
function json(relative) {
    jsonCount++;
    return JSON.parse(fs.readFileSync(path.join(root, relative), 'utf8').replace(/^\uFEFF/, ''));
}
function texture(reference, textures) {
    const seen = new Set();
    while (typeof reference === 'string' && reference.startsWith('#')) {
        if (seen.has(reference)) throw Error('Cyclic texture ' + reference);
        seen.add(reference);
        reference = textures[reference.slice(1)];
    }
    check(typeof reference === 'string' && reference.startsWith('akaishi:block/'), 'Invalid texture ' + reference);
    if (typeof reference !== 'string' || !reference.startsWith('akaishi:')) return reference;
    check(fs.existsSync(path.join(root, 'textures', reference.slice(8) + '.png')), 'Missing texture ' + reference);
    return reference;
}
for (const name of names) {
    const id = 'akaishi_' + name;
    try {
        const block = json('models/block/' + id + '.json');
        const item = json('models/item/' + id + '.json');
        const state = json('blockstates/' + id + '.json');
        const model = 'akaishi:block/' + id;
        check(item.parent === model, id + ': item parent mismatch');
        const variants = Object.values(state.variants || {}).flat();
        const multipart = (state.multipart || []).flatMap(part => [part.apply].flat());
        const applications = [...variants, ...multipart];
        check(applications.length > 0, id + ': empty blockstate');
        for (const entry of applications) check(entry.model === model, id + ': blockstate model mismatch');
        check(['minecraft:block/cube_bottom_top', 'minecraft:block/block'].includes(block.parent), id + ': unexpected parent');
        const textures = block.textures || {};
        for (const reference of Object.values(textures)) texture(reference, textures);
        if (block.elements) {
            for (const element of block.elements) {
                for (const [direction, face] of Object.entries(element.faces || {})) {
                    const expected = direction === 'up' ? 'top' : direction === 'down' ? 'bottom' : 'side';
                    const actual = texture(face.texture, textures);
                    check(actual === 'akaishi:block/' + id + '_' + expected, id + ': incorrect ' + direction + ' face');
                    used.add(actual);
                }
            }
        } else {
            check(block.parent === 'minecraft:block/cube_bottom_top', id + ': missing geometry');
            for (const face of ['top', 'side', 'bottom']) used.add(texture('#' + face, textures));
        }
        for (const face of ['top', 'side', 'bottom']) {
            const reference = 'akaishi:block/' + id + '_' + face;
            check(texture('#' + face, textures) === reference, id + ': incorrect ' + face + ' mapping');
            check(used.has(reference), reference + ': unused texture');
            const png = fs.readFileSync(path.join(root, 'textures/block', id + '_' + face + '.png'));
            pngCount++;
            check(png.length >= 33 && png.subarray(0, 8).toString('hex') === '89504e470d0a1a0a' && png.toString('ascii', 12, 16) === 'IHDR', reference + ': invalid PNG');
            check(png.readUInt32BE(16) === 64 && png.readUInt32BE(20) === 64, reference + ': expected 64x64');
        }
        console.log(id + ': top/side/bottom references verified');
    } catch (error) {
        errors.push(id + ': ' + error.message);
    }
}
console.log(JSON.stringify({ machines: names.length, json: jsonCount, png: pngCount, usedTextures: used.size, errors: errors.length }, null, 2));
for (const error of errors) console.error(error);
process.exitCode = errors.length ? 1 : 0;
