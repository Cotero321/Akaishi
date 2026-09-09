# Generate mechanical part shape and material textures (32x32 shapes, 16x16 materials)
Add-Type -AssemblyName System.Drawing

$targetDir = Join-Path $PSScriptRoot 'common\src\main\resources\assets\akaishi\textures\mechanical_part'
$shapeDir = Join-Path $targetDir "shape"
$materialDir = Join-Path $targetDir "material"

# Use $PSScriptRoot-relative path (Unicode-safe); script itself is ASCII-only
New-Item -ItemType Directory -Path $shapeDir -Force -ErrorAction SilentlyContinue | Out-Null
New-Item -ItemType Directory -Path $materialDir -Force -ErrorAction SilentlyContinue | Out-Null

$S = 64  # complex mechanical parts use 64x64 textures
$BaseS = 32
$Scale = $S / $BaseS

function SX { param($v) return [int][Math]::Round($v * $Scale) }

function SavePng {
    param($bmp, $filename)
    $bmp.Save((Join-Path $shapeDir $filename), [System.Drawing.Imaging.ImageFormat]::Png)
}

function SaveMaterialPng {
    param($bmp, $filename)
    $bmp.Save((Join-Path $materialDir $filename), [System.Drawing.Imaging.ImageFormat]::Png)
}

function Clamp { param($v) return [Math]::Max(0, [Math]::Min(255, [int]($v))) }

function SetPx {
    param($bmp, $x, $y, $a, $r, $g, $b)
    $x = SX $x
    $y = SX $y
    $ca = Clamp($a)
    $cr = Clamp($r)
    $cg = Clamp($g)
    $cb = Clamp($b)
    if ($x -ge 0 -and $x -lt $bmp.Width -and $y -ge 0 -and $y -lt $bmp.Height) {
        $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($ca, $cr, $cg, $cb))
    }
}

function DrawLine {
    param($bmp, $x1, $y1, $x2, $y2, $a, $r, $g, $b)
    $dx = [Math]::Abs($x2 - $x1)
    $dy = [Math]::Abs($y2 - $y1)
    $sx = if ($x1 -lt $x2) { 1 } else { -1 }
    $sy = if ($y1 -lt $y2) { 1 } else { -1 }
    $err = $dx - $dy
    $x = $x1; $y = $y1
    while ($true) {
        if ($x -ge 0 -and $x -lt $bmp.Width -and $y -ge 0 -and $y -lt $bmp.Height) {
            SetPx $bmp $x $y $a $r $g $b
        }
        if ($x -eq $x2 -and $y -eq $y2) { break }
        $e2 = 2 * $err
        if ($e2 -gt -$dy) { $err -= $dy; $x += $sx }
        if ($e2 -lt $dx) { $err += $dx; $y += $sy }
    }
}

function FillCircle {
    param($bmp, $cx, $cy, $r, $a, $cr, $cg, $cb)
    $rInt = $r
    for ($x = -$rInt; $x -le $rInt; $x++) {
        for ($y = -$rInt; $y -le $rInt; $y++) {
            $dist = [Math]::Sqrt($x*$x + $y*$y)
            $px = $cx + $x; $py = $cy + $y
            if ($px -lt 0 -or $px -ge $bmp.Width -or $py -lt 0 -or $py -ge $bmp.Height) { continue }
            if ($dist -le $rInt) { SetPx $bmp $px $py $a $cr $cg $cb }
        }
    }
}

function FillCircleAA {
    param($bmp, $cx, $cy, $r, $a, $cr, $cg, $cb)
    $rInt = $r
    for ($x = -$rInt - 1; $x -le $rInt + 1; $x++) {
        for ($y = -$rInt - 1; $y -le $rInt + 1; $y++) {
            $dist = [Math]::Sqrt($x*$x + $y*$y)
            $px = $cx + $x; $py = $cy + $y
            if ($px -lt 0 -or $px -ge $bmp.Width -or $py -lt 0 -or $py -ge $bmp.Height) { continue }
            if ($dist -le $rInt) {
                $alpha = $a
                if ($dist -gt ($rInt - 1)) { $alpha = [int]($a * ($rInt - $dist + 1)) }
                SetPx $bmp $px $py $alpha $cr $cg $cb
            }
        }
    }
}

