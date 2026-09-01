# Generate 16x16 textures: decay purifier blocks + dusts in redstone-dust style
# Pure ASCII content only (PS 5.1 GBK safety)
Add-Type -AssemblyName System.Drawing
$blockDir = Join-Path $PSScriptRoot 'common\src\main\resources\assets\akaishi\textures\block'
$itemDir  = Join-Path $PSScriptRoot 'common\src\main\resources\assets\akaishi\textures\item'
if (-not (Test-Path $blockDir)) { Write-Error 'textures/block dir not found'; exit 1 }

function New-Canvas($plate) {
    $bmp = New-Object System.Drawing.Bitmap(16,16)
    for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) { $bmp.SetPixel($x,$y,$plate) } }
    return $bmp
}
function Draw-Frame($bmp, $dark, $light, $rivet) {
    for ($x=0; $x -lt 16; $x++) { $bmp.SetPixel($x,0,$dark); $bmp.SetPixel($x,15,$dark) }
    for ($y=0; $y -lt 16; $y++) { $bmp.SetPixel(0,$y,$dark); $bmp.SetPixel(15,$y,$dark) }
    for ($x=1; $x -lt 15; $x++) { $bmp.SetPixel($x,1,$light); $bmp.SetPixel($x,14,$dark) }
    for ($y=1; $y -lt 15; $y++) { $bmp.SetPixel(1,$y,$light); $bmp.SetPixel(14,$y,$dark) }
    foreach ($p in @(@(2,2),@(13,2),@(2,13),@(13,13))) {
        $bmp.SetPixel($p[0],$p[1],$rivet); $bmp.SetPixel($p[0]+1,$p[1],$rivet); $bmp.SetPixel($p[0],$p[1]+1,$rivet)
    }
}
function Save-Png($bmp, $dir, $name) {
    $path = Join-Path $dir $name
    $bmp.Save($path,[System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output ("wrote " + $name)
}

# ===== decay purifier: violet-grey plate + toxic green cross =====
$plate = [System.Drawing.Color]::FromArgb(255,42,38,50)
$dark  = [System.Drawing.Color]::FromArgb(255,18,15,24)
$light = [System.Drawing.Color]::FromArgb(255,104,96,128)
$rivet = [System.Drawing.Color]::FromArgb(255,150,138,180)
$gHi = [System.Drawing.Color]::FromArgb(255,120,240,150)
$gMid = [System.Drawing.Color]::FromArgb(255,60,170,90)
$gDark = [System.Drawing.Color]::FromArgb(255,24,84,48)
$bmp = New-Canvas $plate
Draw-Frame $bmp $dark $light $rivet
Save-Png $bmp $blockDir 'akaishi_decay_purifier_bottom.png'
$bmp = New-Canvas $plate
Draw-Frame $bmp $dark $light $rivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,5,$dark); $bmp.SetPixel($x,10,$dark) }
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,6,$light); $bmp.SetPixel($x,11,$light) }
Save-Png $bmp $blockDir 'akaishi_decay_purifier_side.png'
$bmp = New-Canvas $plate
Draw-Frame $bmp $dark $light $rivet
foreach ($p in @(@(7,3),@(7,4),@(7,5),@(7,10),@(7,11),@(7,12),@(4,7),@(5,7),@(6,7),@(9,7),@(10,7),@(11,7),@(7,7))) { $bmp.SetPixel($p[0],$p[1],$gMid) }
foreach ($p in @(@(7,6),@(7,8),@(6,7),@(8,7))) { $bmp.SetPixel($p[0],$p[1],$gHi) }
foreach ($p in @(@(7,2),@(7,13),@(2,7),@(13,7))) { $bmp.SetPixel($p[0],$p[1],$gDark) }
Save-Png $bmp $blockDir 'akaishi_decay_purifier_top.png'

# ===== dusts: redstone-dust particle layout, recolored per material =====
# particle centers: (x,y,ring) spread over the 16x16 field
$particles = @(
    @(2,3),@(4,6),@(7,2),@(9,5),@(12,3),@(13,6),
    @(2,10),@(5,9),@(8,12),@(11,9),@(13,12),
    @(3,13),@(6,6),@(10,13),@(12,9)
)
function New-Dust($hi, $mid, $dcol) {
    $bmp = New-Object System.Drawing.Bitmap(16,16)
    foreach ($p in $particles) {
        $x = $p[0]; $y = $p[1]
        $bmp.SetPixel($x,$y,$mid)
        if ($x -gt 0) { $bmp.SetPixel($x-1,$y,$dcol) }
        if ($y -gt 0) { $bmp.SetPixel($x,$y-1,$dcol) }
        if ($x -lt 15) { $bmp.SetPixel($x+1,$y,$dcol) }
        if ($y -lt 15) { $bmp.SetPixel($x,$y+1,$dcol) }
        $bmp.SetPixel($x,$y,$hi)
    }
    # sprinkle tiny hi-light specks (redstone style)
    foreach ($s in @(@(1,4),@(6,3),@(10,6),@(3,8),@(7,11),@(11,12),@(5,14),@(9,8),@(12,5),@(1,12))) {
        $bmp.SetPixel($s[0],$s[1],$hi)
    }
    return $bmp
}
$dusts = @(
    @('akaishi_dust',255,210,80,80, 255,140,50,50, 255,70,20,20),
    @('coal_dust',   255,160,160,170, 255,90,90,100, 255,40,40,48),
    @('iron_dust',   255,190,170,140, 255,130,110,90, 255,70,58,46),
    @('copper_dust', 255,230,150,90,  255,180,96,44,  255,100,48,20),
    @('gold_dust',   255,250,220,120, 255,220,170,50, 255,130,90,20),
    @('lapis_dust',  255,120,150,230, 255,60,90,190,  255,26,40,110),
    @('diamond_dust',255,140,225,240, 255,70,170,200, 255,26,90,120),
    @('emerald_dust',255,120,230,140, 255,50,180,80,  255,20,100,44),
    @('quartz_dust', 255,245,245,250, 255,200,200,210,255,120,120,132),
    @('netherite_dust',255,140,120,130,255,80,64,74, 255,40,30,38),
    @('obsidian_dust',255,120,104,160, 255,64,52,100, 255,28,22,52)
)
foreach ($d in $dusts) {
    $hi = [System.Drawing.Color]::FromArgb($d[1],$d[2],$d[3],$d[4])
    $mid = [System.Drawing.Color]::FromArgb($d[5],$d[6],$d[7],$d[8])
    $dc = [System.Drawing.Color]::FromArgb($d[9],$d[10],$d[11],$d[12])
    $bmp = New-Dust $hi $mid $dc
    Save-Png $bmp $itemDir ($d[0] + '.png')
}
Write-Output 'all textures generated'
