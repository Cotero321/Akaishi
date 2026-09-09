// 大型机器族方块贴图重制（工业机械 + 生物科技风）。
// 覆盖 3 族共 35 张 64x64 不透明贴图：
//   akaishi_fusion_*        —— 聚变（高温橙 / 散热青）
//   akaishi_life_matrix_*   —— 生命转换矩阵（生命绿）
//   akaishi_life_wireless_* —— 生命无线（生命青绿）
// 形制（kind）：shell 外壳 / insulation 隔热层 / glass 结构玻璃 / core 核心 /
//   vent 顶部格栅 / side 侧面散热槽 / plate 平底 / panel 控制面板（含 formed 亮屏）/
//   terminal 终端面板 / port 圆形法兰接口 / frame 框架 / portItem 物品口
// 统一规范：拉丝金属底 + 内凹面板 + 四角螺栓 + 左上受光/右下背光 + 细微颗粒。
// 幂等：首次运行备份原图到 gui_layouts/machine_casing_backup/，之后始终以备份为源。
// 用法：node rework_machine_casing_assets.js
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const ROOT = __dirname;
const DIR = path.join(ROOT, 'common/src/main/resources/assets/akaishi/textures/block');
const BACKUP_DIR = path.join(ROOT, 'gui_layouts/machine_casing_backup');
const PREVIEW = path.join(ROOT, 'gui_layouts/machine_casing_rework_preview.png');
const S = 64;

const CRC_TABLE = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1); t[n] = c; }
  return t;
})();
function crc32(buf) { let c = 0xFFFFFFFF; for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 255] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; }
function writePng(w, h, px) {
  const raw = Buffer.alloc(h * (1 + w * 4));
  let o = 0;
  for (let y = 0; y < h; y++) { raw[o++] = 0; px.copy(raw, o, y * w * 4, (y + 1) * w * 4); o += w * 4; }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8; ihdr[9] = 6;
  const chunk = (tag, data) => {
    const b = Buffer.alloc(12 + data.length);
    b.writeUInt32BE(data.length, 0); b.write(tag, 4, 'ascii'); data.copy(b, 8);
    b.writeUInt32BE(crc32(b.subarray(4, 8 + data.length)), 8 + data.length);
    return b;
  };
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]);
}

const cl = v => v < 0 ? 0 : v > 255 ? 255 : v | 0;
const sh = (c, d) => [cl(c[0] + d), cl(c[1] + d), cl(c[2] + d)];
const mix = (a, b, t) => [cl(a[0] + (b[0] - a[0]) * t), cl(a[1] + (b[1] - a[1]) * t), cl(a[2] + (b[2] - a[2]) * t)];
function h2(x, y, s) {
  let h = Math.imul(x, 374761393) ^ Math.imul(y, 668265263) ^ Math.imul(s, 362437);
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return ((h ^ (h >>> 16)) >>> 0) % 1000 / 1000;
}
function put(px, x, y, c) { if (x < 0 || y < 0 || x >= S || y >= S) return; const o = (y * S + x) * 4; px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; px[o + 3] = 255; }
function rect(px, x0, y0, x1, y1, c) { for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) put(px, x, y, c); }
function disc(px, cx, cy, r, c) { for (let y = cy - r; y <= cy + r; y++) for (let x = cx - r; x <= cx + r; x++) { const dx = x - cx, dy = y - cy; if (dx * dx + dy * dy <= r * r) put(px, x, y, c); } }
function ring(px, cx, cy, r0, r1, c) { for (let y = cy - r1; y <= cy + r1; y++) for (let x = cx - r1; x <= cx + r1; x++) { const d2 = (x - cx) ** 2 + (y - cy) ** 2; if (d2 >= r0 * r0 && d2 <= r1 * r1) put(px, x, y, c); } }
function line(px, x0, y0, x1, y1, c) {
  const dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
  let err = dx - dy, x = x0, y = y0;
  for (;;) { put(px, x, y, c); if (x === x1 && y === y1) break; const e2 = 2 * err; if (e2 > -dy) { err -= dy; x += sx; } if (e2 < dx) { err += dx; y += sy; } }
}
// 拉丝金属底板 + 四边倒角
function plate(px, pal, seed) {
  for (let y = 0; y < S; y++) for (let x = 0; x < S; x++) {
    const brush = (h2(x, y, seed) - 0.5) * 12 + (h2(x >> 2, 0, seed + 7) - 0.5) * 6;
    put(px, x, y, sh(pal.base, brush));
  }
  for (let i = 0; i < S; i++) { put(px, i, 0, pal.light); put(px, 0, i, pal.light); put(px, i, S - 1, pal.dark); put(px, S - 1, i, pal.dark); }
}
// 外框
function border(px, x0, y0, x1, y1, pal, w) {
  for (let x = x0; x <= x1; x++) for (let d = 0; d < w; d++) { put(px, x, y0 + d, pal.light); put(px, x, y1 - d, pal.dark); }
  for (let y = y0; y <= y1; y++) for (let d = 0; d < w; d++) { put(px, x0 + d, y, pal.light); put(px, x1 - d, y, pal.dark); }
}
// 内凹面板：上/左为阴影，下/右受光
function recess(px, x0, y0, x1, y1, c, pal) {
  rect(px, x0, y0, x1, y1, c);
  for (let x = x0; x <= x1; x++) { put(px, x, y0, pal.dark); put(px, x, y1, sh(c, 20)); }
  for (let y = y0; y <= y1; y++) { put(px, x0, y, pal.dark); put(px, x1, y, sh(c, 20)); }
}
// 5x5 螺栓
function bolt(px, x, y, pal) {
  rect(px, x, y, x + 4, y + 4, pal.dark);
  rect(px, x + 1, y + 1, x + 3, y + 3, pal.light);
  put(px, x + 2, y + 2, pal.dark);
}
function bolts4(px, pal) { for (const [bx, by] of [[6, 6], [54, 6], [6, 54], [54, 54]]) bolt(px, bx, by, pal); }
// 细微颗粒，打散死平色
function grain(px, seed) {
  for (let y = 0; y < S; y++) for (let x = 0; x < S; x++) {
    const o = (y * S + x) * 4, n = (h2(x, y, seed + 31) - 0.5) * 10;
    px[o] = cl(px[o] + n); px[o + 1] = cl(px[o + 1] + n); px[o + 2] = cl(px[o + 2] + n);
  }
}

