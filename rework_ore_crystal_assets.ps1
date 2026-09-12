param([switch]$PreviewOnly)
# 重制矿石 / 晶体 / 晶洞 / 催化剂 / 收集器家族材质为 32x32。
# 自然矿物（矿石/晶洞/晶块/粗制块/精华块）走赤石晶簇画法；
# 机器件（催化剂/收集器）走暗红机壳 + 品阶色环的工业画法。
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.Drawing
$root = $PSScriptRoot
$block = Join-Path $root 'common\src\main\resources\assets\akaishi\textures\block'
$preview = Join-Path $root 'gui_layouts'
$node = (Get-Command node -ErrorAction Stop).Source
& $node (Join-Path $root 'validate_ore_crystal_assets.js')
if ($LASTEXITCODE -ne 0) { throw 'Preflight failed' }

function Color($red, $green, $blue, $alpha = 255) {
  [Drawing.Color]::FromArgb($alpha, [Math]::Max(0, [Math]::Min(255, $red)), [Math]::Max(0, [Math]::Min(255, $green)), [Math]::Max(0, [Math]::Min(255, $blue)))
}

# 赤石矿系主色
$oreCore = Color 200 50 50
$oreMid = Color 158 71 71
$oreHi = Color 241 123 120
$oreDeep = Color 122 26 26

# ---- 确定性伪随机（同一方块每次生成结果一致）----
$script:Rng = 1
function Seed-Rng([int]$seed) { $script:Rng = $seed }
function Next-Rand([int]$max) {
  $script:Rng = ([int64]$script:Rng * 1103515245 + 12345) -band 0x7FFFFFFF
  return [int](([Math]::Floor($script:Rng / 65536)) % $max)
}

# 岩石底噪点
function DrawRock($bmp, $n, $base, $dark, $light, $density) {
  for ($y = 0; $y -lt $n; $y++) {
    for ($x = 0; $x -lt $n; $x++) {
      $r = Next-Rand 100
      if ($r -lt $density) { $bmp.SetPixel($x, $y, $dark) }
      elseif ($r -gt (100 - $density)) { $bmp.SetPixel($x, $y, $light) }
      else { $bmp.SetPixel($x, $y, $base) }
    }
  }
}

# 赤石矿斑：中心实核 + 中环 + 深色轮廓，构成棱角分明的晶簇
function DrawOreBlob($bmp, $n, $cx, $cy, $r, $hiChance) {
  for ($dy = -$r; $dy -le $r; $dy++) {
    for ($dx = -$r; $dx -le $r; $dx++) {
      $dist = [Math]::Abs($dx) + [Math]::Abs($dy)
      if ($dist -gt $r) { continue }
      $x = $cx + $dx; $y = $cy + $dy
      if ($x -lt 0 -or $y -lt 0 -or $x -ge $n -or $y -ge $n) { continue }
      if ($dist -eq 0) { $bmp.SetPixel($x, $y, $oreCore) }
      elseif ($dist -eq $r) { $bmp.SetPixel($x, $y, $oreDeep) }
      elseif ((Next-Rand 100) -lt $hiChance) { $bmp.SetPixel($x, $y, $oreHi) }
      else { $bmp.SetPixel($x, $y, $oreMid) }
    }
  }
}

# 矿石：环境基岩 + 按浓度递增的矿斑
function DrawOre($bmp, $n, $base, $dark, $light, $blobs, $radius, $hiChance) {
  DrawRock $bmp $n $base $dark $light 22
  for ($i = 0; $i -lt $blobs; $i++) {
    $cx = 4 + (Next-Rand ($n - 8))
    $cy = 4 + (Next-Rand ($n - 8))
    DrawOreBlob $bmp $n $cx $cy $radius $hiChance
  }
}

# 单颗晶面：菱形轮廓 + 左半受光、右半背光、中轴高光
function DrawGem($bmp, $n, $cx, $cy, $r, $lit, $shade, $hi, $edge) {
  for ($dy = -$r; $dy -le $r; $dy++) {
    for ($dx = -$r; $dx -le $r; $dx++) {
      $d = [Math]::Abs($dx) + [Math]::Abs($dy)
      if ($d -gt $r) { continue }
      $x = $cx + $dx; $y = $cy + $dy
      if ($x -lt 0 -or $y -lt 0 -or $x -ge $n -or $y -ge $n) { continue }
      if ($d -eq $r) { $c = $edge }
      elseif ($dx -lt 0) { $c = $lit }
      elseif ($dx -gt 0) { $c = $shade }
      else { $c = $hi }
      $bmp.SetPixel($x, $y, $c)
    }
  }
}

