# 32x32 祭坛与植物家族贴图重制：工业机械 + 生物科技混合风
# 覆盖祭坛石、烈焰花/根（cross 透明）、凋灵根/茎/果实、母祭坛五面，保留原文件名与模型语义
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = $PSScriptRoot
$tex  = Join-Path $root 'common\src\main\resources\assets\akaishi\textures\block'
$prev = Join-Path $root 'gui_layouts\plant_altar_rework_preview.png'
New-Item -ItemType Directory -Force -Path (Split-Path $prev) | Out-Null

function C([string]$h){ [Drawing.ColorTranslator]::FromHtml($h) }
function B([Drawing.Color]$c){ New-Object Drawing.SolidBrush $c }
function P([Drawing.Color]$c,[int]$w){ New-Object Drawing.Pen $c,$w }

function SaveTex($b,[string]$name){
  $b.Save((Join-Path $tex ($name+'.png')),[Drawing.Imaging.ImageFormat]::Png)
  $b.Dispose()
}

# 透明画布（cross 植物用）
function Canvas([int]$s){
  $b = New-Object Drawing.Bitmap $s,$s
  $g = [Drawing.Graphics]::FromImage($b)
  $g.SmoothingMode = 'AntiAlias'
  $g.Clear([Drawing.Color]::Transparent)
  return @($b,$g)
}

# 火焰/花瓣形（尖头朝上，底部在 by）
function Flame($g,[Drawing.Brush]$br,[int]$cx,[int]$by,[int]$w,[int]$h){
  $pts = @(
    (New-Object Drawing.Point $cx,($by-$h)),
    (New-Object Drawing.Point ($cx+[Math]::Floor($w/2)),($by-[Math]::Floor($h*0.5))),
    (New-Object Drawing.Point ($cx+[Math]::Floor($w/4)),($by-[Math]::Floor($h*0.15))),
    (New-Object Drawing.Point $cx,$by),
    (New-Object Drawing.Point ($cx-[Math]::Floor($w/4)),($by-[Math]::Floor($h*0.15))),
    (New-Object Drawing.Point ($cx-[Math]::Floor($w/2)),($by-[Math]::Floor($h*0.5)))
  )
  $g.FillPolygon($br,$pts)
}

function Stem($g,[string]$col,[int]$x,[int]$y0,[int]$y1){
  $g.DrawLine((P (C $col) 3),$x,$y0,$x,$y1)
}

function Leaf($g,[string]$col,[int]$cx,[int]$cy,[int]$w,[int]$h){
  $g.FillEllipse((B (C $col)),($cx-[Math]::Floor($w/2)),($cy-[Math]::Floor($h/2)),$w,$h)
}

# ---- 祭坛石（cube_all，不透明）----
function AltarStone(){
  $b = New-Object Drawing.Bitmap 32,32
  $g = [Drawing.Graphics]::FromImage($b)
  $g.SmoothingMode = 'AntiAlias'
  $g.Clear((C '#3B3B44'))
  $g.FillRectangle((B (C '#4A4A55')),1,1,30,30)
  $p = P (C '#2A2A32') 2
  $g.DrawLine($p,0,11,32,11); $g.DrawLine($p,0,21,32,21)
  $g.DrawLine($p,11,0,11,11); $g.DrawLine($p,21,11,21,21); $g.DrawLine($p,11,21,11,32)
  $p.Dispose()
  $g.DrawEllipse((P (C '#3FE0A8') 2),10,10,12,12)
  $g.FillEllipse((B (C '#7FE8C8')),13,13,6,6)
  $g.Dispose(); SaveTex $b 'akaishi_altar_stone'
}

# ---- 烈焰花（cross）----
function BlazeBloom([string]$name,[int]$petals){
  $r = Canvas 32; $b = $r[0]; $g = $r[1]
  Stem $g '#3E7A3A' 16 30 15
  Leaf $g '#3E7A3A' 10 23 8 4; Leaf $g '#3E7A3A' 22 23 8 4
  $cols = @('#C8481E','#E8781E','#F8C838')
  for($i=0;$i -lt $petals;$i++){
    $cx = 16 + [Math]::Floor((($i - ($petals-1)/2)) * 7)
    $h  = 12 - [Math]::Abs($i - [Math]::Floor($petals/2)) * 2
    Flame $g (B (C $cols[$i % 3])) $cx 18 8 $h
  }
  Flame $g (B (C '#F8E070')) 16 18 5 13
  $g.Dispose(); SaveTex $b $name
}

# ---- 烈焰花根（cross）----
function BlazeRoot([string]$name,[int]$variant){
  $r = Canvas 32; $b = $r[0]; $g = $r[1]
  Stem $g '#2E5E2C' 16 30 18
  $off = 4 + $variant * 2
  Flame $g (B (C '#C8481E')) (16-$off) 30 7 11
  Flame $g (B (C '#E8781E')) (16+$off) 30 7 11
  Flame $g (B (C '#F8C838')) 16 29 6 13
  Leaf $g '#3E7A3A' 16 26 12 4
  $g.Dispose(); SaveTex $b $name
}

# ---- 凋灵根（cross）----
function WitherRoot(){
  $r = Canvas 32; $b = $r[0]; $g = $r[1]
  $p = P (C '#3A2A44') 3
  $g.DrawLine($p,16,30,16,14); $g.DrawLine($p,16,18,9,12); $g.DrawLine($p,16,18,23,12)
  $g.DrawLine($p,16,24,8,22); $g.DrawLine($p,16,24,24,22); $p.Dispose()
  $g.FillEllipse((B (C '#5A3E6A')),13,8,6,6)
  $g.FillEllipse((B (C '#8A5CA8')),14,9,3,3)
  $g.Dispose(); SaveTex $b 'akaishi_wither_root'
}

