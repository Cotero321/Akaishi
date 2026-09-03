# Generate life fusion anvil textures + recolored life fusion armor textures
# Pure ASCII content only (PS 5.1 GBK safety)
Add-Type -AssemblyName System.Drawing
$root = $PSScriptRoot
$blockDir  = Join-Path $root 'common\src\main\resources\assets\akaishi\textures\block'
$itemDir   = Join-Path $root 'common\src\main\resources\assets\akaishi\textures\item'
$armorDir  = Join-Path $root 'common\src\main\resources\assets\akaishi\textures\models\armor'

function Save-Png($bmp, $dir, $name) {
    $path = Join-Path $dir $name
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output ("wrote " + $name)
}

# Blend every pixel toward a life-green tint, preserving alpha (keeps armor UV shape)
function Tint-Copy($srcPath, $dir, $outName, $tr, $tg, $tb, $mix) {
    $bmp = New-Object System.Drawing.Bitmap($srcPath)
    $inv = 1.0 - $mix
    for ($y = 0; $y -lt $bmp.Height; $y++) {
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            $p = $bmp.GetPixel($x, $y)
            $r = [int](($p.R * $inv) + ($tr * $mix))
            $g = [int](($p.G * $inv) + ($tg * $mix))
            $b = [int](($p.B * $inv) + ($tb * $mix))
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($p.A, $r, $g, $b))
        }
    }
    Save-Png $bmp $dir $outName
}

# ===== life fusion armor item textures (recolor akaishi armor) =====
foreach ($n in @('helmet', 'chestplate', 'leggings', 'boots')) {
    $src = Join-Path $itemDir ('akaishi_' + $n + '.png')
    if (Test-Path $src) {
        Tint-Copy $src $itemDir ('akaishi_life_fusion_' + $n + '.png') 60 190 150 0.55
    } else {
        Write-Output ('missing ' + $src)
    }
}

# ===== life fusion armor layer textures (worn on body, keep 64x32 UV layout) =====
foreach ($n in @(1, 2)) {
    $src = Join-Path $armorDir ('akaishi_layer_' + $n + '.png')
    if (Test-Path $src) {
        Tint-Copy $src $armorDir ('life_fusion_layer_' + $n + '.png') 60 190 150 0.55
    } else {
        Write-Output ('missing ' + $src)
    }
}

# ===== life fusion anvil block textures (recolored anvil-like metal) =====
$plate  = [System.Drawing.Color]::FromArgb(255, 70, 82, 78)
$dark   = [System.Drawing.Color]::FromArgb(255, 34, 42, 40)
$light  = [System.Drawing.Color]::FromArgb(255, 118, 140, 132)
$accent = [System.Drawing.Color]::FromArgb(255, 70, 200, 150)
$acDark = [System.Drawing.Color]::FromArgb(255, 28, 96, 70)

function New-Plate() {
    $bmp = New-Object System.Drawing.Bitmap(16, 16)
    for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { $bmp.SetPixel($x, $y, $plate) } }
    return $bmp
}
function Draw-Frame($bmp) {
    for ($x = 0; $x -lt 16; $x++) { $bmp.SetPixel($x, 0, $dark); $bmp.SetPixel($x, 15, $dark) }
    for ($y = 0; $y -lt 16; $y++) { $bmp.SetPixel(0, $y, $dark); $bmp.SetPixel(15, $y, $dark) }
    for ($x = 1; $x -lt 15; $x++) { $bmp.SetPixel($x, 1, $light); $bmp.SetPixel($x, 14, $dark) }
    for ($y = 1; $y -lt 15; $y++) { $bmp.SetPixel(1, $y, $light); $bmp.SetPixel(14, $y, $dark) }
}

# top: anvil-style face with a life emblem
$bmp = New-Plate
Draw-Frame $bmp
for ($x = 3; $x -lt 13; $x++) { $bmp.SetPixel($x, 3, $light); $bmp.SetPixel($x, 12, $dark) }
for ($y = 3; $y -lt 13; $y++) { $bmp.SetPixel(3, $y, $light); $bmp.SetPixel(12, $y, $dark) }
foreach ($p in @(@(7,5),@(7,6),@(7,7),@(7,8),@(7,9),@(5,7),@(6,7),@(8,7),@(9,7))) { $bmp.SetPixel($p[0], $p[1], $accent) }
foreach ($p in @(@(7,4),@(7,10),@(4,7),@(10,7))) { $bmp.SetPixel($p[0], $p[1], $acDark) }
Save-Png $bmp $blockDir 'akaishi_life_fusion_anvil_top.png'

# side: anvil body with a horizontal seam
$bmp = New-Plate
Draw-Frame $bmp
for ($x = 2; $x -lt 14; $x++) { $bmp.SetPixel($x, 5, $dark); $bmp.SetPixel($x, 6, $light) }
for ($x = 2; $x -lt 14; $x++) { $bmp.SetPixel($x, 10, $dark); $bmp.SetPixel($x, 11, $light) }
Save-Png $bmp $blockDir 'akaishi_life_fusion_anvil_side.png'

# bottom: plain dark metal
$bmp = New-Plate
Draw-Frame $bmp
Save-Png $bmp $blockDir 'akaishi_life_fusion_anvil_bottom.png'

Write-Output 'life fusion textures generated'