# 晶洞母岩：粗糙岩面内嵌晶簇，完美度越高晶面越多越大
function DrawGeode($bmp, $n, $rock, $rockDark, $rockLight, $gemCount, $lit, $shade, $hi, $edge) {
  DrawRock $bmp $n $rock $rockDark $rockLight 24
  for ($i = 0; $i -lt $gemCount; $i++) {
    $cx = 4 + (Next-Rand ($n - 8))
    $cy = 4 + (Next-Rand ($n - 8))
    $r = 2 + (Next-Rand 3)
    DrawGem $bmp $n $cx $cy $r $lit $shade $hi $edge
  }
}

# 水晶块：8x8 周期菱形棱面 + 双向对角高光
function DrawCrystalBlock($bmp, $n) {
  $base = Color 158 71 71
  $lit = Color 204 107 106
  $shade = Color 150 58 58
  $hi = Color 241 123 120
  for ($y = 0; $y -lt $n; $y++) {
    for ($x = 0; $x -lt $n; $x++) {
      $cell = ([Math]::Floor($x / 8) + [Math]::Floor($y / 8)) % 2
      $d = ($x + $y) % 8
      $e = (($x - $y) + 64) % 8
      if ($d -eq 0 -or $e -eq 0) { $c = $hi }
      elseif ($cell -eq 0) { $c = $lit }
      else { $c = $shade }
      $bmp.SetPixel($x, $y, $c)
    }
  }
}

# 注：raw_akaishi_block / akaishi_essence_block 已迁到 rework_redstone_family_assets.js 统一出图


# 机壳通用：1px 外缘 + 3px 框架 + 内部底板 + 四角铆钉
function DrawMachineShell($bmp, $n, $edge, $frame, $base, $rivet) {
  for ($y = 0; $y -lt $n; $y++) {
    for ($x = 0; $x -lt $n; $x++) {
      if ($x -eq 0 -or $y -eq 0 -or $x -eq $n - 1 -or $y -eq $n - 1) { $bmp.SetPixel($x, $y, $edge) }
      elseif ($x -le 2 -or $y -le 2 -or $x -ge $n - 3 -or $y -ge $n - 3) { $bmp.SetPixel($x, $y, $frame) }
      else { $bmp.SetPixel($x, $y, $base) }
    }
  }
  foreach ($cx in @(4, ($n - 5))) {
    foreach ($cy in @(4, ($n - 5))) {
      for ($dy = -1; $dy -le 1; $dy++) { for ($dx = -1; $dx -le 1; $dx++) { $bmp.SetPixel($cx + $dx, $cy + $dy, $rivet) } }
    }
  }
}

# 赤石催化器：机壳 + 中央反应腔（品阶色环）+ 四向品阶导线
function DrawCatalyst($bmp, $n, $tier) {
  DrawMachineShell $bmp $n (Color 12 6 6) (Color 90 26 26) (Color 26 14 14) (Color 138 138 138)
  $dark = Color 16 8 8
  $i0 = 9; $i1 = $n - 10
  for ($y = $i0; $y -le $i1; $y++) {
    for ($x = $i0; $x -le $i1; $x++) {
      if ($x -eq $i0 -or $x -eq $i1 -or $y -eq $i0 -or $y -eq $i1) { $bmp.SetPixel($x, $y, $tier) }
      else { $bmp.SetPixel($x, $y, $dark) }
    }
  }
  for ($dy = -1; $dy -le 1; $dy++) { for ($dx = -1; $dx -le 1; $dx++) { $bmp.SetPixel(16 + $dx, 16 + $dy, $tier) } }
  for ($i = 3; $i -le 8; $i++) {
    $bmp.SetPixel($i, 16, $tier)
    $bmp.SetPixel(($n - 1 - $i), 16, $tier)
    $bmp.SetPixel(16, $i, $tier)
    $bmp.SetPixel(16, ($n - 1 - $i), $tier)
  }
}