function FillRect {
    param($bmp, $x1, $y1, $x2, $y2, $a, $r, $g, $b)
    for ($x = $x1; $x -le $x2; $x++) {
        for ($y = $y1; $y -le $y2; $y++) {
            if ($x -ge 0 -and $x -lt $bmp.Width -and $y -ge 0 -and $y -lt $bmp.Height) {
                SetPx $bmp $x $y $a $r $g $b
            }
        }
    }
}

function FillRectRounded {
    param($bmp, $x1, $y1, $x2, $y2, $radius, $a, $r, $g, $b)
    # main body
    FillRect $bmp ($x1 + $radius) $y1 ($x2 - $radius) $y2 $a $r $g $b
    FillRect $bmp $x1 ($y1 + $radius) $x2 ($y2 - $radius) $a $r $g $b
    # corners
    for ($cx = $x1 + $radius; $cx -le $x2 - $radius; $cx++) {
        for ($cy = $y1 + $radius; $cy -le $y2 - $radius; $cy++) {
            SetPx $bmp $cx $cy $a $r $g $b
        }
    }
    # rounded corners
    for ($i = 0; $i -lt $radius; $i++) {
        for ($j = 0; $j -lt $radius; $j++) {
            $dist = [Math]::Sqrt(($i+0.5)*($i+0.5) + ($j+0.5)*($j+0.5))
            if ($dist -le $radius) {
                SetPx $bmp ($x1 + $radius - 1 - $i) ($y1 + $radius - 1 - $j) $a $r $g $b
                SetPx $bmp ($x2 - $radius + $i) ($y1 + $radius - 1 - $j) $a $r $g $b
                SetPx $bmp ($x1 + $radius - 1 - $i) ($y2 - $radius + $j) $a $r $g $b
                SetPx $bmp ($x2 - $radius + $i) ($y2 - $radius + $j) $a $r $g $b
            }
        }
    }
}

# ==================== PART SHAPE TEXTURES (32x32) ====================

# --- CORE: gear/cog with center glow ---
$bmp = New-Object System.Drawing.Bitmap($S, $S)
# outer gear ring
FillCircleAA $bmp 16 16 14 50 180 180 200
# gear teeth (8 teeth)
for ($i = 0; $i -lt 8; $i++) {
    $angle = $i * [Math]::PI / 4
    $tx = [int](16 + [Math]::Cos($angle) * 13)
    $ty = [int](16 + [Math]::Sin($angle) * 13)
    for ($dx = -2; $dx -le 2; $dx++) { for ($dy = -2; $dy -le 2; $dy++) {
        if ($dx*$dx + $dy*$dy -le 4) { SetPx $bmp ($tx+$dx) ($ty+$dy) 180 200 200 220 }
    }}
}
# inner ring
FillCircleAA $bmp 16 16 9 120 160 160 180
# center glow
FillCircleAA $bmp 16 16 5 200 255 255 255
# core center
FillCircleAA $bmp 16 16 2 220 255 200 100
# inner detail lines
DrawLine $bmp 16 2 16 7 100 255 255 255
DrawLine $bmp 16 25 16 30 100 255 255 255
DrawLine $bmp 2 16 7 16 100 255 255 255
DrawLine $bmp 25 16 30 16 100 255 255 255
SavePng $bmp "core.png"
$bmp.Dispose()
Write-Output "Created shape: core.png (32x32 detailed gear)"

# --- MODULE: circuit board with traces ---
$bmp = New-Object System.Drawing.Bitmap($S, $S)
# board base
FillRect $bmp 4 4 27 27 160 180 200 180
# board edge highlight
DrawLine $bmp 4 4 27 4 180 220 240 220
DrawLine $bmp 4 4 4 27 180 220 240 220
# circuit traces
DrawLine $bmp 6 6 14 6 200 255 220 200
DrawLine $bmp 14 6 14 14 200 255 220 200
DrawLine $bmp 14 14 22 14 200 255 220 200
DrawLine $bmp 22 14 22 22 200 255 220 200
DrawLine $bmp 6 22 14 22 200 200 220 255
DrawLine $bmp 14 22 14 14 200 200 220 255
# nodes (connection points)
FillCircleAA $bmp 6 6 3 200 255 200 100
FillCircleAA $bmp 14 14 3 200 255 200 100
FillCircleAA $bmp 22 22 3 200 255 200 100
FillCircleAA $bmp 6 22 3 200 255 200 100
# chip in center
FillRect $bmp 10 9 18 17 200 220 180 160
FillRect $bmp 12 11 16 15 220 200 200 200
# pins around chip
for ($px = 11; $px -le 17; $px += 2) {
    SetPx $bmp $px 8 200 255 220 180
    SetPx $bmp $px 18 200 255 220 180
}
for ($py = 10; $py -le 16; $py += 2) {
    SetPx $bmp 9 $py 200 255 220 180
    SetPx $bmp 19 $py 200 255 220 180
}
SavePng $bmp "module.png"
$bmp.Dispose()
Write-Output "Created shape: module.png (32x32 detailed circuit)"

