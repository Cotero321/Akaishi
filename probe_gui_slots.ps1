# Probe GUI textures: detect 18x18 slot frames (16x16 inner pure #8B8B8B) + panel bounds.
# ASCII-only (PS5.1 GBK safety).
Add-Type -AssemblyName System.Drawing

$guiDir = Join-Path $PSScriptRoot 'common\src\main\resources\assets\akaishi\textures\gui'

function Read-Pixels {
    param($bmp)
    $rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
    $fmt = [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    $data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, $fmt)
    $len = $data.Stride * $bmp.Height
    $buf = New-Object byte[] $len
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $buf, 0, $len)
    $bmp.UnlockBits($data)
    return @{ buf = $buf; stride = $data.Stride; w = $bmp.Width; h = $bmp.Height }
}

Get-ChildItem -Path $guiDir -Filter *.png | Sort-Object Name | ForEach-Object {
    $bmp = [System.Drawing.Bitmap]::FromFile($_.FullName)
    $img = Read-Pixels $bmp
    $w = $img.w; $h = $img.h
    $mask = New-Object 'bool[]' ($w * $h)
    for ($y = 0; $y -lt $h; $y++) {
        $row = $y * $w
        for ($x = 0; $x -lt $w; $x++) {
            $i = $y * $img.stride + $x * 4
            if ($img.buf[$i+2] -eq 139 -and $img.buf[$i+1] -eq 139 -and $img.buf[$i] -eq 139) { $mask[$row + $x] = $true }
        }
    }
    # candidate top-left corners of 16x16 fully-139 blocks
    $slots = New-Object System.Collections.ArrayList
    for ($y = 0; $y -le $h - 16; $y++) {
        for ($x = 0; $x -le $w - 16; $x++) {
            if (-not $mask[$y * $w + $x]) { continue }
            if ($x -gt 0 -and $mask[$y * $w + $x - 1]) { continue }
            if ($y -gt 0 -and $mask[($y - 1) * $w + $x]) { continue }
            $ok = $true
            for ($j = 0; $j -lt 16 -and $ok; $j++) {
                $base = ($y + $j) * $w + $x
                for ($i = 0; $i -lt 16; $i++) { if (-not $mask[$base + $i]) { $ok = $false; break } }
            }
            if ($ok) { [void]$slots.Add(("{0},{1}" -f $x, $y)) }
        }
    }
    # panel bounds: bbox of pixels != panel gray(198) and != pure 198
    $minX = $w; $minY = $h; $maxX = -1; $maxY = -1
    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $i = $y * $img.stride + $x * 4
            $r = $img.buf[$i+2]; $g = $img.buf[$i+1]; $b = $img.buf[$i]
            if ($r -ne 198 -or $g -ne 198 -or $b -ne 198) {
                if ($x -lt $minX) { $minX = $x }; if ($x -gt $maxX) { $maxX = $x }
                if ($y -lt $minY) { $minY = $y }; if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }
    Write-Output ("{0} | {1}x{2} | panel=[{3},{4}]-[{5},{6}] | slots({7})={8}" -f `
        $_.Name, $w, $h, $minX, $minY, $maxX, $maxY, $slots.Count, ($slots -join ' '))
    $bmp.Dispose()
}
