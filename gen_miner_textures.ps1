# Generate 16x16 textures for the chishi miner multiblock set
# Pure ASCII content only (PS 5.1 GBK safety)
Add-Type -AssemblyName System.Drawing
$dir = Join-Path $PSScriptRoot 'common\src\main\resources\assets\template_mod\textures\block'
if (-not (Test-Path $dir)) { Write-Error 'textures/block dir not found'; exit 1 }
$itemDir = Split-Path $dir -Parent | Join-Path -ChildPath 'item'
if (-not (Test-Path $itemDir)) { New-Item -ItemType Directory -Path $itemDir | Out-Null }

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
        $bmp.SetPixel($p[0],$p[1],$rivet)
        $bmp.SetPixel($p[0]+1,$p[1],$rivet); $bmp.SetPixel($p[0],$p[1]+1,$rivet)
    }
}

function Draw-Crystal($bmp, $cx, $cy, $glow, $glowCol, $hi, $mid, $darkC) {
    if ($glow) {
        for ($y=$cy-3; $y -le $cy+3; $y++) { for ($x=$cx-3; $x -le $cx+3; $x++) {
            if ($x -ge 1 -and $x -lt 15 -and $y -ge 1 -and $y -lt 15) { $bmp.SetPixel($x,$y,$glowCol) }
        } }
    }
    $bmp.SetPixel($cx,$cy,$hi)
    $bmp.SetPixel($cx+1,$cy,$mid);  $bmp.SetPixel($cx-1,$cy,$mid)
    $bmp.SetPixel($cx,$cy+1,$mid);  $bmp.SetPixel($cx,$cy-1,$mid)
    $bmp.SetPixel($cx+2,$cy,$mid); $bmp.SetPixel($cx-2,$cy,$mid)
    $bmp.SetPixel($cx,$cy+2,$mid); $bmp.SetPixel($cx,$cy-2,$mid)
    foreach ($p in @(@(1,1),@(-1,1),@(1,-1),@(-1,-1))) {
        $bmp.SetPixel($cx+$p[0],$cy+$p[1],$mid)
    }
    $bmp.SetPixel($cx+2,$cy+2,$darkC); $bmp.SetPixel($cx-2,$cy+2,$darkC)
    $bmp.SetPixel($cx+2,$cy-2,$darkC); $bmp.SetPixel($cx-2,$cy-2,$darkC)
}

function Draw-Star($bmp, $cx, $cy, $glow, $glowCol, $hi, $mid, $darkC) {
    if ($glow) {
        for ($y=$cy-3; $y -le $cy+3; $y++) { for ($x=$cx-3; $x -le $cx+3; $x++) {
            if ($x -ge 1 -and $x -lt 15 -and $y -ge 1 -and $y -lt 15 -and ([Math]::Abs($x-$cx) -le 3 -and [Math]::Abs($y-$cy) -le 3)) { $bmp.SetPixel($x,$y,$glowCol) }
        } }
    }
    foreach ($p in @(@(0,3),@(0,-3),@(3,0),@(-3,0))) { $bmp.SetPixel($cx+$p[0],$cy+$p[1],$hi) }
    foreach ($p in @(@(0,2),@(0,-2),@(2,0),@(-2,0),@(2,2),@(-2,2),@(2,-2),@(-2,-2))) { $bmp.SetPixel($cx+$p[0],$cy+$p[1],$mid) }
    foreach ($p in @(@(1,0),@(-1,0),@(0,1),@(0,-1),@(1,1),@(-1,1),@(1,-1),@(-1,-1))) { $bmp.SetPixel($cx+$p[0],$cy+$p[1],$mid) }
    $bmp.SetPixel($cx,$cy,$hi)
}