# --- SHELL: armored plate with rivets ---
$bmp = New-Object System.Drawing.Bitmap($S, $S)
# main plate
FillRectRounded $bmp 3 3 28 28 4 180 180 190 200
# plate edge (thicker border)
DrawLine $bmp 3 3 28 3 200 200 210 220
DrawLine $bmp 3 3 3 28 200 200 210 220
DrawLine $bmp 28 3 28 28 180 160 170 180
DrawLine $bmp 3 28 28 28 180 160 170 180
# center reinforcement ridge
FillRect $bmp 8 14 23 17 160 190 200 210
FillRect $bmp 8 14 23 14 180 210 220 230
FillRect $bmp 8 17 23 17 140 170 180 190
# rivets at corners
$rivets = @((6,6), (25,6), (6,25), (25,25))
foreach ($rv in $rivets) {
    FillCircleAA $bmp $rv[0] $rv[1] 3 200 200 180 160
    SetPx $bmp $rv[0] $rv[1] 220 220 220 220
}
# mid-edge rivets
$midRivets = @((16,4), (16,28), (4,16), (28,16))
foreach ($rv in $midRivets) {
    FillCircleAA $bmp $rv[0] $rv[1] 2 180 180 180 190
}
# inner detail - panel lines
DrawLine $bmp 6 10 25 10 100 180 190 200
DrawLine $bmp 6 20 25 20 100 180 190 200
SavePng $bmp "shell.png"
$bmp.Dispose()
Write-Output "Created shape: shell.png (32x32 detailed armor)"

# --- COOLING: heat sink fins with vents ---
$bmp = New-Object System.Drawing.Bitmap($S, $S)
# base plate
FillRect $bmp 2 20 29 29 180 160 180 200
# fins (vertical)
for ($fx = 4; $fx -le 28; $fx += 4) {
    FillRect $bmp $fx 2 $fx 20 160 180 200 220
    # fin edge highlight
    SetPx $bmp $fx 2 200 220 240 255
    SetPx $bmp $fx 20 140 160 180 200
}
# center spine
FillRect $bmp 14 2 17 20 180 200 220 240
# vent holes
for ($vx = 6; $vx -le 24; $vx += 6) {
    FillRect $bmp ($vx+1) 12 ($vx+3) 16 200 100 120 140
    FillRect $bmp ($vx+1) 12 ($vx+3) 12 180 80 100 120
}
# fan hub in center
FillCircleAA $bmp 16 10 4 180 200 200 220
SetPx $bmp 16 10 220 255 255 255
# fan blades
for ($bi = 0; $bi -lt 6; $bi++) {
    $ba = $bi * [Math]::PI / 3
    $bx = [int](16 + [Math]::Cos($ba) * 7)
    $by = [int](10 + [Math]::Sin($ba) * 7)
    FillCircleAA $bmp $bx $by 2 120 200 220 240
}
SavePng $bmp "cooling.png"
$bmp.Dispose()
Write-Output "Created shape: cooling.png (32x32 detailed heat sink)"

# ==================== ORGAN SHAPE TEXTURES (32x32) ====================

