# Generate 32x32 ore-base and dust item textures; fail fast.
Add-Type -AssemblyName System.Drawing
$targetDir=Join-Path $PSScriptRoot 'common\src\main\resources\assets\akaishi\textures\item'; New-Item -ItemType Directory -Path $targetDir -Force -ErrorAction Stop|Out-Null
function P($b,$x,$y,$a,$r,$g,$bl){$b.SetPixel($x,$y,[Drawing.Color]::FromArgb($a,$r,$g,$bl))}
function Save($b,$n){$o=New-Object Drawing.Bitmap 32,32;$g=[Drawing.Graphics]::FromImage($o);$g.InterpolationMode='NearestNeighbor';$g.DrawImage($b,0,0,32,32);$g.Dispose();$o.Save((Join-Path $targetDir $n),[Drawing.Imaging.ImageFormat]::Png);$o.Dispose();$b.Dispose()}
function Dust($n,$d,$m,$l,$spark){$b=New-Object Drawing.Bitmap 16,16;$pts=@(@(4,7),@(5,5),@(7,4),@(9,5),@(11,6),@(12,8),@(10,10),@(8,11),@(6,10),@(4,9),@(3,8));foreach($p in $pts){P $b $p[0] $p[1] 255 $m[0] $m[1] $m[2]};foreach($p in @(@(5,6),@(7,5),@(9,6),@(10,8),@(8,9),@(6,9))){P $b $p[0] $p[1] 255 $l[0] $l[1] $l[2]};foreach($p in @(@(2,7),@(3,11),@(5,12),@(9,12),@(12,11),@(13,7))){P $b $p[0] $p[1] 255 $d[0] $d[1] $d[2]};if($spark){P $b 8 5 255 $l[0] $l[1] $l[2];P $b 10 9 255 $l[0] $l[1] $l[2]};Save $b $n}
function Base($n,$i){$b=New-Object Drawing.Bitmap 16,16;for($x=0;$x-lt 16;$x++){for($y=0;$y-lt 16;$y++){if($y-lt 5){$c=@(205,211,208)}elseif($y-gt 11){$c=@(48,54,60)}else{$c=@(120,128,132)};P $b $x $y 255 $c[0] $c[1] $c[2]}};for($x=3;$x-le 12;$x++){for($y=5;$y-le 10;$y++){P $b $x $y 255 $i[0] $i[1] $i[2]}};Save $b $n}
Dust 'akaishi_dust.png' @(70,35,30) @(145,55,42) @(205,88,55) $false
Dust 'coal_dust.png' @(20,20,22) @(45,45,48) @(78,78,80) $false
Dust 'iron_dust.png' @(55,58,60) @(125,128,130) @(185,188,190) $false
Dust 'copper_dust.png' @(70,42,30) @(150,78,48) @(205,125,75) $false
Dust 'gold_dust.png' @(80,58,20) @(175,135,42) @(230,190,80) $false
Dust 'lapis_dust.png' @(25,40,85) @(48,75,150) @(92,120,205) $false
Dust 'diamond_dust.png' @(35,105,120) @(75,175,190) @(160,230,225) $true
Dust 'emerald_dust.png' @(20,75,52) @(35,135,82) @(100,195,125) $false
Dust 'quartz_dust.png' @(100,96,82) @(175,170,150) @(235,230,205) $true
Dust 'netherite_dust.png' @(35,28,34) @(82,58,65) @(140,105,110) $false
Dust 'obsidian_dust.png' @(18,18,30) @(42,35,65) @(78,60,105) $false
Base 'cooling_base.png' @(80,150,122);Base 'coal_ore_base.png' @(40,44,48);Base 'iron_ore_base.png' @(150,145,140);Base 'copper_ore_base.png' @(160,82,48);Base 'gold_ore_base.png' @(210,170,55);Base 'redstone_ore_base.png' @(150,45,42);Base 'lapis_ore_base.png' @(48,78,160);Base 'diamond_ore_base.png' @(70,180,190);Base 'emerald_ore_base.png' @(45,165,92);Base 'quartz_ore_base.png' @(210,190,165);Base 'netherite_ore_base.png' @(110,75,82);Base 'akaishi_ore_base.png' @(165,65,50)
