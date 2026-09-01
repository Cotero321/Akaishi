Add-Type -AssemblyName System.Drawing
$base = Join-Path $PSScriptRoot "common\src\main\resources\assets\template_mod\textures"
$itemDir = "$base\item"
$blockDir = "$base\block"
$fluidDir = "$base\block\fluid"

function Save($bmp, [string]$path) {
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function SetRect($bmp, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [int]$r, [int]$gg, [int]$b2) {
    for ($x = $x0; $x -le $x1; $x++) {
        for ($y = $y0; $y -le $y1; $y++) {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $r, $gg, $b2))
        }
    }
}

function NewEmpty {
    return (New-Object System.Drawing.Bitmap 16, 16)
}

# ===== 1. Fusion rod: red caps + metal shaft =====
$b = NewEmpty
SetRect $b 6 0 9 15 180 40 40
SetRect $b 6 0 7 2 110 115 125
SetRect $b 6 13 7 15 110 115 125
SetRect $b 8 0 9 2 230 200 60
SetRect $b 8 13 9 15 230 200 60
SetRect $b 6 3 9 12 150 155 165
SetRect $b 6 3 7 12 105 110 120
Save $b "$itemDir\chishi_fusion_rod.png"

# ===== 2. Mixed plasma rod: bright blue-white glow =====
$b = NewEmpty
SetRect $b 6 0 9 15 70 140 255
SetRect $b 6 0 7 15 40 90 200
SetRect $b 8 0 9 15 190 220 255
SetRect $b 6 2 9 13 230 240 255
SetRect $b 6 2 7 13 150 180 240
$b.SetPixel(4, 7, [System.Drawing.Color]::FromArgb(255, 220, 235, 255))
$b.SetPixel(11, 7, [System.Drawing.Color]::FromArgb(255, 220, 235, 255))
$b.SetPixel(7, 1, [System.Drawing.Color]::FromArgb(255, 255, 255, 255))
Save $b "$itemDir\chishi_mixed_plasma_rod.png"

# ===== 3. Nether plasma rod: orange-red glow =====
$b = NewEmpty
SetRect $b 6 0 9 15 255 130 60
SetRect $b 6 0 7 15 200 70 30
SetRect $b 8 0 9 15 255 190 120
SetRect $b 6 2 9 13 255 160 90
SetRect $b 6 2 7 13 220 100 50
$b.SetPixel(4, 7, [System.Drawing.Color]::FromArgb(255, 255, 220, 180))
$b.SetPixel(11, 7, [System.Drawing.Color]::FromArgb(255, 255, 220, 180))
$b.SetPixel(7, 1, [System.Drawing.Color]::FromArgb(255, 255, 255, 230))
Save $b "$itemDir\chishi_nether_plasma_rod.png"

# ===== 4. End plasma rod: purple glow =====
$b = NewEmpty
SetRect $b 6 0 9 15 190 120 255
SetRect $b 6 0 7 15 130 60 200
SetRect $b 8 0 9 15 230 200 255
SetRect $b 6 2 9 13 210 160 255
SetRect $b 6 2 7 13 160 90 230
$b.SetPixel(4, 7, [System.Drawing.Color]::FromArgb(255, 240, 220, 255))
$b.SetPixel(11, 7, [System.Drawing.Color]::FromArgb(255, 240, 220, 255))
$b.SetPixel(7, 1, [System.Drawing.Color]::FromArgb(255, 255, 255, 255))
Save $b "$itemDir\chishi_end_plasma_rod.png"