# --- EYE: circular lens with pupil ---
$bmp = New-Object System.Drawing.Bitmap($S, $S)
FillCircleAA $bmp 16 16 13 160 180 200 220
FillCircleAA $bmp 16 16 10 140 200 210 230
FillCircleAA $bmp 16 16 7 180 220 230 240
FillCircleAA $bmp 16 16 4 200 80 80 100
FillCircleAA $bmp 16 16 2 220 255 200 100
# lens glare
DrawLine $bmp 10 10 14 14 100 255 255 255
SetPx $bmp 9 11 100 255 255 255
# outer ring details
for ($i = 0; $i -lt 12; $i++) {
    $ra = $i * [Math]::PI / 6
    $rx = [int](16 + [Math]::Cos($ra) * 12)
    $ry = [int](16 + [Math]::Sin($ra) * 12)
    SetPx $bmp $rx $ry 80 255 255 255
}
# mounting bracket
FillRect $bmp 2 14 4 18 120 160 180 200
FillRect $bmp 28 14 30 18 120 160 180 200
SavePng $bmp "organ_eye.png"
$bmp.Dispose()
Write-Output "Created shape: organ_eye.png"

# --- HEART: mechanical heart with chambers ---
$bmp = New-Object System.Drawing.Bitmap($S, $S)
# main heart shape (two overlapping circles + triangle bottom)
FillCircleAA $bmp 12 12 9 160 200 180 200
FillCircleAA $bmp 20 12 9 160 200 180 200
# bottom point
for ($x = 6; $x -le 26; $x++) {
    for ($y = 14; $y -le 24; $y++) {
        $leftDist = [Math]::Sqrt(($x-12)*($x-12) + ($y-12)*($y-12))
        $rightDist = [Math]::Sqrt(($x-20)*($x-20) + ($y-12)*($y-12))
        if ($leftDist -le 9 -or $rightDist -le 9) { continue }
        if ($x -ge 10 -and $x -le 22 -and $y -ge ($x/2 + 10) -and $y -le (-$x/2 + 26)) {
            SetPx $bmp $x $y 160 200 180 200
        }
    }
}
# chambers
FillCircleAA $bmp 11 12 5 180 180 100 120
FillCircleAA $bmp 21 12 5 180 180 100 120
# center divider
DrawLine $bmp 16 4 16 22 140 220 200 220
# tubes/arteries
FillRect $bmp 14 2 18 4 120 200 180 200
FillRect $bmp 14 22 18 25 120 200 180 200
# mechanical detail
SetPx $bmp 16 14 200 255 200 100
SetPx $bmp 16 15 200 255 200 100
SavePng $bmp "organ_heart.png"
$bmp.Dispose()
Write-Output "Created shape: organ_heart.png"

# --- LUNG: dual lobe structure ---
$bmp = New-Object System.Drawing.Bitmap($S, $S)
# left lobe
FillCircleAA $bmp 10 15 10 140 200 190 210
# right lobe (larger)
FillCircleAA $bmp 22 15 11 140 200 190 210
# windpipe
FillRect $bmp 14 2 18 10 140 200 180 200
DrawLine $bmp 16 10 16 14 100 220 200 220
# branching
DrawLine $bmp 16 8 10 12 100 220 220 240
DrawLine $bmp 16 8 22 12 100 220 220 240
# lobe detail - alveoli
for ($li = 0; $li -lt 5; $li++) {
    $lx = 6 + $li * 2
    $ly = 12 + $li
    if ($lx -ge 2 -and $lx -le 18) { SetPx $bmp $lx $ly 120 255 220 240 }
}
for ($ri = 0; $ri -lt 6; $ri++) {
    $rx = 18 + $ri * 2
    $ry = 12 + $ri
    if ($rx -ge 18 -and $rx -le 30) { SetPx $bmp $rx $ry 120 255 220 240 }
}
SavePng $bmp "organ_lung.png"
$bmp.Dispose()
Write-Output "Created shape: organ_lung.png"

# --- VISCERA: coiled tubular ---
$bmp = New-Object System.Drawing.Bitmap($S, $S)
# coiled tube path
for ($t = 0; $t -lt 100; $t++) {
    $angle = $t * 0.2
    $tx = [int](16 + [Math]::Cos($angle) * ($angle * 0.08))
    $ty = [int](16 + [Math]::Sin($angle) * ($angle * 0.08))
    if ($tx -ge 0 -and $tx -lt $S -and $ty -ge 0 -and $ty -lt $S) {
        SetPx $bmp $tx $ty 160 200 180 160
        SetPx $bmp ($tx+1) $ty 120 220 200 180
    }
}
# outer container
FillCircleAA $bmp 16 16 14 60 180 160 140
# connection ports
FillRect $bmp 6 2 10 4 120 200 180 160
FillRect $bmp 22 28 26 30 120 200 180 160
# inner detail
FillCircleAA $bmp 16 16 8 80 220 200 180
SavePng $bmp "organ_viscera.png"
$bmp.Dispose()
Write-Output "Created shape: organ_viscera.png"