# 自动收集器：机壳 + 中央收集口（红芯）+ 四向品阶管道
function DrawCollector($bmp, $n, $tier) {
  DrawMachineShell $bmp $n (Color 10 6 6) (Color 64 20 20) (Color 24 16 16) (Color 138 138 138)
  $dark = Color 14 8 8
  $i0 = 8; $i1 = $n - 9
  for ($y = $i0; $y -le $i1; $y++) {
    for ($x = $i0; $x -le $i1; $x++) {
      if ($x -eq $i0 -or $x -eq $i1 -or $y -eq $i0 -or $y -eq $i1) { $bmp.SetPixel($x, $y, $tier) }
      else { $bmp.SetPixel($x, $y, $dark) }
    }
  }
  for ($dy = -3; $dy -le 3; $dy++) { for ($dx = -3; $dx -le 3; $dx++) { $bmp.SetPixel(16 + $dx, 16 + $dy, $oreCore) } }
  for ($dy = -1; $dy -le 1; $dy++) { for ($dx = -1; $dx -le 1; $dx++) { $bmp.SetPixel(16 + $dx, 16 + $dy, $tier) } }
  for ($i = 4; $i -le 9; $i++) {
    $bmp.SetPixel($i, 16, $tier)
    $bmp.SetPixel(($n - 1 - $i), 16, $tier)
    $bmp.SetPixel(16, $i, $tier)
    $bmp.SetPixel(16, ($n - 1 - $i), $tier)
  }
}

# 水晶簇（cross 模型）：透明背景 + 中央尖晶
function DrawSpike($bmp, $n, $cx, $topY, $botY, $halfW, $lit, $shade, $hi, $edge) {
  $span = [double]($botY - $topY)
  for ($y = $topY; $y -le $botY; $y++) {
    # 宽度加速增长并在下半段保持满宽，避免只出现一根细柱
    $w = [Math]::Min($halfW, [Math]::Floor(($y - $topY) / $span * $halfW * 1.8))
    for ($dx = -$w; $dx -le $w; $dx++) {
      $x = $cx + $dx
      if ($x -lt 0 -or $x -ge $n) { continue }
      if ($dx -eq -$w -or $dx -eq $w) { $c = $edge }
      elseif ($dx -lt 0) { $c = $lit }
      elseif ($dx -gt 0) { $c = $shade }
      else { $c = $hi }
      $bmp.SetPixel($x, $y, $c)
    }
  }
}

function DrawCluster($bmp, $n) {
  $lit = Color 204 107 106
  $shade = Color 150 58 58
  $hi = Color 241 123 120
  $edge = Color 110 40 40
  DrawSpike $bmp $n 11 13 25 2 $lit $shade $hi $edge
  DrawSpike $bmp $n 21 11 25 2 $lit $shade $hi $edge
  DrawSpike $bmp $n 16 4 26 3 $lit $shade $hi $edge
  # 底部岩基
  for ($y = 26; $y -le 29; $y++) {
    for ($x = 12; $x -le 20; $x++) {
      if ($x -eq 12 -or $x -eq 20 -or $y -eq 29) { $bmp.SetPixel($x, $y, (Color 64 28 28)) }
      else { $bmp.SetPixel($x, $y, (Color 88 40 40)) }
    }
  }
}

function SaveFace($n, $name, $draw) {
  $bmp = New-Object Drawing.Bitmap($n, $n)
  try {
    & $draw $bmp $n
    $bmp.Save((Join-Path $preview $name), [Drawing.Imaging.ImageFormat]::Png)
    if (!$PreviewOnly) { $bmp.Save((Join-Path $block $name), [Drawing.Imaging.ImageFormat]::Png) }
  } finally { $bmp.Dispose() }
}

