import struct, zlib, os
from pathlib import Path

ROOT = Path(__file__).resolve().parent
TEXTURES = ROOT / 'common/src/main/resources/assets/akaishi/textures/block'
TARGETS = ('akaishi_fluid_tank_basic_side.png', 'akaishi_fluid_tank_advanced_side.png', 'akaishi_fluid_tank_super_side.png')

def chunk(kind, data):
    return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

def pixel(tier, x, y):
    frame = x < 4 or x >= 60 or y < 4 or y >= 60
    rivet = ((x in (8, 55)) and (y in (8, 55)))
    band = 27 <= y <= 31 or 36 <= y <= 39
    palette = [(39, 49, 55), (49, 107, 119), (105, 190, 202)]
    accent = [(54, 71, 73), (104, 205, 185), (178, 239, 211)][tier]
    if frame: return (18, 24, 28, 255)
    if rivet: return (185, 196, 190, 255)
    if band: return (*accent, 255)
    if 13 <= x <= 50 and 12 <= y <= 54:
        shade = max(0, 8 - abs(x - 32) // 5)
        base = palette[tier]
        return tuple(min(255, c + shade) for c in base) + (255,)
    return (28, 37, 42, 255)

def write_png(path, tier):
    rows = []
    for y in range(64):
        row = bytearray([0])
        for x in range(64): row.extend(pixel(tier, x, y))
        rows.append(row)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 64, 64, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(b''.join(rows), 9)) + chunk(b'IEND', b'')
    path.write_bytes(data)

for tier, name in enumerate(TARGETS): write_png(TEXTURES / name, tier)
print('generated', ', '.join(TARGETS))