# --- KIDNEY: bean shape ---
$bmp = New-Object System.Drawing.Bitmap($S, $S)
# bean shape (two overlapping circles with indentation)
FillCircleAA $bmp 12 16 10 150 180 170 190
FillCircleAA $bmp 20 16 9 150 180 170 190
# fill in the gap
for ($x = 6; $x -le 26; $x++) {
    for ($y = 6; $y -le 26; $y++) {
        $d1 = [Math]::Sqrt(($x-12)*($x-12) + ($y-16)*($y-16))
        $d2 = [Math]::Sqrt(($x-20)*($x-20) + ($y-16)*($y-16))
        if ($d1 -le 10 -or $d2 -le 9) { continue }
        # indentation at center-left
        if ($x -ge 10 -and $x -le 14 -and $y -ge 12 -and $y -le 20) { continue }
        if ($x -ge 12 -and $x -le 14 -and $y -ge 10 -and $y -le 22) { continue }
    }
}
# center indentation
FillRect $bmp 11 12 14 20 0 0 0 0
# inner medulla
FillCircleAA $bmp 14 16 5 120 200 150 170
# ureter tube
FillRect $bmp 13 2 17 6 120 180 160 180
DrawLine $bmp 15 6 15 12 100 200 180 200
# outer edge highlight
DrawLine $bmp 5 16 8 16 100 220 200 220
DrawLine $bmp 22 16 26 16 100 220 200 220
SavePng $bmp "organ_kidney.png"
$bmp.Dispose()
Write-Output "Created shape: organ_kidney.png"

# --- LEFT_ARM: arm silhouette ---
$bmp = New-Object System.Drawing.Bitmap($S, $S)
# shoulder
FillCircleAA $bmp 8 6 7 160 180 180 200
# upper arm
FillRect $bmp 5 8 11 18 160 180 180 200
# elbow
FillCircleAA $bmp 8 19 5 160 180 180 200
# forearm
FillRect $bmp 6 20 12 27 160 180 180 200
# hand
FillCircleAA $bmp 9 28 3 160 180 180 200
# mechanical joints
FillCircleAA $bmp 8 6 3 180 220 200 180
FillCircleAA $bmp 8 19 3 180 220 200 180
# arm detail - hydraulic lines
DrawLine $bmp 6 10 6 18 120 255 220 200
DrawLine $bmp 10 10 10 18 120 255 220 200
# servo nodes
SetPx $bmp 8 13 200 255 200 100
SetPx $bmp 8 23 200 255 200 100
SavePng $bmp "organ_left_arm.png"
$bmp.Dispose()
Write-Output "Created shape: organ_left_arm.png"

# --- RIGHT_ARM: arm silhouette (mirrored) ---
$bmp = New-Object System.Drawing.Bitmap($S, $S)
# shoulder
FillCircleAA $bmp 24 6 7 160 180 180 200
# upper arm
FillRect $bmp 21 8 27 18 160 180 180 200
# elbow
FillCircleAA $bmp 24 19 5 160 180 180 200
# forearm
FillRect $bmp 20 20 26 27 160 180 180 200
# hand
FillCircleAA $bmp 23 28 3 160 180 180 200
# mechanical joints
FillCircleAA $bmp 24 6 3 180 220 200 180
FillCircleAA $bmp 24 19 3 180 220 200 180
# arm detail - hydraulic lines
DrawLine $bmp 22 10 22 18 120 255 220 200
DrawLine $bmp 26 10 26 18 120 255 220 200
# servo nodes
SetPx $bmp 24 13 200 255 200 100
SetPx $bmp 24 23 200 255 200 100
SavePng $bmp "organ_right_arm.png"
$bmp.Dispose()
Write-Output "Created shape: organ_right_arm.png"

