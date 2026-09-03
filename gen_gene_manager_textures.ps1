# Generate distinct purple-texture set for the Gene Manager block
# Base art = gene analyzer top/side, recolored to a purple 'gene bank' look
# plus horizontal mirror so the two machines never read as identical.
# Pure ASCII content only (PS 5.1 GBK safety).
Add-Type -AssemblyName System.Drawing
$blockDir = Join-Path $PSScriptRoot 'common\src\main\resources\assets\akaishi\textures\block'
if (-not (Test-Path $blockDir)) { Write-Error 'textures/block dir not found'; exit 1 }

# analyzer color -> manager color table (all opaque)
$map = @{
    '2A5A2A' = '3A2470'   # frame dark green -> dark purple
    '142A14' = '1A1038'   # panel bg -> deep purple
    '8AE08A' = 'C6A8FF'   # light green -> light purple
    '4EC04E' = '8A5CF0'   # mid green -> mid purple
    'C83232' = 'E8A860'   # red marker -> amber marker (extra contrast)
    '0E1A0E' = '100A24'   # vent dark -> near-black purple
}

function Convert-ToArgb($hex) {
    $r = [Convert]::ToInt32($hex.Substring(0, 2), 16)
    $g = [Convert]::ToInt32($hex.Substring(2, 2), 16)
    $b = [Convert]::ToInt32($hex.Substring(4, 2), 16)
    return [System.Drawing.Color]::FromArgb(255, $r, $g, $b)
}

function New-RecoloredMirror($srcPath, $dstPath) {
    $src = New-Object System.Drawing.Bitmap($srcPath)
    $w = $src.Width; $h = $src.Height
    $dst = New-Object System.Drawing.Bitmap($w, $h)
    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $p = $src.GetPixel($x, $y)
            if ($p.A -eq 0) { $dst.SetPixel($w - 1 - $x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0)) }
            else {
                $hex = '{0:X2}{1:X2}{2:X2}' -f $p.R, $p.G, $p.B
                $c = if ($map.ContainsKey($hex)) { Convert-ToArgb $map[$hex] } else { [System.Drawing.Color]::FromArgb(255, $p.R, $p.G, $p.B) }
                $dst.SetPixel($w - 1 - $x, $y, $c)
            }
        }
    }
    $tmp = $dstPath + '.tmp.png'
    $dst.Save($tmp, [System.Drawing.Imaging.ImageFormat]::Png)
    $dst.Dispose(); $src.Dispose()
    Copy-Item -Force $tmp $dstPath
    Remove-Item -Force $tmp
}

New-RecoloredMirror (Join-Path $blockDir 'akaishi_gene_analyzer_top.png') (Join-Path $blockDir 'akaishi_gene_manager_top.png')
New-RecoloredMirror (Join-Path $blockDir 'akaishi_gene_analyzer_side.png') (Join-Path $blockDir 'akaishi_gene_manager_side.png')
Write-Output 'gene manager textures generated'
