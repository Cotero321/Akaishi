param([switch]$PreviewOnly)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.Drawing
$root=$PSScriptRoot
$block=Join-Path $root 'common\src\main\resources\assets\akaishi\textures\block'
$preview=Join-Path $root 'gui_layouts'
$node=(Get-Command node -ErrorAction Stop).Source
& $node (Join-Path $root 'validate_purifier_assets.js')
if($LASTEXITCODE -ne 0){throw 'Preflight failed'}
function Color($red,$green,$blue,$alpha=255){[Drawing.Color]::FromArgb($alpha,[Math]::Max(0,[Math]::Min(255,$red)),[Math]::Max(0,[Math]::Min(255,$green)),[Math]::Max(0,[Math]::Min(255,$blue)))}
function NewTexture($baseColor,$accentColor,$name,$mode,$sourceName){
 $path=Join-Path $block $sourceName; $source=[Drawing.Bitmap]::FromFile($path); $bmp=New-Object Drawing.Bitmap(64,64); $graphics=[Drawing.Graphics]::FromImage($bmp)
 try { for($y=0;$y -lt 64;$y++){for($x=0;$x -lt 64;$x++){ $v=((([int]($x/4)*7+[int]($y/4)*11)%5)-2); $bmp.SetPixel($x,$y,(Color ($baseColor.R+$v*2) ($baseColor.G+$v*2) ($baseColor.B+$v*2))) }}
  if($mode -eq 'top'){for($y=12;$y -lt 52;$y++){for($x=12;$x -lt 52;$x++){if(([Math]::Max([Math]::Abs($x-31.5),[Math]::Abs($y-31.5))) -lt 20){$bmp.SetPixel($x,$y,$accentColor)}}}}
  elseif($mode -eq 'side'){for($x=16;$x -lt 50;$x+=10){$graphics.FillRectangle((New-Object Drawing.SolidBrush($accentColor)),$x,18,5,28)}}
  else {$graphics.FillRectangle((New-Object Drawing.SolidBrush($accentColor)),8,8,48,5)}
  for($y=0;$y -lt 64;$y++){for($x=0;$x -lt 64;$x++){ $alphaValue=$source.GetPixel([Math]::Min($source.Width-1,[int]($x*$source.Width/64)),[Math]::Min($source.Height-1,[int]($y*$source.Height/64))).A; $pixel=$bmp.GetPixel($x,$y);$bmp.SetPixel($x,$y,(Color $pixel.R $pixel.G $pixel.B $alphaValue)) }}
  $source.Dispose(); $bmp.Save((Join-Path $preview $name),[Drawing.Imaging.ImageFormat]::Png);if(!$PreviewOnly){$bmp.Save((Join-Path $block $name),[Drawing.Imaging.ImageFormat]::Png)}
 } finally {$graphics.Dispose();$bmp.Dispose();$source.Dispose()}
}
$steel=Color 48 57 70;$decay=Color 66 43 48;$accent=Color 230 121 83
foreach($family in @('purifier','life_purifier')){foreach($face in @('side','top','bottom')){NewTexture $steel $accent ('akaishi_'+$family+'_'+$face+'.png') $face ('akaishi_'+$family+'_'+$face+'.png')}}
if(!$PreviewOnly){& $node (Join-Path $root 'validate_purifier_assets.js') --check-output;if($LASTEXITCODE -ne 0){throw 'Postflight failed'}}
Write-Output 'Generated purifier previews.'
