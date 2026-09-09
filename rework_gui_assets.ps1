# Rework GUI textures -> industrial-mech + bio-tech panel style.
# Slot frames are kept pixel-perfect from the source (no slot shift);
# only the panel body, edge colors and per-machine theme accent change.
# ASCII-only (PS5.1 GBK safety).
Add-Type -AssemblyName System.Drawing

$root   = $PSScriptRoot
$guiDir = Join-Path $root 'common\src\main\resources\assets\akaishi\textures\gui'
$outDir = Join-Path $root 'gui_layouts'
New-Item -ItemType Directory -Path $outDir -Force -ErrorAction SilentlyContinue | Out-Null

$PANEL_OLD      = 198
$EDGE_DARK_OLD  = 55
$EDGE_LIGHT_OLD = 255
$SLOT           = 139
$SEAM_DARK      = 182
$SEAM_LIGHT      = 208

# theme accent per texture (r,g,b) - energy=amber, life=bio-teal, wireless=steel-blue
$theme = @{}
$theme['akaishi_auto_collector']     = @(150, 92, 42)
$theme['akaishi_energy_cell']        = @(150, 92, 42)
$theme['akaishi_energy_generator']   = @(150, 92, 42)
$theme['akaishi_fuel_canner']        = @(150, 92, 42)
$theme['akaishi_fusion_controller']  = @(150, 92, 42)
$theme['akaishi_life_struct']        = @(42, 132, 118)
$theme['akaishi_purifier']           = @(42, 132, 118)
$theme['akaishi_purifier_matrix']    = @(42, 132, 118)
$theme['akaishi_reactor_controller'] = @(150, 92, 42)
$theme['akaishi_reactor_fuel_port']  = @(150, 92, 42)
$theme['akaishi_super_generator']    = @(150, 92, 42)
$theme['akaishi_wireless_terminal']  = @(62, 100, 156)

function Read-Pixels {
    param($bmp)
    $rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
    $fmt  = [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    $data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, $fmt)
    $stride = $data.Stride
    $len  = $stride * $bmp.Height
    $buf  = New-Object byte[] $len
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $buf, 0, $len)
    $bmp.UnlockBits($data)
    return @{ buf = $buf; stride = $stride }
}

# Paint a 1px themed rectangle on pixels that were plain panel gray in the source.
function Paint-ThemeBox {
    param($out, $stride, $isPanel, $x0, $y0, $x1, $y1, $tc)
    for ($x = $x0; $x -le $x1; $x++) {
        foreach ($y in @($y0, $y1)) {
            if ($x -lt 0 -or $x -gt 255 -or $y -lt 0 -or $y -gt 255) { continue }
            if ($isPanel[$y * 256 + $x] -eq 0) { continue }
            $di = $y * $stride + $x * 4
            $out[$di] = $tc[2]; $out[$di + 1] = $tc[1]; $out[$di + 2] = $tc[0]
        }
    }
    for ($y = $y0; $y -le $y1; $y++) {
        foreach ($x in @($x0, $x1)) {
            if ($x -lt 0 -or $x -gt 255 -or $y -lt 0 -or $y -gt 255) { continue }
            if ($isPanel[$y * 256 + $x] -eq 0) { continue }
            $di = $y * $stride + $x * 4
            $out[$di] = $tc[2]; $out[$di + 1] = $tc[1]; $out[$di + 2] = $tc[0]
        }
    }
}

$files = Get-ChildItem -Path $guiDir -Filter *.png | Sort-Object Name
$reworked = New-Object System.Collections.ArrayList
$totalSlots = 0