// —— 形制 ——

// 机械外壳：外框 + 内凹大面板 + 十字加强筋 + 中央徽记
function kShell(px, pal) {
  plate(px, pal, 11);
  border(px, 2, 2, 61, 61, pal, 2);
  recess(px, 8, 8, 55, 55, sh(pal.base, -12), pal);
  rect(px, 8, 30, 55, 33, sh(pal.base, 16));
  rect(px, 30, 8, 33, 55, sh(pal.base, 16));
  for (let x = 8; x <= 55; x++) put(px, x, 33, pal.dark);
  for (let y = 8; y <= 55; y++) put(px, 33, y, pal.dark);
  disc(px, 31, 31, 6, pal.dark);
  disc(px, 31, 31, 4, pal.accent);
  disc(px, 31, 31, 2, pal.accentHot);
  bolts4(px, pal);
}

// 隔热层：横向层压条纹 + 左右包边
function kInsulation(px, pal) {
  plate(px, pal, 17);
  for (let y = 0; y < S; y++) {
    const band = Math.floor(y / 6) % 2 === 0;
    if (!band) for (let x = 0; x < S; x++) put(px, x, y, sh(pal.base, 14));
    if (y % 6 === 0) for (let x = 0; x < S; x++) put(px, x, y, pal.dark);
  }
  border(px, 0, 0, S - 1, S - 1, pal, 3);
  rect(px, 4, 8, 7, 55, sh(pal.base, 22));
  rect(px, 56, 8, 59, 55, sh(pal.base, -18));
  bolts4(px, pal);
}

// 结构玻璃：金属框 + 玻璃面板 + 斜向高光 + 中挺
function kGlass(px, pal) {
  plate(px, pal, 23);
  border(px, 1, 1, 62, 62, pal, 3);
  for (let y = 5; y <= 58; y++) for (let x = 5; x <= 58; x++) {
    const t = (x + y) / 116;
    put(px, x, y, mix(sh(pal.base, 30), sh(pal.base, -14), t));
  }
  for (let k = 0; k < 3; k++) line(px, 9 + k * 17, 54, 34 + k * 17, 12, sh(pal.base, 46));
  rect(px, 30, 5, 33, 58, pal.light);   // 竖向中挺
  rect(px, 5, 30, 58, 33, pal.light);   // 横向中挺
  for (let x = 5; x <= 58; x++) put(px, x, 33, pal.dark);
  for (let y = 5; y <= 58; y++) put(px, 33, y, pal.dark);
  bolts4(px, pal);
}

