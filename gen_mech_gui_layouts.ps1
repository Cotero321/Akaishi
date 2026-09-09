# Generate GUI layout sketches for 3 mechanical machines (enlarged panel, inv moved down)
# ASCII-only content (PS5.1 GBK safety). Outputs to ./gui_layouts/
Add-Type -AssemblyName System.Drawing

$outDir = Join-Path $PSScriptRoot 'gui_layouts'
New-Item -ItemType Directory -Path $outDir -Force -ErrorAction SilentlyContinue | Out-Null

$PANEL = [System.Drawing.Color]::FromArgb(255, 198, 198, 198)
$SLOT = [System.Drawing.Color]::FromArgb(255, 139, 139, 139)
$DARK = [System.Drawing.Color]::FromArgb(255, 55, 55, 55)
$LIGHT = [System.Drawing.Color]::FromArgb(255, 255, 255, 255)
$RED = [System.Drawing.Color]::FromArgb(255, 224, 48, 48)
$GREEN = [System.Drawing.Color]::FromArgb(255, 40, 180, 40)
$YELLOW = [System.Drawing.Color]::FromArgb(255, 255, 208, 48)
$BLUE = [System.Drawing.Color]::FromArgb(255, 90, 130, 190)
$TEXT = [System.Drawing.Color]::FromArgb(255, 63, 63, 63)

$PW = 176   # panel width
$PH = 196   # enlarged panel height (inv zone moved down)
$STR_Y = 56 # machine slot row
$INV_Y = 104 # player inv 3x9 start row
$BAR_Y = 168 # hotbar row
$INFO_Y = 80 # info text row

function New-Canvas {
    param($w, $h)
    $bmp = New-Object System.Drawing.Bitmap($w, $h)
    for ($x = 0; $x -lt $w; $x++) { for ($y = 0; $y -lt $h; $y++) { $bmp.SetPixel($x, $y, $PANEL) } }
    return $bmp
}

function Fill {
    param($bmp, $x1, $y1, $x2, $y2, $c)
    for ($x = $x1; $x -le $x2; $x++) { for ($y = $y1; $y -le $y2; $y++) { $bmp.SetPixel($x, $y, $c) } }
}

function SlotBox {
    param($bmp, $x, $y)
    Fill $bmp $x $y ($x+17) ($y+17) $SLOT
    for ($i = 0; $i -lt 18; $i++) { $bmp.SetPixel($x+$i, $y, $DARK); $bmp.SetPixel($x+$i, $y+17, $LIGHT); $bmp.SetPixel($x, $y+$i, $DARK); $bmp.SetPixel($x+17, $y+$i, $LIGHT) }
}

function Track {
    param($bmp, $x, $y, $w, $h)
    Fill $bmp $x $y ($x+$w-1) ($y+$h-1) $SLOT
    for ($i = 0; $i -lt $w; $i++) { $bmp.SetPixel($x+$i, $y, $DARK); $bmp.SetPixel($x+$i, $y+$h-1, $LIGHT) }
    for ($i = 0; $i -lt $h; $i++) { $bmp.SetPixel($x, $y+$i, $DARK); $bmp.SetPixel($x+$w-1, $y+$i, $LIGHT) }
}

function BarFill {
    param($bmp, $x, $y, $w, $h, $c)
    Fill $bmp $x $y ($x+$w-1) ($y+$h-1) $c
}

function Label {
    param($bmp, $x, $y, $len)
    Fill $bmp $x $y ($x+$len-1) ($y+1) $TEXT
}

function PlayerInv {
    param($bmp)
    for ($r = 0; $r -lt 3; $r++) { for ($c = 0; $c -lt 9; $c++) { SlotBox $bmp (8 + $c*18) ($INV_Y + $r*18) } }
    for ($c = 0; $c -lt 9; $c++) { SlotBox $bmp (8 + $c*18) $BAR_Y }
}

# Right arrow that doubles as a progress bar. Body rect + solid triangle head.
# Progress color fills body left->right up to pct. All coords clamped to canvas.
function ProgressArrow {
    param($bmp, $x, $y, $w, $h, $pct)
    $head = 6
    $bodyW = $w - $head
    $mid = [int]($h/2)
    # grey arrow base: body rect + triangle head (stacked rows)
    $bmp.SetPixel($x, $y, $DARK); $bmp.SetPixel($x, $y+$h-1, $DARK)
    for ($i = 0; $i -lt $h; $i++) {
        $dy = [Math]::Abs($i - $mid)
        $rowW = $head - $dy
        if ($rowW -lt 1) { $rowW = 1 }
        $col = if ($i -eq 0 -or $i -eq $h-1) { $DARK } elseif ($dy -eq 0) { $LIGHT } else { $SLOT }
        # head horizontal run (right portion), white tip edge on far right
        $sx = $x + $bodyW; $ex = $x + $bodyW + $rowW - 1
        if ($ex -ge $bmp.Width) { $ex = $bmp.Width - 1 }
        if ($sx -lt $ex) { Fill $bmp $sx ($y+$i) $ex ($y+$i) $col }
        $bmp.SetPixel($ex, $y+$i, $LIGHT)
    }
    # body fill with slot grey + dark/light edges
    for ($i = 0; $i -lt $bodyW; $i++) {
        for ($j = 0; $j -lt $h; $j++) {
            $edge = ($i -eq 0 -or $j -eq 0) -and ($i -eq 0 -or $j -eq $h-1)
            $c = $SLOT
            if ($j -eq 0) { $c = $DARK } elseif ($j -eq $h-1) { $c = $LIGHT } elseif ($i -eq 0) { $c = $DARK }
            if (($x+$i) -ge 0 -and ($x+$i) -lt $bmp.Width -and ($y+$j) -ge 0 -and ($y+$j) -lt $bmp.Height) { $bmp.SetPixel($x+$i, $y+$j, $c) }
        }
    }
    # body right edge
    for ($j = 0; $j -lt $h; $j++) { if (($x+$bodyW-1) -lt $bmp.Width -and ($y+$j) -lt $bmp.Height) { $bmp.SetPixel($x+$bodyW-1, $y+$j, $LIGHT) } }
    # progress fill inside body, yellow
    $fillB = [int]($bodyW * $pct / 100)
    if ($fillB -gt 0) {
        if ($fillB -gt $bodyW) { $fillB = $bodyW }
        BarFill $bmp $x $y $fillB $h $YELLOW
    }
}