foreach ($f in $files) {
    $name = [System.IO.Path]::GetFileNameWithoutExtension($f.Name)

    $srcBmp = [System.Drawing.Bitmap]::FromFile($f.FullName)
    $src    = Read-Pixels $srcBmp
    $srcBmp.Dispose()   # release the file lock before writing back to the same path

    # ---- 1. detect slot inner blocks (139 gray rectangles >= 12x12) ----
    $slots = New-Object System.Collections.ArrayList
    for ($y = 0; $y -lt 256; $y++) {
        for ($x = 0; $x -lt 256; $x++) {
            $si = $y * $src.stride + $x * 4
            if ($src.buf[$si + 2] -ne $SLOT -or $src.buf[$si + 1] -ne $SLOT -or $src.buf[$si] -ne $SLOT) { continue }
            if ($x -gt 0) {
                $li = $y * $src.stride + ($x - 1) * 4
                if ($src.buf[$li + 2] -eq $SLOT -and $src.buf[$li + 1] -eq $SLOT -and $src.buf[$li] -eq $SLOT) { continue }
            }
            if ($y -gt 0) {
                $ui = ($y - 1) * $src.stride + $x * 4
                if ($src.buf[$ui + 2] -eq $SLOT -and $src.buf[$ui + 1] -eq $SLOT -and $src.buf[$ui] -eq $SLOT) { continue }
            }
            $w = 0
            while (($x + $w) -lt 256) {
                $i = $y * $src.stride + ($x + $w) * 4
                if ($src.buf[$i + 2] -eq $SLOT -and $src.buf[$i + 1] -eq $SLOT -and $src.buf[$i] -eq $SLOT) { $w++ } else { break }
            }
            $h = 0
            while (($y + $h) -lt 256) {
                $i = ($y + $h) * $src.stride + $x * 4
                if ($src.buf[$i + 2] -eq $SLOT -and $src.buf[$i + 1] -eq $SLOT -and $src.buf[$i] -eq $SLOT) { $h++ } else { break }
            }
            if ($w -ge 12 -and $h -ge 12) {
                $cx = $x + [Math]::Floor($w / 2)
                $cy = $y + [Math]::Floor($h / 2)
                [void]$slots.Add(@(($cx - 9), ($cy - 9)))
            }
        }
    }
    $totalSlots += $slots.Count

    # ---- 2. build reworked pixel buffer ----
    $outBmp = New-Object System.Drawing.Bitmap(256, 256, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $rect   = New-Object System.Drawing.Rectangle(0, 0, 256, 256)
    $dst    = $outBmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::WriteOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $out    = New-Object byte[] ($dst.Stride * 256)
    $isPanel = New-Object byte[] 65536

    for ($y = 0; $y -lt 256; $y++) {
        for ($x = 0; $x -lt 256; $x++) {
            $si = $y * $src.stride + $x * 4
            $r = $src.buf[$si + 2]; $g = $src.buf[$si + 1]; $b = $src.buf[$si]
            $nr = $r; $ng = $g; $nb = $b
            if ($r -eq $PANEL_OLD -and $g -eq $PANEL_OLD -and $b -eq $PANEL_OLD) {
                # brushed metal body: horizontal grain + slight top-lit gradient + engraved plate seams
                $grain = (($x * 37 + $y * 91) % 5) - 2
                $grad  = [int][Math]::Round(4 * (1.0 - $y / 255.0)) - 2
                $c = $PANEL_OLD + $grain + $grad
                $my = $y % 32
                if ($my -eq 16) { $c = $SEAM_DARK }
                elseif ($my -eq 17) { $c = $SEAM_LIGHT }
                $nr = $c; $ng = $c; $nb = $c
                $isPanel[$y * 256 + $x] = 1
            }
            $di = $y * $dst.Stride + $x * 4
            $out[$di] = $nb; $out[$di + 1] = $ng; $out[$di + 2] = $nr; $out[$di + 3] = 255
        }
    }

    # ---- 3. theme accent frame around each slot cluster (y-clustered) ----
    $tc = $theme[$name]
    if ($tc -and $slots.Count -gt 0) {
        $sorted = $slots | Sort-Object { $_[1] }, { $_[0] }
        $groups = New-Object System.Collections.ArrayList
        $cur = New-Object System.Collections.ArrayList
        $lastY = -999
        foreach ($s in $sorted) {
            if ($cur.Count -gt 0 -and ($s[1] - $lastY) -gt 26) {
                [void]$groups.Add($cur); $cur = New-Object System.Collections.ArrayList
            }
            [void]$cur.Add($s); $lastY = $s[1]
        }
        if ($cur.Count -gt 0) { [void]$groups.Add($cur) }
        foreach ($grp in $groups) {
            $minX = ($grp | ForEach-Object { $_[0] } | Measure-Object -Minimum).Minimum
            $minY = ($grp | ForEach-Object { $_[1] } | Measure-Object -Minimum).Minimum
            $maxX = ($grp | ForEach-Object { $_[0] } | Measure-Object -Maximum).Maximum
            $maxY = ($grp | ForEach-Object { $_[1] } | Measure-Object -Maximum).Maximum
            Paint-ThemeBox $out $dst.Stride $isPanel ($minX - 3) ($minY - 3) ($maxX + 20) ($maxY + 20) $tc
        }
    }

    [System.Runtime.InteropServices.Marshal]::Copy($out, 0, $dst.Scan0, $out.Length)
    $outBmp.UnlockBits($dst)
    $outBmp.Save($f.FullName, [System.Drawing.Imaging.ImageFormat]::Png)
    $outBmp.Dispose()

    [void]$reworked.Add(@{ name = $name; slots = $slots.Count })
}

# ---- 4. preview sheet: 4 columns x 4 rows of the actually-visible 176x200 area ----
$cellW = 176; $cellH = 200; $cols = 4
$rows = [int][Math]::Ceiling($reworked.Count / $cols)
$sheet = New-Object System.Drawing.Bitmap(($cols * $cellW), ($rows * $cellH))
$gfx = [System.Drawing.Graphics]::FromImage($sheet)
$gfx.Clear([System.Drawing.Color]::FromArgb(255, 32, 32, 32))
$srcRect = New-Object System.Drawing.Rectangle(0, 0, $cellW, $cellH)
$i = 0
foreach ($r in $reworked) {
    $p = Join-Path $guiDir ($r.name + '.png')
    $bmp = [System.Drawing.Bitmap]::FromFile($p)
    $dx = ($i % $cols) * $cellW
    $dy = [Math]::Floor($i / $cols) * $cellH
    $gfx.DrawImage($bmp, $dx, $dy, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
    $bmp.Dispose()
    $i++
}
$gfx.Dispose()
$sheet.Save((Join-Path $outDir 'gui_rework_preview.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()

Write-Output ("reworked {0} gui textures, slot frames kept: {1}" -f $reworked.Count, $totalSlots)
foreach ($r in $reworked) { Write-Output ("  {0}: slots={1}" -f $r.name, $r.slots) }
Write-Output ("preview: {0}" -f (Join-Path $outDir 'gui_rework_preview.png'))