// 核心：中央反应堆视窗
function kCore(px, pal, bright) {
  plate(px, pal, 29);
  border(px, 2, 2, 61, 61, pal, 2);
  recess(px, 6, 6, 57, 57, sh(pal.base, -14), pal);
  disc(px, 31, 31, 22, pal.dark);
  disc(px, 31, 31, 20, sh(pal.base, 10));
  ring(px, 31, 31, 15, 18, pal.dark);
  ring(px, 31, 31, 12, 15, pal.accent);
  disc(px, 31, 31, 11, sh(pal.glow, bright ? 0 : -26));
  ring(px, 31, 31, 5, 8, pal.accentHot);
  disc(px, 31, 31, 4, bright ? [255, 255, 255] : pal.accentHot);
  // 环向卡榫
  for (let a = 0; a < 8; a++) {
    const rad = a * Math.PI / 4;
    put(px, 31 + Math.round(Math.cos(rad) * 19), 31 + Math.round(Math.sin(rad) * 19), pal.accentHot);
  }
  bolts4(px, pal);
}

// 顶部格栅
function kVent(px, pal) {
  plate(px, pal, 31);
  border(px, 2, 2, 61, 61, pal, 2);
  for (let y = 10; y <= 54; y += 5) {
    rect(px, 8, y, 55, y + 2, pal.dark);
    for (let x = 8; x <= 55; x++) put(px, x, y + 3, sh(pal.base, 22));
  }
  rect(px, 28, 8, 35, 55, sh(pal.base, 12));   // 中梁
  bolts4(px, pal);
}

// 侧面散热槽：竖向导流片
function kSide(px, pal) {
  plate(px, pal, 37);
  border(px, 2, 2, 61, 61, pal, 2);
  for (let x = 9; x <= 54; x += 6) {
    rect(px, x, 9, x + 2, 54, sh(pal.base, 20));
    for (let y = 9; y <= 54; y++) put(px, x + 3, y, pal.dark);
  }
  rect(px, 8, 28, 55, 35, sh(pal.base, 6));    // 中腰加强带
  for (let x = 8; x <= 55; x++) put(px, x, 35, pal.dark);
  bolts4(px, pal);
}

// 平底：整体平整 + 中央铭牌
function kPlate(px, pal) {
  plate(px, pal, 41);
  border(px, 2, 2, 61, 61, pal, 2);
  recess(px, 20, 26, 43, 37, sh(pal.base, -10), pal);
  rect(px, 24, 30, 39, 33, pal.accent);
  bolts4(px, pal);
}

// 控制面板：屏幕 + 按钮 + 指示灯（bright = 运行态亮屏）
function kPanel(px, pal, bright) {
  plate(px, pal, 43);
  border(px, 2, 2, 61, 61, pal, 2);
  recess(px, 8, 8, 55, 38, [18, 22, 28], pal);          // 屏幕凹槽
  if (bright) {
    for (let y = 10; y <= 36; y++) for (let x = 10; x <= 53; x++) {
      const t = (y - 10) / 26;
      put(px, x, y, mix(sh(pal.accentHot, -30), sh(pal.glow, -50), t));
    }
    for (let y = 14; y <= 34; y += 5) rect(px, 13, y, 50, y, sh(pal.accentHot, 40));  // 读数行
    rect(px, 13, 20, 30, 20, [255, 255, 255]);
  } else {
    for (let y = 10; y <= 36; y++) for (let x = 10; x <= 53; x++)
      put(px, x, y, mix([30, 36, 44], [16, 20, 26], (y - 10) / 26));
    for (let y = 13; y <= 33; y += 5) rect(px, 13, y, 50, y, [44, 52, 62]);
  }
  line(px, 10, 12, 53, 12, sh(pal.base, 60));           // 屏幕反光
  // 按钮排
  for (let i = 0; i < 4; i++) {
    const bx = 10 + i * 12;
    rect(px, bx, 44, bx + 8, 52, pal.dark);
    rect(px, bx + 1, 45, bx + 7, 50, i === 0 ? pal.accent : sh(pal.base, 18));
    put(px, bx + 1, 45, sh(pal.base, 44));
  }
  // 指示灯
  for (let i = 0; i < 3; i++) disc(px, 52 - i * 7, 58, 2, i === 0 ? (bright ? pal.accentHot : pal.accent) : sh(pal.base, -6));
  bolts4(px, pal);
}