function Save-Png($bmp, $name) {
    $path = Join-Path $dir $name
    $bmp.Save($path,[System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output ("wrote " + $name)
}

function Save-ItemPng($bmp, $name) {
    $path = Join-Path $itemDir $name
    $bmp.Save($path,[System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output ("wrote item " + $name)
}

# ===== shared steel plate palette (framework) =====
$steelPlate = [System.Drawing.Color]::FromArgb(255,38,42,52)
$steelDark  = [System.Drawing.Color]::FromArgb(255,16,18,26)
$steelLight = [System.Drawing.Color]::FromArgb(255,84,92,116)
$steelRivet = [System.Drawing.Color]::FromArgb(255,120,130,160)

# ===== framework block (plain steel) =====
# bottom: plain plate
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
Save-Png $bmp 'chishi_miner_frame_bottom.png'
# side: plate + two horizontal seams
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,5,$steelDark); $bmp.SetPixel($x,10,$steelDark) }
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,6,$steelLight); $bmp.SetPixel($x,11,$steelLight) }
Save-Png $bmp 'chishi_miner_frame_side.png'
# top: plate + cross grid
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,7,$steelDark); $bmp.SetPixel(7,$x,$steelDark) }
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,8,$steelLight); $bmp.SetPixel(8,$x,$steelLight) }
Save-Png $bmp 'chishi_miner_frame_top.png'

# ===== upgrade frame (steel + blue plus sign on top) =====
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
Save-Png $bmp 'chishi_miner_upgrade_frame_bottom.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,5,$steelDark); $bmp.SetPixel($x,10,$steelDark) }
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,6,$steelLight); $bmp.SetPixel($x,11,$steelLight) }
Save-Png $bmp 'chishi_miner_upgrade_frame_side.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
$uHi = [System.Drawing.Color]::FromArgb(255,150,220,255)
$uMid = [System.Drawing.Color]::FromArgb(255,70,160,220)
foreach ($p in @(@(7,4),@(7,5),@(7,6),@(7,9),@(7,10),@(7,11),@(4,7),@(5,7),@(6,7),@(9,7),@(10,7),@(11,7))) { $bmp.SetPixel($p[0],$p[1],$uMid) }
foreach ($p in @(@(7,7),@(7,8),@(8,7),@(6,7))) { $bmp.SetPixel($p[0],$p[1],$uHi) }
Save-Png $bmp 'chishi_miner_upgrade_frame_top.png'

# ===== port (steel + gold ring outlet on top) =====
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,5,$steelDark); $bmp.SetPixel($x,10,$steelDark) }
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,6,$steelLight); $bmp.SetPixel($x,11,$steelLight) }
for ($x=3; $x -lt 13; $x++) { $bmp.SetPixel($x,14,$steelDark) }
Save-Png $bmp 'chishi_miner_port_side.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
for ($x=3; $x -lt 13; $x++) { $bmp.SetPixel($x,14,$steelDark) }
Save-Png $bmp 'chishi_miner_port_bottom.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
$rHi = [System.Drawing.Color]::FromArgb(255,255,220,120)
$rMid = [System.Drawing.Color]::FromArgb(255,224,130,40)
$rDark = [System.Drawing.Color]::FromArgb(255,60,40,16)
foreach ($p in @(@(5,5),@(10,5),@(5,10),@(10,10),@(7,7),@(8,8),@(8,7),@(7,8))) { $bmp.SetPixel($p[0],$p[1],$rMid) }
foreach ($p in @(@(6,5),@(9,5),@(5,6),@(10,6),@(5,9),@(10,9),@(6,10),@(9,10),@(5,7),@(10,7),@(7,5),@(8,5),@(7,10),@(8,10),@(5,8),@(10,8))) { $bmp.SetPixel($p[0],$p[1],$rHi) }
foreach ($p in @(@(6,6),@(9,6),@(6,9),@(9,9),@(7,6),@(8,6),@(6,7),@(6,8),@(9,7),@(9,8),@(7,9),@(8,9))) { $bmp.SetPixel($p[0],$p[1],$rDark) }
Save-Png $bmp 'chishi_miner_port_top.png'

