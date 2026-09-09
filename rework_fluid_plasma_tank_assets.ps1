param([switch]$PreviewOnly)
# 统一流体罐/等离子罐家族的三面材质：侧面已重制(32/64)，此处补齐顶/底/注料机，
# 使同一 cube_bottom_top 方块的六面分辨率与工业机壳风格一致。
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.Drawing
$root = $PSScriptRoot
$block = Join-Path $root 'common\src\main\resources\assets\akaishi\textures\block'
$preview = Join-Path $root 'gui_layouts'
$node = (Get-Command node -ErrorAction Stop).Source
& $node (Join-Path $root 'validate_fluid_plasma_tank_assets.js')
if ($LASTEXITCODE -ne 0) { throw 'Preflight failed' }

function Color($red, $green, $blue, $alpha = 255) {
  [Drawing.Color]::FromArgb($alpha, [Math]::Max(0, [Math]::Min(255, $red)), [Math]::Max(0, [Math]::Min(255, $green)), [Math]::Max(0, [Math]::Min(255, $blue)))
}

# 与侧面取样一致的机壳配色
$edge = Color 34 41 46
$frame = Color 110 125 132
$base = Color 65 75 82
$dark = Color 48 56 62
$light = Color 150 166 172
$basic = Color 61 183 160
$advanced = Color 141 114 232
$super = Color 240 184 77
$plasma = Color 224 100 255
$blue = Color 120 170 255
$orange = Color 255 140 70
$violet = Color 190 130 255

function DrawCasing($bmp, $n) {
  for ($y = 0; $y -lt $n; $y++) {
    for ($x = 0; $x -lt $n; $x++) {
      if ($x -eq 0 -or $y -eq 0 -or $x -eq $n - 1 -or $y -eq $n - 1) { $bmp.SetPixel($x, $y, $edge) } elseif ($x -le 2 -or $y -le 2 -or $x -ge $n - 3 -or $y -ge $n - 3) { $bmp.SetPixel($x, $y, $frame) } else { $bmp.SetPixel($x, $y, $base) }
    }
  }
}

function DrawRivets($bmp, $n) {
  $r = [int]($n / 8)
  foreach ($cx in @($r, ($n - 1 - $r))) {
    foreach ($cy in @($r, ($n - 1 - $r))) {
      for ($dy = -1; $dy -le 1; $dy++) { for ($dx = -1; $dx -le 1; $dx++) { $bmp.SetPixel($cx + $dx, $cy + $dy, $light) } }
    }
  }
}

# 顶面：带加强环与中心接口的罐盖
function DrawTop($bmp, $n, $accent) {
  DrawCasing $bmp $n
  DrawRivets $bmp $n
  $i0 = [int]($n * 0.25); $i1 = $n - 1 - $i0
  for ($y = $i0; $y -le $i1; $y++) {
    for ($x = $i0; $x -le $i1; $x++) {
      if ($x -eq $i0 -or $x -eq $i1 -or $y -eq $i0 -or $y -eq $i1) { $bmp.SetPixel($x, $y, $accent) } else { $bmp.SetPixel($x, $y, $dark) }
    }
  }
  $c0 = [int]($n * 0.4375); $c1 = $n - 1 - $c0
  for ($y = $c0; $y -le $c1; $y++) { for ($x = $c0; $x -le $c1; $x++) { $bmp.SetPixel($x, $y, $accent) } }
}

# 底面：机座底板 + 中央排放口
function DrawBottom($bmp, $n) {
  DrawCasing $bmp $n
  $p0 = [int]($n * 0.1875); $p1 = $n - 1 - $p0
  for ($y = $p0; $y -le $p1; $y++) { for ($x = $p0; $x -le $p1; $x++) { $bmp.SetPixel($x, $y, $dark) } }
  $c0 = [int]($n * 0.375); $c1 = $n - 1 - $c0
  for ($y = $c0; $y -le $c1; $y++) { for ($x = $c0; $x -le $c1; $x++) { $bmp.SetPixel($x, $y, $edge) } }
  $d0 = [int]($n * 0.46875); $d1 = $n - 1 - $d0
  for ($y = $d0; $y -le $d1; $y++) { for ($x = $d0; $x -le $d1; $x++) { $bmp.SetPixel($x, $y, $light) } }
}

# 注料机侧面：三色等离子观察窗
function DrawFillerSide($bmp, $n) {
  DrawCasing $bmp $n
  $w0 = [int]($n * 0.1875); $w1 = $n - 1 - $w0
  for ($y = $w0; $y -le $w1; $y++) { for ($x = $w0; $x -le $w1; $x++) { $bmp.SetPixel($x, $y, $dark) } }
  $s0 = $w0 + 2; $s1 = $w1 - 2
  # 三色等宽：用 Floor 而非 [int]（PS 的 [int] 是四舍五入，会导致条宽不均）
  $step = ($s1 - $s0 + 1) / 3.0
  for ($y = $s0; $y -le $s1; $y++) {
    for ($x = $s0; $x -le $s1; $x++) {
      $idx = [Math]::Min(2, [Math]::Floor(($x - $s0) / $step))
      $col = @($blue, $orange, $violet)[$idx]
      $bmp.SetPixel($x, $y, $col)
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

# 流体罐：三阶顶面共用同一底板，顶面按品阶着色
SaveFace 32 'akaishi_fluid_tank_bottom.png' { param($b, $n) DrawBottom $b $n }
SaveFace 32 'akaishi_fluid_tank_basic_top.png' { param($b, $n) DrawTop $b $n $basic }
SaveFace 32 'akaishi_fluid_tank_advanced_top.png' { param($b, $n) DrawTop $b $n $advanced }
SaveFace 32 'akaishi_fluid_tank_super_top.png' { param($b, $n) DrawTop $b $n $super }
# 等离子罐：与 64×64 侧面同尺寸
SaveFace 64 'akaishi_plasma_tank_top.png' { param($b, $n) DrawTop $b $n $plasma }
SaveFace 64 'akaishi_plasma_tank_bottom.png' { param($b, $n) DrawBottom $b $n }
# 等离子注料机：三色观察窗侧面 + 品红顶盖 + 机座
SaveFace 64 'akaishi_plasma_filler_side.png' { param($b, $n) DrawFillerSide $b $n }
SaveFace 64 'akaishi_plasma_filler_top.png' { param($b, $n) DrawTop $b $n $plasma }
SaveFace 64 'akaishi_plasma_filler_bottom.png' { param($b, $n) DrawBottom $b $n }

if (!$PreviewOnly) {
  & $node (Join-Path $root 'validate_fluid_plasma_tank_assets.js') --check-output
  if ($LASTEXITCODE -ne 0) { throw 'Postflight failed' }
}
Write-Output 'Generated fluid/plasma tank textures.'