# ===== 5. Fusion fuel aggregator block =====
$b = NewEmpty
SetRect $b 0 0 15 15 58 62 66
SetRect $b 1 1 14 14 72 76 80
SetRect $b 2 2 13 6 48 52 56
SetRect $b 2 10 13 13 200 60 50
SetRect $b 4 11 11 12 240 100 80
SetRect $b 1 1 14 1 120 124 128
Save $b "$blockDir\chishi_fusion_fuel_aggregator_side.png"
$b = NewEmpty
SetRect $b 0 0 15 15 70 74 78
SetRect $b 1 1 14 14 90 94 98
SetRect $b 6 6 9 9 180 220 255
SetRect $b 5 5 10 10 70 140 255
SetRect $b 4 4 11 11 120 150 190
Save $b "$blockDir\chishi_fusion_fuel_aggregator_top.png"
$b = NewEmpty
SetRect $b 0 0 15 15 45 48 52
SetRect $b 1 1 14 14 58 62 66
Save $b "$blockDir\chishi_fusion_fuel_aggregator_bottom.png"

# ===== 6. Plasma filler block =====
$b = NewEmpty
SetRect $b 0 0 15 15 58 62 66
SetRect $b 1 1 14 14 72 76 80
SetRect $b 2 2 13 13 48 52 56
SetRect $b 3 3 6 12 120 170 255
SetRect $b 7 3 9 12 255 140 70
SetRect $b 10 3 12 12 190 130 255
SetRect $b 1 1 14 1 120 124 128
Save $b "$blockDir\chishi_plasma_filler_side.png"
$b = NewEmpty
SetRect $b 0 0 15 15 70 74 78
SetRect $b 1 1 14 14 90 94 98
SetRect $b 5 5 10 10 60 64 68
SetRect $b 7 7 8 8 220 230 255
Save $b "$blockDir\chishi_plasma_filler_top.png"
$b = NewEmpty
SetRect $b 0 0 15 15 45 48 52
SetRect $b 1 1 14 14 58 62 66
Save $b "$blockDir\chishi_plasma_filler_bottom.png"

# ===== 7. Plasma pipe: metal ring + glowing core =====
$b = NewEmpty
SetRect $b 3 3 12 12 160 165 170
SetRect $b 4 4 11 11 90 94 98
SetRect $b 5 5 10 10 60 64 68
SetRect $b 6 6 9 9 110 160 255
SetRect $b 7 7 8 8 230 240 255
Save $b "$blockDir\chishi_plasma_pipe.png"

# ===== 8. Plasma fluid textures =====
$b = NewEmpty
SetRect $b 6 6 9 9 235 245 255
SetRect $b 5 5 10 10 190 220 255
SetRect $b 4 4 11 11 140 190 255
SetRect $b 3 3 12 12 90 140 220
SetRect $b 2 2 13 13 50 90 170
SetRect $b 0 0 15 15 30 55 110
Save $b "$fluidDir\plasma_still.png"
$b = NewEmpty
SetRect $b 6 7 9 10 235 245 255
SetRect $b 5 6 10 11 190 220 255
SetRect $b 4 5 11 12 140 190 255
SetRect $b 3 4 12 13 90 140 220
SetRect $b 2 3 13 14 50 90 170
SetRect $b 0 0 15 15 30 55 110
Save $b "$fluidDir\plasma_flow.png"

# ===== 9. Plasma fuel tank block =====
$b = NewEmpty
SetRect $b 0 0 15 15 40 60 90
SetRect $b 1 1 14 14 52 74 106
SetRect $b 2 2 13 13 42 62 92
SetRect $b 6 4 9 11 120 180 255
SetRect $b 7 5 8 10 200 230 255
SetRect $b 1 1 14 1 70 92 124
Save $b "$blockDir\chishi_plasma_tank_side.png"
$b = NewEmpty
SetRect $b 0 0 15 15 52 74 106
SetRect $b 1 1 14 14 62 84 116
SetRect $b 5 5 10 10 80 120 200
SetRect $b 7 7 8 8 220 240 255
Save $b "$blockDir\chishi_plasma_tank_top.png"
$b = NewEmpty
SetRect $b 0 0 15 15 36 52 78
SetRect $b 1 1 14 14 46 64 92
Save $b "$blockDir\chishi_plasma_tank_bottom.png"

Write-Output "DONE"