# ===== controller basic: steel + blue crystal core =====
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,14,$steelDark) }
Save-Png $bmp 'chishi_miner_controller_basic_bottom.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,5,$steelDark); $bmp.SetPixel($x,10,$steelDark) }
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,6,$steelLight); $bmp.SetPixel($x,11,$steelLight) }
Save-Png $bmp 'chishi_miner_controller_basic_side.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
$bHi = [System.Drawing.Color]::FromArgb(255,150,220,255)
$bMid = [System.Drawing.Color]::FromArgb(255,70,160,220)
$bDark = [System.Drawing.Color]::FromArgb(255,26,66,104)
Draw-Crystal $bmp 8 8 $true $bHi $bHi $bMid $bDark
Save-Png $bmp 'chishi_miner_controller_basic_top.png'

# ===== controller advanced: steel + gold-red star core =====
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,14,$steelDark) }
Save-Png $bmp 'chishi_miner_controller_advanced_bottom.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,5,$steelDark); $bmp.SetPixel($x,10,$steelDark) }
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,6,$steelLight); $bmp.SetPixel($x,11,$steelLight) }
Save-Png $bmp 'chishi_miner_controller_advanced_side.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
$aHi = [System.Drawing.Color]::FromArgb(255,255,220,120)
$aMid = [System.Drawing.Color]::FromArgb(255,224,130,40)
$aDark = [System.Drawing.Color]::FromArgb(255,110,58,30)
Draw-Star $bmp 8 8 $true $aHi $aHi $aMid $aDark
Save-Png $bmp 'chishi_miner_controller_advanced_top.png'

# ===== controller super: deep violet plate + purple diamond core =====
$vPlate = [System.Drawing.Color]::FromArgb(255,40,34,56)
$vDark  = [System.Drawing.Color]::FromArgb(255,18,14,30)
$vLight = [System.Drawing.Color]::FromArgb(255,100,88,140)
$vRivet = [System.Drawing.Color]::FromArgb(255,140,120,190)
$bmp = New-Canvas $vPlate
Draw-Frame $bmp $vDark $vLight $vRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,14,$vDark) }
Save-Png $bmp 'chishi_miner_controller_super_bottom.png'
$bmp = New-Canvas $vPlate
Draw-Frame $bmp $vDark $vLight $vRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,5,$vDark); $bmp.SetPixel($x,10,$vDark) }
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,6,$vLight); $bmp.SetPixel($x,11,$vLight) }
Save-Png $bmp 'chishi_miner_controller_super_side.png'
$bmp = New-Canvas $vPlate
Draw-Frame $bmp $vDark $vLight $vRivet
$sHi = [System.Drawing.Color]::FromArgb(255,220,170,255)
$sMid = [System.Drawing.Color]::FromArgb(255,150,90,220)
$sDark = [System.Drawing.Color]::FromArgb(255,70,36,120)
Draw-Crystal $bmp 8 8 $true $sHi $sHi $sMid $sDark
$bmp.SetPixel(8,8,$sHi); $bmp.SetPixel(7,7,$sHi); $bmp.SetPixel(9,9,$sHi); $bmp.SetPixel(7,9,$sHi); $bmp.SetPixel(9,7,$sHi)
Save-Png $bmp 'chishi_miner_controller_super_top.png'

# ===== controller ultimate: obsidian plate + gold layered star =====
$uPlate = [System.Drawing.Color]::FromArgb(255,22,20,24)
$uDark  = [System.Drawing.Color]::FromArgb(255,8,8,10)
$uLight = [System.Drawing.Color]::FromArgb(255,70,66,84)
$uRivet = [System.Drawing.Color]::FromArgb(255,150,140,120)
$bmp = New-Canvas $uPlate
Draw-Frame $bmp $uDark $uLight $uRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,14,$uDark) }
Save-Png $bmp 'chishi_miner_controller_ultimate_bottom.png'
$bmp = New-Canvas $uPlate
Draw-Frame $bmp $uDark $uLight $uRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,5,$uDark); $bmp.SetPixel($x,10,$uDark) }
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,6,$uLight); $bmp.SetPixel($x,11,$uLight) }
Save-Png $bmp 'chishi_miner_controller_ultimate_side.png'
$bmp = New-Canvas $uPlate
Draw-Frame $bmp $uDark $uLight $uRivet
$tHi = [System.Drawing.Color]::FromArgb(255,255,225,140)
$tMid = [System.Drawing.Color]::FromArgb(255,224,170,60)
$tDark = [System.Drawing.Color]::FromArgb(255,120,84,30)
Draw-Star $bmp 8 8 $true $tHi $tHi $tMid $tDark
foreach ($p in @(@(0,1),@(0,-1),@(1,0),@(-1,0))) { $bmp.SetPixel(8+$p[0],8+$p[1],$tHi) }
Save-Png $bmp 'chishi_miner_controller_ultimate_top.png'