// 终端面板：大屏 + 键盘阵列
function kTerminal(px, pal, bright) {
  plate(px, pal, 47);
  border(px, 2, 2, 61, 61, pal, 2);
  recess(px, 7, 7, 56, 32, [18, 22, 28], pal);
  if (bright) {
    for (let y = 9; y <= 30; y++) for (let x = 9; x <= 54; x++)
      put(px, x, y, mix(sh(pal.accentHot, -20), sh(pal.glow, -60), (y - 9) / 21));
    for (let y = 13; y <= 27; y += 4) rect(px, 12, y, 51, y, sh(pal.accentHot, 50));
    rect(px, 12, 13, 22, 13, [255, 255, 255]);
  } else {
    for (let y = 9; y <= 30; y++) for (let x = 9; x <= 54; x++)
      put(px, x, y, mix([30, 36, 44], [16, 20, 26], (y - 9) / 21));
    for (let y = 12; y <= 28; y += 4) rect(px, 12, y, 51, y, [44, 52, 62]);
  }
  line(px, 9, 10, 54, 10, sh(pal.base, 60));
  // 键盘阵列 6x3
  for (let r = 0; r < 3; r++) for (let c = 0; c < 6; c++) {
    const bx = 8 + c * 8, by = 38 + r * 7;
    rect(px, bx, by, bx + 6, by + 5, pal.dark);
    rect(px, bx + 1, by + 1, bx + 5, by + 4, sh(pal.base, 16));
  }
  disc(px, 57, 57, 3, bright ? pal.accentHot : pal.accent);
  bolts4(px, pal);
}

// 圆形法兰接口（能量口）
function kPort(px, pal, bright) {
  plate(px, pal, 53);
  border(px, 2, 2, 61, 61, pal, 2);
  disc(px, 31, 31, 23, pal.dark);
  disc(px, 31, 31, 21, sh(pal.base, 12));
  for (let a = 0; a < 8; a++) {
    const rad = a * Math.PI / 4;
    bolt(px, 31 + Math.round(Math.cos(rad) * 18) - 2, 31 + Math.round(Math.sin(rad) * 18) - 2, pal);
  }
  ring(px, 31, 31, 12, 15, pal.dark);
  ring(px, 31, 31, 9, 12, pal.accent);
  disc(px, 31, 31, 8, sh(pal.glow, bright ? 0 : -30));
  ring(px, 31, 31, 4, 6, pal.accentHot);
  disc(px, 31, 31, 3, bright ? [255, 255, 255] : pal.accentHot);
  bolts4(px, pal);
}

// 实心三角：顶点在 (ax,ay)，沿 (dx,dy) 方向展开
function triApex(px, ax, ay, dx, dy, len, halfW, c) {
  for (let t = 0; t <= len; t++) {
    const hw = Math.round((t / len) * halfW);
    if (dx === 0) rect(px, ax - hw, ay + dy * t, ax + hw, ay + dy * t, c);
    else rect(px, ax + dx * t, ay - hw, ax + dx * t, ay + hw, c);
  }
}

// 物品口：方形舱门 + 四向三角标（inward = 输入）
function kPortItem(px, pal, inward) {
  plate(px, pal, 59);
  border(px, 2, 2, 61, 61, pal, 2);
  recess(px, 12, 12, 51, 51, sh(pal.base, -14), pal);
  rect(px, 16, 16, 47, 47, pal.dark);
  rect(px, 18, 18, 45, 45, sh(pal.base, 6));
  const c = inward ? pal.accent : pal.accentHot;
  if (inward) {
    triApex(px, 31, 24, 0, -1, 9, 7, c);
    triApex(px, 31, 38, 0, 1, 9, 7, c);
    triApex(px, 24, 31, -1, 0, 9, 7, c);
    triApex(px, 38, 31, 1, 0, 9, 7, c);
  } else {
    triApex(px, 31, 15, 0, 1, 9, 7, c);
    triApex(px, 31, 47, 0, -1, 9, 7, c);
    triApex(px, 15, 31, 1, 0, 9, 7, c);
    triApex(px, 47, 31, -1, 0, 9, 7, c);
  }
  disc(px, 31, 31, 5, pal.dark);
  disc(px, 31, 31, 3, c);
  bolts4(px, pal);
}

