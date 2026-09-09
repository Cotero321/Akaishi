# Generate generic part mould item texture (16x16, iron frame + redstone core + life green seal)
# ASCII-only content (PS5.1 GBK safety)
Add-Type -AssemblyName System.Drawing

$targetDir = Join-Path $PSScriptRoot 'common\src\main\resources\assets\akaishi\textures\item'
New-Item -ItemType Directory -Path $targetDir -Force -ErrorAction SilentlyContinue | Out-Null

$bmp = New-Object System.Drawing.Bitmap(16, 16)

function SetPx {
    param($x, $y, $a, $r, $g, $b)
    $c = [System.Drawing.Color]::FromArgb($a, $r, $g, $b)
    $bmp.SetPixel($x, $y, $c)
}

function Rect {
    param($x1, $y1, $x2, $y2, $a, $r, $g, $b)
    for ($x = $x1; $x -le $x2; $x++) { for ($y = $y1; $y -le $y2; $y++) { SetPx $x $y $a $r $g $b } }
}

# transparent canvas by default; draw plate outline (2..13)
Rect 3 3 12 12 255 150 150 155
# bevel: top-left light, bottom-right dark
Rect 3 3 12 3 255 200 200 205
Rect 3 3 3 12 255 200 200 205
Rect 3 12 12 12 255 90 90 95
Rect 12 3 12 12 255 90 90 95
# inner cavity (mould depression)
Rect 5 5 10 10 255 70 70 75
# redstone core line (center horizontal)
Rect 6 7 9 8 255 190 50 40
# life green seal dots (corners of cavity)
SetPx 5 5 255 60 200 80
SetPx 10 5 255 60 200 80
SetPx 5 10 255 60 200 80
SetPx 10 10 255 60 200 80
# corner rivets
SetPx 4 4 255 220 220 225
SetPx 11 4 255 220 220 225
SetPx 4 11 255 220 220 225
SetPx 11 11 255 220 220 225

$bmp.Save((Join-Path $targetDir 'akaishi_generic_part_mould.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "Created akaishi_generic_part_mould.png"