# Dropdown box (collapsed shows selected label + down chevron). If $open, draw item rows below.
function Dropdown {
    param($bmp, $x, $y, $w, $h, $textLen, $open, $openRows)
    Fill $bmp $x $y ($x+$w-1) ($y+$h-1) $SLOT
    for ($i = 0; $i -lt $w; $i++) { $bmp.SetPixel($x+$i, $y, $DARK); $bmp.SetPixel($x+$i, $y+$h-1, $LIGHT) }
    for ($i = 0; $i -lt $h; $i++) { $bmp.SetPixel($x, $y+$i, $DARK); $bmp.SetPixel($x+$w-1, $y+$i, $LIGHT) }
    # selected label text
    Label $bmp ($x+2) ($y+2) $textLen
    # down chevron at right
    $cx = $x + $w - 8
    for ($i = 0; $i -lt 4; $i++) { $bmp.SetPixel($cx+$i, $y+$h-5+$i, $TEXT) }
    if ($open) {
        for ($r = 0; $r -lt $openRows; $r++) {
            $ry = $y + $h + $r*($h+1)
            Fill $bmp $x $ry ($x+$w-1) ($ry+$h-1) $SLOT
            for ($i = 0; $i -lt $w; $i++) { $bmp.SetPixel($x+$i, $ry, $LIGHT); $bmp.SetPixel($x+$i, $ry+$h-1, $DARK) }
            Label $bmp ($x+2) ($ry+2) ($textLen-4)
        }
    }
}

# ===== shared top area =====
function DrawTop {
    param($bmp)
    Label $bmp 8 6 88                    # title
    Track $bmp 20 24 60 6; BarFill $bmp 21 25 40 4 $RED     # akaishi bar
    Track $bmp 20 32 60 6; BarFill $bmp 21 33 25 4 $GREEN   # life bar
    SlotBox $bmp 134 8; SlotBox $bmp 152 8                  # upgrade slots
    Label $bmp 98 12 30
}

# ============ 1. Template factory (with dropdown selectors) ============
$bmp = New-Canvas $PW $PH
DrawTop $bmp
# slot row y=56
Label $bmp 26 50 20
SlotBox $bmp 26 56   # generic part mould
Label $bmp 44 50 20
SlotBox $bmp 44 56   # life essence solid
ProgressArrow $bmp 86 56 22 18 55      # arrow = progress
SlotBox $bmp 116 56  # output organ template
Label $bmp 116 50 20
# dropdown selectors (both collapsed by default; expanding is a transient overlay in game)
Dropdown $bmp 20 80 66 11 26 $false 0     # organ dropdown (closed)
Dropdown $bmp 92 80 66 11 26 $false 0     # part dropdown (closed)
# player inv moved down
PlayerInv $bmp
$bmp.Save((Join-Path $outDir 'mech_template_factory_layout.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

# ============ 2. Processing factory ============
$bmp = New-Canvas $PW $PH
DrawTop $bmp
Label $bmp 26 50 20; SlotBox $bmp 26 56  # template
Label $bmp 44 50 20; SlotBox $bmp 44 56  # material
Label $bmp 62 50 20; SlotBox $bmp 62 56  # solid
ProgressArrow $bmp 86 56 22 18 40
SlotBox $bmp 116 56
Label $bmp 116 50 20
Label $bmp 20 $INFO_Y 120               # cost text
PlayerInv $bmp
$bmp.Save((Join-Path $outDir 'mech_processing_factory_layout.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

# ============ 3. Assembly station ============
$bmp = New-Canvas $PW $PH
DrawTop $bmp
Label $bmp 20 50 16; SlotBox $bmp 20 56   # CORE
Label $bmp 38 50 16; SlotBox $bmp 38 56   # MODULE
Label $bmp 56 50 16; SlotBox $bmp 56 56   # SHELL
Label $bmp 74 50 16; SlotBox $bmp 74 56   # COOLING
ProgressArrow $bmp 94 56 20 18 30
SlotBox $bmp 116 56
Label $bmp 116 50 20
Label $bmp 20 $INFO_Y 120                # readiness/status text
PlayerInv $bmp
$bmp.Save((Join-Path $outDir 'mech_assembly_station_layout.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Write-Output "Saved 3 layout sketches to: $outDir"