// 框架：镂空格架（不透明，用深色底 + 交叉梁表现）
function kFrame(px, pal) {
  plate(px, pal, 61);
  border(px, 2, 2, 61, 61, pal, 3);
  for (let y = 8; y <= 55; y++) for (let x = 8; x <= 55; x++) put(px, x, y, sh(pal.base, -30));
  for (let x = 8; x <= 55; x += 16) rect(px, x, 8, x + 3, 55, sh(pal.base, 16));
  for (let y = 8; y <= 55; y += 16) rect(px, 8, y, 55, y + 3, sh(pal.base, 16));
  for (let x = 8; x <= 55; x++) put(px, x, 55, pal.dark);
  for (let y = 8; y <= 55; y++) put(px, 55, y, pal.dark);
  disc(px, 31, 31, 7, pal.dark);
  disc(px, 31, 31, 5, pal.accent);
  disc(px, 31, 31, 2, pal.accentHot);
  bolts4(px, pal);
}

// —— 调色板 ——
function pal(base, light, dark, accent, accentHot, glow) { return { base, light, dark, accent, accentHot, glow }; }
const FUSION = pal([74, 76, 84], [124, 128, 140], [38, 40, 46], [255, 128, 44], [255, 206, 130], [200, 60, 20]);
const FUSION_COOL = pal([70, 78, 86], [118, 130, 142], [36, 42, 48], [72, 196, 220], [186, 244, 255], [28, 108, 140]);
const LIFE_MATRIX = pal([68, 78, 74], [116, 132, 126], [36, 44, 40], [40, 180, 40], [150, 240, 150], [24, 108, 26]);
const LIFE_WIRELESS = pal([66, 76, 84], [114, 130, 142], [34, 42, 50], [64, 224, 192], [186, 255, 240], [26, 138, 118]);

const ITEMS = [
  // 聚变
  { n: 'akaishi_fusion_controller_top', p: FUSION, k: 'vent' },
  { n: 'akaishi_fusion_controller_side', p: FUSION, k: 'side' },
  { n: 'akaishi_fusion_controller_bottom', p: FUSION, k: 'plate' },
  { n: 'akaishi_fusion_controller_front', p: FUSION, k: 'panel', b: false },
  { n: 'akaishi_fusion_controller_formed_front', p: FUSION, k: 'panel', b: true },
  { n: 'akaishi_fusion_cooler_frame', p: FUSION_COOL, k: 'frame' },
  { n: 'akaishi_fusion_efficiency_frame', p: FUSION, k: 'frame' },
  { n: 'akaishi_fusion_fuel_frame', p: FUSION, k: 'frame' },
  { n: 'akaishi_fusion_core', p: FUSION, k: 'core', b: true },
  { n: 'akaishi_fusion_energy_output', p: FUSION, k: 'port', b: true },
  { n: 'akaishi_fusion_item_input', p: FUSION, k: 'portItem', in: true },
  { n: 'akaishi_fusion_item_output', p: FUSION, k: 'portItem', in: false },
  { n: 'akaishi_fusion_fuel_aggregator_top', p: FUSION, k: 'vent' },
  { n: 'akaishi_fusion_fuel_aggregator_side', p: FUSION, k: 'side' },
  { n: 'akaishi_fusion_fuel_aggregator_bottom', p: FUSION, k: 'plate' },
  { n: 'akaishi_fusion_insulation', p: FUSION, k: 'insulation' },
  { n: 'akaishi_fusion_shell', p: FUSION, k: 'shell' },
  { n: 'akaishi_fusion_structure_glass', p: FUSION, k: 'glass' },
  // 生命转换矩阵
  { n: 'akaishi_life_matrix_casing', p: LIFE_MATRIX, k: 'shell' },
  { n: 'akaishi_life_matrix_controller', p: LIFE_MATRIX, k: 'panel', b: false },
  { n: 'akaishi_life_matrix_controller_formed', p: LIFE_MATRIX, k: 'panel', b: true },
  { n: 'akaishi_life_matrix_energy_input', p: LIFE_MATRIX, k: 'port', b: false },
  { n: 'akaishi_life_matrix_energy_output', p: LIFE_MATRIX, k: 'port', b: true },
  { n: 'akaishi_life_matrix_structure_glass', p: LIFE_MATRIX, k: 'glass' },
  // 生命无线
  { n: 'akaishi_life_wireless_controller_top', p: LIFE_WIRELESS, k: 'vent' },
  { n: 'akaishi_life_wireless_controller_side', p: LIFE_WIRELESS, k: 'side' },
  { n: 'akaishi_life_wireless_controller_bottom', p: LIFE_WIRELESS, k: 'plate' },
  { n: 'akaishi_life_wireless_controller_front', p: LIFE_WIRELESS, k: 'panel', b: true },
  { n: 'akaishi_life_wireless_core', p: LIFE_WIRELESS, k: 'core', b: true },
  { n: 'akaishi_life_wireless_input_port', p: LIFE_WIRELESS, k: 'port', b: false },
  { n: 'akaishi_life_wireless_output_port', p: LIFE_WIRELESS, k: 'port', b: true },
  { n: 'akaishi_life_wireless_shell', p: LIFE_WIRELESS, k: 'shell' },
  { n: 'akaishi_life_wireless_structure_glass', p: LIFE_WIRELESS, k: 'glass' },
  { n: 'akaishi_life_wireless_terminal', p: LIFE_WIRELESS, k: 'terminal', b: false },
  { n: 'akaishi_life_wireless_terminal_formed', p: LIFE_WIRELESS, k: 'terminal', b: true },
];