# ---- 凋灵茎（cross）----
function WitherStem([string]$name,[int]$fruit){
  $r = Canvas 32; $b = $r[0]; $g = $r[1]
  Stem $g '#3A2A44' 16 31 6
  $p = P (C '#2A1E32') 2
  $g.DrawLine($p,16,26,11,24); $g.DrawLine($p,16,20,21,18); $g.DrawLine($p,16,14,11,12); $p.Dispose()
  if($fruit -eq 0){
    $g.FillEllipse((B (C '#5A3E6A')),20,8,7,7)
    $g.FillEllipse((B (C '#8A5CA8')),22,10,3,3)
  } elseif($fruit -eq 1){
    $g.FillEllipse((B (C '#4A3358')),21,9,5,5)
  }
  $g.Dispose(); SaveTex $b $name
}

# ---- 母祭坛（64x64 自定义多面）----
function MotherAltar([string]$name,[string]$kind){
  $b = New-Object Drawing.Bitmap 64,64
  $g = [Drawing.Graphics]::FromImage($b)
  $g.SmoothingMode = 'AntiAlias'
  $g.Clear((C '#26262E'))
  $g.FillRectangle((B (C '#3E3E48')),2,2,60,60)
  $g.FillRectangle((B (C '#4E4E5A')),6,6,52,52)
  $p = P (C '#22222A') 2
  $g.DrawRectangle($p,6,6,51,51); $p.Dispose()
  switch($kind){
    'top' {
      $g.DrawEllipse((P (C '#3FE0A8') 3),14,14,36,36)
      $g.DrawEllipse((P (C '#7FE8C8') 2),22,22,20,20)
      $g.FillEllipse((B (C '#B8FFE4')),28,28,8,8)
      foreach($a in 0,90,180,270){
        $rad = $a * [Math]::PI / 180
        $x1 = 32 + [Math]::Floor(18*[Math]::Cos($rad)); $y1 = 32 + [Math]::Floor(18*[Math]::Sin($rad))
        $x2 = 32 + [Math]::Floor(28*[Math]::Cos($rad)); $y2 = 32 + [Math]::Floor(28*[Math]::Sin($rad))
        $g.DrawLine((P (C '#3FE0A8') 3),$x1,$y1,$x2,$y2)
      }
    }
    'side' {
      for($i=0;$i -lt 4;$i++){ $g.DrawLine((P (C '#2A2A32') 2),8,(12+$i*12),56,(12+$i*12)) }
      $g.FillRectangle((B (C '#3FE0A8')),26,10,12,44)
      $g.FillRectangle((B (C '#7FE8C8')),29,16,6,32)
    }
    'base' {
      for($i=0;$i -lt 5;$i++){ $g.DrawLine((P (C '#2A2A32') 2),8,(10+$i*10),56,(10+$i*10)) }
      $g.DrawEllipse((P (C '#3FE0A8') 2),24,24,16,16)
    }
    'bottom' {
      for($i=0;$i -lt 3;$i++){ $g.DrawLine((P (C '#2A2A32') 2),8,(16+$i*16),56,(16+$i*16)) }
    }
  }
  $g.Dispose(); SaveTex $b $name
}

# ---- 生成 ----
AltarStone
BlazeBloom 'akaishi_blaze_bloom_0' 3
BlazeBloom 'akaishi_blaze_bloom_1' 4
BlazeBloom 'akaishi_blaze_bloom_2' 5
BlazeRoot 'akaishi_blaze_flower_root_0' 0
BlazeRoot 'akaishi_blaze_flower_root_1' 1
BlazeRoot 'akaishi_blaze_flower_root_2' 2
WitherRoot
WitherStem 'akaishi_wither_stem' -1
WitherStem 'akaishi_wither_stem_fruit_ripe' 0
WitherStem 'akaishi_wither_stem_fruit_small' 1
MotherAltar 'akaishi_mother_altar_top' 'top'
MotherAltar 'akaishi_mother_altar_side' 'side'
MotherAltar 'akaishi_mother_altar_base' 'base'
MotherAltar 'akaishi_mother_altar_bottom' 'bottom'

$all = @('akaishi_altar_stone','akaishi_blaze_bloom_0','akaishi_blaze_bloom_1','akaishi_blaze_bloom_2',
  'akaishi_blaze_flower_root_0','akaishi_blaze_flower_root_1','akaishi_blaze_flower_root_2',
  'akaishi_wither_root','akaishi_wither_stem','akaishi_wither_stem_fruit_ripe','akaishi_wither_stem_fruit_small',
  'akaishi_mother_altar_top','akaishi_mother_altar_side','akaishi_mother_altar_base','akaishi_mother_altar_bottom')

$cols = 5
$rows = [Math]::Ceiling($all.Count / $cols)
$sheet = New-Object Drawing.Bitmap (($cols*40)),(($rows*40))
$sg = [Drawing.Graphics]::FromImage($sheet)
$sg.Clear([Drawing.Color]::FromArgb(255,10,13,16))
for($i=0;$i -lt $all.Count;$i++){
  $im = [Drawing.Image]::FromFile((Join-Path $tex ($all[$i]+'.png')))
  $sg.DrawImage($im,(($i % $cols)*40+4),([Math]::Floor($i/$cols)*40+4),32,32)
  $im.Dispose()
}
$sg.Dispose()
$sheet.Save($prev,[Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()
Write-Output "painted $($all.Count) textures; preview $prev"
