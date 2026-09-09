$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Join-Path $PSScriptRoot 'common/src/main/resources/assets/akaishi/textures/block'
New-Item -ItemType Directory -Force -Path $root | Out-Null
function New-Tex([string]$name, [scriptblock]$draw, [bool]$glass=$false) {
  $bmp = New-Object Drawing.Bitmap 64,64
  $g = [Drawing.Graphics]::FromImage($bmp); $g.SmoothingMode = 'AntiAlias'
  if ($glass) { $g.Clear([Drawing.Color]::FromArgb(72,32,180,185)); $p=New-Object Drawing.Pen ([Drawing.Color]::FromArgb(180,150,255,245)),2; $g.DrawRectangle($p,2,2,59,59); $g.DrawLine($p,5,58,58,5); $p.Dispose() }
  else { $g.Clear([Drawing.Color]::FromArgb(255,24,30,35)); & $draw $g }
  $g.Dispose(); $bmp.Save((Join-Path $root $name),[Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
}
$metal = [Drawing.Color]::FromArgb(255,58,72,78); $edge=[Drawing.Color]::FromArgb(255,112,130,126); $cyan=[Drawing.Color]::FromArgb(255,50,220,190); $amber=[Drawing.Color]::FromArgb(255,230,150,45); $violet=[Drawing.Color]::FromArgb(255,150,65,220)
$drawMetal = { param($g) $b=New-Object Drawing.SolidBrush $metal; $p=New-Object Drawing.Pen $edge,2; $g.FillRectangle($b,4,4,56,56); 0..7 | % { $g.DrawLine($p,4,($_*8+4),60,($_*8+4)) }; $b.Dispose();$p.Dispose() }
New-Tex 'akaishi_reactor_controller.png' { param($g) & $drawMetal $g; $b=New-Object Drawing.SolidBrush $cyan; $g.FillRectangle($b,12,14,40,18); $g.FillEllipse($b,27,39,10,10); $b.Dispose() }
New-Tex 'akaishi_reactor_controller_formed.png' { param($g) & $drawMetal $g; $b=New-Object Drawing.SolidBrush $violet; $g.FillRectangle($b,8,8,48,10); $g.FillRectangle($b,8,46,48,10); $b.Dispose(); $p=New-Object Drawing.Pen $cyan,3; $g.DrawEllipse($p,20,20,24,24);$p.Dispose() }
foreach($n in 'shell','core','fuel_rod','cooler','exhausted_barrel'){ New-Tex "akaishi_reactor_$n.png" { param($g) & $drawMetal $g; $b=New-Object Drawing.SolidBrush $(if($n -eq 'core'){$violet}elseif($n -eq 'fuel_rod'){$amber}else{$cyan}); $g.FillEllipse($b,14,14,36,36); $b.Dispose() } }
foreach($n in 'fuel_port','energy_output','waste_port'){ New-Tex "akaishi_reactor_$n.png" { param($g) & $drawMetal $g; $b=New-Object Drawing.SolidBrush $(if($n -eq 'fuel_port'){$amber}elseif($n -eq 'waste_port'){$violet}else{$cyan}); $g.FillRectangle($b,16,16,32,32); $p=New-Object Drawing.Pen $edge,3; $g.DrawEllipse($p,22,22,20,20);$p.Dispose();$b.Dispose() } }
New-Tex 'akaishi_reactor_structure_glass.png' {} $true
Write-Output 'Reactor textures generated.'