# --- LEFT_LEG: leg silhouette ---
$bmp = New-Object System.Drawing.Bitmap($S, $S)
# hip joint
FillCircleAA $bmp 8 4 5 160 160 180 200
# upper thigh
FillRect $bmp 5 6 11 16 160 160 180 200
# knee
FillCircleAA $bmp 8 17 5 160 160 180 200
# lower leg
FillRect $bmp 6 18 12 26 160 160 180 200
# foot
FillRect $bmp 4 27 13 29 160 160 180 200
# mechanical joints
FillCircleAA $bmp 8 4 3 180 200 180 160
FillCircleAA $bmp 8 17 3 180 200 180 160
# piston detail
DrawLine $bmp 8 8 8 15 140 255 220 200
FillCircleAA $bmp 8 11 2 180 255 200 100
# shock absorber
DrawLine $bmp 6 20 6 26 100 200 200 220
DrawLine $bmp 10 20 10 26 100 200 200 220
SavePng $bmp "organ_left_leg.png"
$bmp.Dispose()
Write-Output "Created shape: organ_left_leg.png"

# --- RIGHT_LEG: leg silhouette (mirrored) ---
$bmp = New-Object System.Drawing.Bitmap($S, $S)
# hip joint
FillCircleAA $bmp 24 4 5 160 160 180 200
# upper thigh
FillRect $bmp 21 6 27 16 160 160 180 200
# knee
FillCircleAA $bmp 24 17 5 160 160 180 200
# lower leg
FillRect $bmp 20 18 26 26 160 160 180 200
# foot
FillRect $bmp 19 27 28 29 160 160 180 200
# mechanical joints
FillCircleAA $bmp 24 4 3 180 200 180 160
FillCircleAA $bmp 24 17 3 180 200 180 160
# piston detail
DrawLine $bmp 24 8 24 15 140 255 220 200
FillCircleAA $bmp 24 11 2 180 255 200 100
# shock absorber
DrawLine $bmp 22 20 22 26 100 200 200 220
DrawLine $bmp 26 20 26 26 100 200 200 220
SavePng $bmp "organ_right_leg.png"
$bmp.Dispose()
Write-Output "Created shape: organ_right_leg.png"

# ==================== MATERIAL TEXTURES (16x16, richer patterns) ====================

# Pattern 0: horizontal grain lines
# Pattern 1: crosshatch grid
# Pattern 2: L-shaped corner dots
# Pattern 3: diagonal stripes
# Pattern 4: radial burst from center
# Pattern 5: dense horizontal lines
# Pattern 6: concentric circles
# Pattern 7: hex grid
# Pattern 8: speckle/noise
# Pattern 9: vertical stripes + horizontal

