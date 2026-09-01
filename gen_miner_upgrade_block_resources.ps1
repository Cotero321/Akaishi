# Generate JSON + textures for miner upgrade module blocks (speed/fortune/storage)
# Pure ASCII content only (PS 5.1 GBK safety)
Add-Type -AssemblyName System.Drawing
$root = Join-Path $PSScriptRoot 'common\src\main\resources'
$blockstates = Join-Path $root 'assets\template_mod\blockstates'
$modelsBlock = Join-Path $root 'assets\template_mod\models\block'
$modelsItem  = Join-Path $root 'assets\template_mod\models\item'
$loot        = Join-Path $root 'data\template_mod\loot_tables\blocks'
$texDir      = Join-Path $root 'assets\template_mod\textures\block'
$utf8NoBom   = New-Object System.Text.UTF8Encoding $false

function Write-Utf8([string]$path, [string]$content) {
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
}

$blocks = @(
    'chishi_miner_speed_upgrade_block',
    'chishi_miner_fortune_upgrade_block',
    'chishi_miner_storage_upgrade_block'
)

foreach ($b in $blocks) {
    $bs = "{`n  `"variants`": {`n    `"`": {`n      `"model`": `"template_mod:block/$b`"`n    }`n  }`n}"
    Write-Utf8 (Join-Path $blockstates ($b + '.json')) $bs
    $bm = "{`n  `"parent`": `"minecraft:block/cube_bottom_top`",`n  `"textures`": {`n    `"bottom`": `"template_mod:block/${b}_bottom`",`n    `"top`": `"template_mod:block/${b}_top`",`n    `"side`": `"template_mod:block/${b}_side`"`n  }`n}"
    Write-Utf8 (Join-Path $modelsBlock ($b + '.json')) $bm
    $im = "{`n  `"parent`": `"template_mod:block/$b`"`n}"
    Write-Utf8 (Join-Path $modelsItem ($b + '.json')) $im
    $lt = "{`n  `"type`": `"minecraft:block`",`n  `"pools`": [`n    {`n      `"bonus_rolls`": 0.0,`n      `"conditions`": [`n        { `"condition`": `"minecraft:survives_explosion`" }`n      ],`n      `"entries`": [`n        {`n          `"type`": `"minecraft:item`",`n          `"name`": `"template_mod:$b`"`n        }`n      ],`n      `"rolls`": 1.0`n    }`n  ]`n}"
    Write-Utf8 (Join-Path $loot ($b + '.json')) $lt
}

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

function Save-Png($bmp, $name) {
    $path = Join-Path $texDir $name
    $bmp.Save($path,[System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output ("wrote " + $name)
}

# shared steel plate palette (same as upgrade frame)
$steelPlate = [System.Drawing.Color]::FromArgb(255,38,42,52)
$steelDark  = [System.Drawing.Color]::FromArgb(255,16,18,26)
$steelLight = [System.Drawing.Color]::FromArgb(255,84,92,116)
$steelRivet = [System.Drawing.Color]::FromArgb(255,120,130,160)

# ===== speed module: blue up-arrow on top =====
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
Save-Png $bmp 'chishi_miner_speed_upgrade_block_bottom.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,5,$steelDark); $bmp.SetPixel($x,10,$steelDark) }
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,6,$steelLight); $bmp.SetPixel($x,11,$steelLight) }
Save-Png $bmp 'chishi_miner_speed_upgrade_block_side.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
$sHi = [System.Drawing.Color]::FromArgb(255,150,220,255)
$sMid = [System.Drawing.Color]::FromArgb(255,70,160,220)
$sDark = [System.Drawing.Color]::FromArgb(255,26,66,104)
foreach ($p in @(@(7,3),@(8,3),@(7,4),@(8,4),@(6,5),@(9,5),@(5,6),@(10,6),@(4,7),@(11,7))) { $bmp.SetPixel($p[0],$p[1],$sHi) }
foreach ($p in @(@(7,6),@(8,6),@(7,7),@(8,7),@(7,8),@(8,8),@(7,9),@(8,9),@(7,10),@(8,10),@(7,11),@(8,11),@(7,12),@(8,12))) { $bmp.SetPixel($p[0],$p[1],$sMid) }
Save-Png $bmp 'chishi_miner_speed_upgrade_block_top.png'

# ===== fortune module: green clover on top =====
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
Save-Png $bmp 'chishi_miner_fortune_upgrade_block_bottom.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,5,$steelDark); $bmp.SetPixel($x,10,$steelDark) }
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,6,$steelLight); $bmp.SetPixel($x,11,$steelLight) }
Save-Png $bmp 'chishi_miner_fortune_upgrade_block_side.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
$fHi = [System.Drawing.Color]::FromArgb(255,170,255,170)
$fMid = [System.Drawing.Color]::FromArgb(255,80,200,90)
$fDark = [System.Drawing.Color]::FromArgb(255,30,110,44)
foreach ($p in @(@(7,2),@(8,2),@(6,3),@(9,3),@(7,3),@(8,3),@(5,4),@(10,4),@(7,4),@(8,4),@(4,5),@(5,5),@(6,5),@(7,5),@(8,5),@(9,5),@(10,5),@(11,5),@(4,6),@(5,6),@(10,6),@(11,6))) { $bmp.SetPixel($p[0],$p[1],$fMid) }
foreach ($p in @(@(7,6),@(8,6),@(6,4),@(9,4),@(7,5),@(8,5),@(6,6),@(9,6),@(7,7),@(8,7))) { $bmp.SetPixel($p[0],$p[1],$fHi) }
foreach ($p in @(@(7,8),@(8,8),@(7,9),@(8,9),@(7,10),@(8,10),@(6,11),@(9,11),@(7,12),@(8,12))) { $bmp.SetPixel($p[0],$p[1],$fDark) }
Save-Png $bmp 'chishi_miner_fortune_upgrade_block_top.png'

# ===== storage module: yellow battery on top =====
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
Save-Png $bmp 'chishi_miner_storage_upgrade_block_bottom.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,5,$steelDark); $bmp.SetPixel($x,10,$steelDark) }
for ($x=2; $x -lt 14; $x++) { $bmp.SetPixel($x,6,$steelLight); $bmp.SetPixel($x,11,$steelLight) }
Save-Png $bmp 'chishi_miner_storage_upgrade_block_side.png'
$bmp = New-Canvas $steelPlate
Draw-Frame $bmp $steelDark $steelLight $steelRivet
$gHi = [System.Drawing.Color]::FromArgb(255,255,235,150)
$gMid = [System.Drawing.Color]::FromArgb(255,235,190,60)
$gDark = [System.Drawing.Color]::FromArgb(255,120,90,24)
foreach ($p in @(@(6,3),@(7,3),@(8,3),@(9,3),@(6,12),@(7,12),@(8,12),@(9,12),@(6,3),@(6,4),@(9,3),@(9,4),@(6,11),@(6,12),@(9,11),@(9,12))) { $bmp.SetPixel($p[0],$p[1],$gDark) }
foreach ($p in @(@(7,2),@(8,2))) { $bmp.SetPixel($p[0],$p[1],$gHi) }
for ($y=4; $y -lt 12; $y++) { for ($x=7; $x -lt 9; $x++) { $bmp.SetPixel($x,$y,$gMid) } }
foreach ($p in @(@(7,5),@(8,5),@(7,9),@(8,9))) { $bmp.SetPixel($p[0],$p[1],$gHi) }
Save-Png $bmp 'chishi_miner_storage_upgrade_block_top.png'

Write-Output 'miner upgrade block resources generated'