const KINDS = {
  shell: (px, it) => kShell(px, it.p),
  insulation: (px, it) => kInsulation(px, it.p),
  glass: (px, it) => kGlass(px, it.p),
  core: (px, it) => kCore(px, it.p, it.b),
  vent: (px, it) => kVent(px, it.p),
  side: (px, it) => kSide(px, it.p),
  plate: (px, it) => kPlate(px, it.p),
  panel: (px, it) => kPanel(px, it.p, it.b),
  terminal: (px, it) => kTerminal(px, it.p, it.b),
  port: (px, it) => kPort(px, it.p, it.b),
  portItem: (px, it) => kPortItem(px, it.p, it.in),
  frame: (px, it) => kFrame(px, it.p),
};

function checker(w, h, cell) {
  const px = Buffer.alloc(w * h * 4);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const on = ((Math.floor(x / cell) + Math.floor(y / cell)) & 1) === 0;
    const v = on ? 70 : 52, o = (y * w + x) * 4;
    px[o] = v; px[o + 1] = v; px[o + 2] = v + 4; px[o + 3] = 255;
  }
  return px;
}
function blit(dst, DW, src, SW, SH, ox, oy, f) {
  for (let y = 0; y < SH * f; y++) for (let x = 0; x < SW * f; x++) {
    const so = (Math.floor(y / f) * SW + Math.floor(x / f)) * 4;
    if (src[so + 3] === 0) continue;
    const dof = ((oy + y) * DW + ox + x) * 4;
    dst[dof] = src[so]; dst[dof + 1] = src[so + 1]; dst[dof + 2] = src[so + 2]; dst[dof + 3] = 255;
  }
}

function main() {
  fs.mkdirSync(BACKUP_DIR, { recursive: true });
  const cells = [];
  for (const it of ITEMS) {
    const src = path.join(DIR, it.n + '.png');
    const bak = path.join(BACKUP_DIR, it.n + '.png');
    if (!fs.existsSync(src)) { console.log('MISS ' + it.n); continue; }
    if (!fs.existsSync(bak)) fs.copyFileSync(src, bak);
    const px = Buffer.alloc(S * S * 4);
    KINDS[it.k](px, it);
    grain(px, it.n.length);
    fs.writeFileSync(src, writePng(S, S, px));
    const colors = new Set();
    for (let i = 0; i < S * S; i++) colors.add(`${px[i * 4]},${px[i * 4 + 1]},${px[i * 4 + 2]}`);
    console.log(it.n.padEnd(42) + ' kind=' + it.k.padEnd(10) + ' colors=' + colors.size);
    cells.push(px);
  }

  const f = 2, pad = 6, cols = 6;
  const rows = Math.ceil(cells.length / cols);
  const cw = S * f + pad * 2, ch = S * f + pad * 2;
  const PW = cw * cols, PH = ch * rows;
  const sheet = checker(PW, PH, 8);
  cells.forEach((px, i) => blit(sheet, PW, px, S, S, (i % cols) * cw + pad, Math.floor(i / cols) * ch + pad, f));
  fs.writeFileSync(PREVIEW, writePng(PW, PH, sheet));
  console.log('共重制 ' + cells.length + ' 张机器贴图');
  console.log('预览: ' + path.relative(ROOT, PREVIEW));
  return 0;
}

process.exit(main());