function New-MaterialTexture {
    param($name, $br, $bg, $bb, $ar, $ag, $ab, $pattern)
    $bmp = New-Object System.Drawing.Bitmap(16, 16)
    # base fill
    for ($x = 0; $x -lt 16; $x++) { for ($y = 0; $y -lt 16; $y++) {
        SetPx $bmp $x $y 255 $br $bg $bb
    }}

    if ($pattern -eq 0) {
        # horizontal grain
        for ($y = 0; $y -lt 16; $y += 2) { DrawLine $bmp 0 $y 15 $y 80 $ar $ag $ab }
        for ($y = 1; $y -lt 16; $y += 4) { DrawLine $bmp 0 $y 15 $y 40 ($ar*0.5) ($ag*0.5) ($ab*0.5) }
    } elseif ($pattern -eq 1) {
        # crosshatch grid
        for ($x = 0; $x -lt 16; $x += 4) { DrawLine $bmp $x 0 $x 15 60 $ar $ag $ab }
        for ($y = 0; $y -lt 16; $y += 4) { DrawLine $bmp 0 $y 15 $y 60 $ar $ag $ab }
        # grid dots at intersections
        for ($x = 0; $x -lt 16; $x += 4) { for ($y = 0; $y -lt 16; $y += 4) {
            SetPx $bmp $x $y 100 ($ar+40) ($ag+40) ($ab+40)
        }}
    } elseif ($pattern -eq 2) {
        # L-shaped corner brackets
        for ($x = 0; $x -lt 16; $x += 4) { for ($y = 0; $y -lt 16; $y += 4) {
            DrawLine $bmp $x $y ($x+2) $y 80 $ar $ag $ab
            DrawLine $bmp $x $y $x ($y+2) 80 $ar $ag $ab
        }}
    } elseif ($pattern -eq 3) {
        # diagonal stripes
        for ($i = -16; $i -lt 32; $i += 4) { DrawLine $bmp $i 0 ($i - 16) 16 60 $ar $ag $ab }
        for ($i = -16; $i -lt 32; $i += 8) { DrawLine $bmp $i 0 ($i - 16) 16 40 ($ar*0.7) ($ag*0.7) ($ab*0.7) }
    } elseif ($pattern -eq 4) {
        # radial burst from center
        for ($a = 0; $a -lt 8; $a++) {
            $angle = $a * [Math]::PI / 4
            $ex = [int](8 + [Math]::Cos($angle) * 10)
            $ey = [int](8 + [Math]::Sin($angle) * 10)
            DrawLine $bmp 8 8 $ex $ey 60 $ar $ag $ab
        }
        FillCircleAA $bmp 8 8 3 80 $ar $ag $ab
    } elseif ($pattern -eq 5) {
        # dense horizontal lines
        for ($y = 0; $y -lt 16; $y += 2) { DrawLine $bmp 0 $y 15 $y 60 $ar $ag $ab }
        for ($y = 1; $y -lt 16; $y += 2) { DrawLine $bmp 0 $y 15 $y 30 ($ar*0.6) ($ag*0.6) ($ab*0.6) }
    } elseif ($pattern -eq 6) {
        # concentric circles
        for ($r = 2; $r -le 10; $r += 3) { FillCircleAA $bmp 8 8 $r 50 $ar $ag $ab }
        SetPx $bmp 8 8 100 ($ar+60) ($ag+60) ($ab+60)
    } elseif ($pattern -eq 7) {
        # hex grid approximation
        for ($x = 0; $x -lt 16; $x += 6) { for ($y = 0; $y -lt 16; $y += 5) {
            FillCircleAA $bmp $x $y 2 70 $ar $ag $ab
            FillCircleAA $bmp ($x+3) ($y+3) 2 70 $ar $ag $ab
        }}
    } elseif ($pattern -eq 8) {
        # speckle/noise
        for ($i = 0; $i -lt 20; $i++) {
            $nx = [int]($i * 7.3) % 16
            $ny = [int]($i * 11.7) % 16
            SetPx $bmp $nx $ny 100 ($ar+$i*2) ($ag+$i) ($ab+$i*1.5)
        }
    } elseif ($pattern -eq 9) {
        # vertical + horizontal mixed
        for ($x = 0; $x -lt 16; $x += 3) { DrawLine $bmp $x 0 $x 15 50 $ar $ag $ab }
        for ($y = 0; $y -lt 16; $y += 6) { DrawLine $bmp 0 $y 15 $y 50 $ar $ag $ab }
    }

    # edge border (subtle)
    DrawLine $bmp 0 0 15 0 40 255 255 255
    DrawLine $bmp 0 0 0 15 40 255 255 255
    SaveMaterialPng $bmp "$name.png"
    $bmp.Dispose()
    Write-Output "Created material: $name.png"
}

# Basic materials (5pts)
New-MaterialTexture "iron" 160 160 160 180 180 180 0
New-MaterialTexture "redstone_alloy" 180 60 40 220 80 60 4
New-MaterialTexture "ceramic_composite" 220 220 210 200 200 190 1
New-MaterialTexture "resistant_steel" 80 90 100 100 110 120 0

# Mid materials (7pts)
New-MaterialTexture "precision_alloy" 190 200 215 210 220 235 3
New-MaterialTexture "polymerized_redstone" 120 20 30 180 30 40 2
New-MaterialTexture "bio_ceramic" 160 190 140 180 210 160 1

# Advanced materials (9pts)
New-MaterialTexture "refined_core" 220 140 40 255 180 60 4
New-MaterialTexture "alloy_steel" 100 110 130 120 130 150 5

# Top materials (11pts)
New-MaterialTexture "psionic_composite" 120 80 160 160 100 200 2

Write-Output "All textures generated successfully!"
Write-Output "Generated: 4 part shapes, 9 organ shapes, 10 material textures"