# ---- 矿石：4 环境 x 4 浓度 = 16 张 ----
$envs = @(
  @{ name = ''; base = (Color 125 125 125); dark = (Color 107 107 107); light = (Color 138 138 138) },
  @{ name = 'deepslate_'; base = (Color 85 85 88); dark = (Color 68 68 71); light = (Color 100 100 104) },
  @{ name = 'nether_'; base = (Color 110 52 52); dark = (Color 88 40 40); light = (Color 132 66 66) },
  @{ name = 'end_'; base = (Color 219 214 180); dark = (Color 200 196 164); light = (Color 232 228 198) }
)
$tiers = @(
  @{ name = 'low'; blobs = 3; radius = 2; hi = 10 },
  @{ name = 'medium'; blobs = 4; radius = 2; hi = 20 },
  @{ name = 'perfect'; blobs = 5; radius = 3; hi = 30 },
  @{ name = 'flawless'; blobs = 6; radius = 3; hi = 45 }
)
$seed = 17
foreach ($e in $envs) {
  foreach ($t in $tiers) {
    Seed-Rng $seed; $seed = $seed + 97
    SaveFace 32 "$($e.name)akaishi_ore_$($t.name).png" { param($b, $n) DrawOre $b $n $e.base $e.dark $e.light $t.blobs $t.radius $t.hi }
  }
}

# ---- 晶洞：完美度递增，杂质递减、晶面增多、高光增强 ----
Seed-Rng 501
SaveFace 32 'akaishi_geode_flawed.png' { param($b, $n) DrawGeode $b $n (Color 82 54 54) (Color 66 42 42) (Color 98 64 64) 5 (Color 150 58 58) (Color 120 44 44) (Color 181 91 91) (Color 96 36 36) }
Seed-Rng 502
SaveFace 32 'akaishi_geode_normal.png' { param($b, $n) DrawGeode $b $n (Color 100 64 64) (Color 84 52 52) (Color 118 76 76) 6 (Color 181 91 91) (Color 150 58 58) (Color 204 107 106) (Color 120 44 44) }
Seed-Rng 503
SaveFace 32 'akaishi_geode_perfect.png' { param($b, $n) DrawGeode $b $n (Color 118 74 74) (Color 100 62 62) (Color 136 86 86) 7 (Color 204 107 106) (Color 168 80 80) (Color 241 123 120) (Color 150 58 58) }
Seed-Rng 504
SaveFace 32 'akaishi_geode_pristine.png' { param($b, $n) DrawGeode $b $n (Color 136 86 86) (Color 116 72 72) (Color 156 98 98) 8 (Color 224 130 126) (Color 190 100 96) (Color 240 156 144) (Color 168 80 80) }

# ---- 晶体方块与簇 ----
Seed-Rng 601
SaveFace 32 'akaishi_crystal_block.png' { param($b, $n) DrawCrystalBlock $b $n }
SaveFace 32 'akaishi_crystal_cluster.png' { param($b, $n) DrawCluster $b $n }

# ---- 催化剂 / 收集器：品阶色与旧资产一致（灰 / 赤红 / 橙金 / 亮金）----
$basic = Color 150 158 166
$medium = Color 200 50 50
$advanced = Color 208 160 48
$ultimate = Color 255 208 96
Seed-Rng 701
SaveFace 32 'akaishi_catalyst_basic.png' { param($b, $n) DrawCatalyst $b $n $basic }
Seed-Rng 702
SaveFace 32 'akaishi_catalyst_medium.png' { param($b, $n) DrawCatalyst $b $n $medium }
Seed-Rng 703
SaveFace 32 'akaishi_catalyst_advanced.png' { param($b, $n) DrawCatalyst $b $n $advanced }
Seed-Rng 704
SaveFace 32 'akaishi_catalyst_ultimate.png' { param($b, $n) DrawCatalyst $b $n $ultimate }
Seed-Rng 705
SaveFace 32 'akaishi_collector_basic.png' { param($b, $n) DrawCollector $b $n $basic }
Seed-Rng 706
SaveFace 32 'akaishi_collector_medium.png' { param($b, $n) DrawCollector $b $n $medium }
Seed-Rng 707
SaveFace 32 'akaishi_collector_advanced.png' { param($b, $n) DrawCollector $b $n $advanced }
Seed-Rng 708
SaveFace 32 'akaishi_collector_ultimate.png' { param($b, $n) DrawCollector $b $n $ultimate }

if (!$PreviewOnly) {
  & $node (Join-Path $root 'validate_ore_crystal_assets.js') --check-output
  if ($LASTEXITCODE -ne 0) { throw 'Postflight failed' }
}
Write-Output 'Generated ore/crystal textures.'