# ===== upgrade item icons (16x16) =====
# speed: blue up arrow
$bmp = New-Object System.Drawing.Bitmap(16,16)
$tHi = [System.Drawing.Color]::FromArgb(255,150,220,255)
$tMid = [System.Drawing.Color]::FromArgb(255,70,160,220)
$tDark = [System.Drawing.Color]::FromArgb(255,26,66,104)
foreach ($p in @(@(7,3),@(8,3),@(7,4),@(8,4),@(6,5),@(9,5),@(5,6),@(10,6),@(4,7),@(11,7))) { $bmp.SetPixel($p[0],$p[1],$tHi) }
foreach ($p in @(@(7,6),@(8,6),@(7,7),@(8,7),@(7,8),@(8,8),@(7,9),@(8,9),@(7,10),@(8,10),@(7,11),@(8,11),@(7,12),@(8,12))) { $bmp.SetPixel($p[0],$p[1],$tMid) }
Save-ItemPng $bmp 'chishi_miner_speed_upgrade.png'

# fortune: green clover
$bmp = New-Object System.Drawing.Bitmap(16,16)
$fHi = [System.Drawing.Color]::FromArgb(255,170,255,170)
$fMid = [System.Drawing.Color]::FromArgb(255,80,200,90)
$fDark = [System.Drawing.Color]::FromArgb(255,30,110,44)
foreach ($p in @(@(7,2),@(8,2),@(6,3),@(9,3),@(7,3),@(8,3),@(5,4),@(10,4),@(7,4),@(8,4),@(4,5),@(5,5),@(6,5),@(7,5),@(8,5),@(9,5),@(10,5),@(11,5),@(4,6),@(5,6),@(10,6),@(11,6))) { $bmp.SetPixel($p[0],$p[1],$fMid) }
foreach ($p in @(@(7,6),@(8,6),@(6,4),@(9,4),@(7,5),@(8,5),@(6,6),@(9,6),@(7,7),@(8,7))) { $bmp.SetPixel($p[0],$p[1],$fHi) }
foreach ($p in @(@(7,8),@(8,8),@(7,9),@(8,9),@(7,10),@(8,10),@(6,11),@(9,11),@(7,12),@(8,12))) { $bmp.SetPixel($p[0],$p[1],$fDark) }
Save-ItemPng $bmp 'chishi_miner_fortune_upgrade.png'

# storage: yellow battery
$bmp = New-Object System.Drawing.Bitmap(16,16)
$gHi = [System.Drawing.Color]::FromArgb(255,255,235,150)
$gMid = [System.Drawing.Color]::FromArgb(255,235,190,60)
$gDark = [System.Drawing.Color]::FromArgb(255,120,90,24)
foreach ($p in @(@(6,3),@(7,3),@(8,3),@(9,3),@(6,12),@(7,12),@(8,12),@(9,12),@(6,3),@(6,4),@(9,3),@(9,4),@(6,11),@(6,12),@(9,11),@(9,12))) { $bmp.SetPixel($p[0],$p[1],$gDark) }
foreach ($p in @(@(7,2),@(8,2))) { $bmp.SetPixel($p[0],$p[1],$gHi) }
for ($y=4; $y -lt 12; $y++) { for ($x=7; $x -lt 9; $x++) { $bmp.SetPixel($x,$y,$gMid) } }
foreach ($p in @(@(7,5),@(8,5),@(7,9),@(8,9))) { $bmp.SetPixel($p[0],$p[1],$gHi) }
Save-ItemPng $bmp 'chishi_miner_storage_upgrade.png'

Write-Output 'all miner textures generated'
