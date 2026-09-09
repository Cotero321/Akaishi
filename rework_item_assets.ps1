# 32x32 物品图标重制：工业机械 + 生物科技混合风
# 覆盖 akaishi 装甲套装 / 生命融合套装 / 调试工具 / 手册 / 升级件 / sculk 生命体
# 保留原文件名与模型引用（item/generated + layer0）
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = $PSScriptRoot
$tex  = Join-Path $root 'common\src\main\resources\assets\akaishi\textures\item'
$prev = Join-Path $root 'gui_layouts\item_icons_rework_preview.png'
New-Item -ItemType Directory -Force -Path (Split-Path $prev) | Out-Null

function C([string]$h){ [Drawing.ColorTranslator]::FromHtml($h) }
function B([Drawing.Color]$c){ New-Object Drawing.SolidBrush $c }
function P([Drawing.Color]$c,[int]$w){ New-Object Drawing.Pen $c,$w }

# 透明画布（物品图标）
function NewIcon(){
  $b = New-Object Drawing.Bitmap 32,32
  $g = [Drawing.Graphics]::FromImage($b)
  $g.SmoothingMode = 'AntiAlias'
  $g.Clear([Drawing.Color]::Transparent)
  return @($b,$g)
}
function SaveIcon($b,[string]$name){
  $b.Save((Join-Path $tex ($name+'.png')),[Drawing.Imaging.ImageFormat]::Png)
  $b.Dispose()
}
# 环形（Alternate 填充规则挖洞）
function Ring($g,[Drawing.Color]$c,[int]$x,[int]$y,[int]$w,[int]$h,[int]$iw,[int]$ih){
  $p = New-Object Drawing.Drawing2D.GraphicsPath
  $p.FillMode = [Drawing.Drawing2D.FillMode]::Alternate
  $p.AddEllipse($x,$y,$w,$h)
  $p.AddEllipse(($x+[Math]::Floor(($w-$iw)/2)),($y+[Math]::Floor(($h-$ih)/2)),$iw,$ih)
  $g.FillPath((B $c),$p)
  $p.Dispose()
}
function Poly($g,[Drawing.Color]$c,[object[]]$pts){
  $g.FillPolygon((B $c),$pts)
}

# 调色板：钢铁装甲 / 生命融合装甲
$steel = @{ base='#8C99A6'; mid='#6E7C89'; dark='#39434B'; edge='#232A30'; accent='#3FB8E8'; glow='#9BE4FF' }
$life  = @{ base='#3E7A5C'; mid='#2E5C46'; dark='#1B3A2C'; edge='#12241B'; accent='#4FE08C'; glow='#B8FFD4' }

# ---- 装甲图标（头盔/胸甲/护腿/靴子）----
function ArmorIcon([string]$name,[hashtable]$pal,[string]$kind){
  $r = NewIcon; $b=$r[0]; $g=$r[1]
  $base=C $pal.base; $mid=C $pal.mid; $dark=C $pal.dark; $edge=C $pal.edge; $acc=C $pal.accent; $glow=C $pal.glow
  switch($kind){
    'helmet' {
      $g.FillEllipse((B $base),6,4,20,19)
      $g.FillRectangle((B $base),6,13,20,9)
      $g.FillRectangle((B $mid),8,20,16,5)
      $g.FillRectangle((B $dark),9,12,14,5)
      $g.DrawLine((P $acc 2),11,14,21,14)
      $g.FillRectangle((B $edge),15,3,2,4)
      $g.FillRectangle((B $glow),10,7,4,2)
      $g.DrawRectangle((P $edge 1),6,4,19,18)
    }
    'chest' {
      $g.FillRectangle((B $mid),2,8,7,10)
      $g.FillRectangle((B $mid),23,8,7,10)
      $g.FillRectangle((B $base),8,6,16,21)
      $g.FillRectangle((B $edge),8,6,16,3)
      $g.FillRectangle((B $dark),12,12,8,9)
      $g.FillRectangle((B $acc),14,14,4,5)
      $g.FillRectangle((B $mid),8,24,16,3)
      $g.FillRectangle((B $glow),10,8,3,2)
      $g.FillRectangle((B $glow),19,8,3,2)
    }
    'legs' {
      $g.FillRectangle((B $base),7,6,18,7)
      $g.FillRectangle((B $edge),7,6,18,2)
      $g.FillRectangle((B $base),8,13,7,16)
      $g.FillRectangle((B $base),17,13,7,16)
      $g.FillRectangle((B $mid),8,13,7,3)
      $g.FillRectangle((B $mid),17,13,7,3)
      $g.FillRectangle((B $acc),10,21,3,3)
      $g.FillRectangle((B $acc),19,21,3,3)
      $g.FillRectangle((B $glow),14,9,4,2)
    }
    'boots' {
      $g.FillRectangle((B $base),5,11,9,12)
      $g.FillRectangle((B $base),18,11,9,12)
      $g.FillRectangle((B $mid),3,21,12,7)
      $g.FillRectangle((B $mid),17,21,12,7)
      $g.FillRectangle((B $edge),3,26,12,2)
      $g.FillRectangle((B $edge),17,26,12,2)
      $g.FillRectangle((B $acc),7,14,5,2)
      $g.FillRectangle((B $acc),20,14,5,2)
    }
  }
  $g.Dispose(); SaveIcon $b $name
}

# ---- 生命融合锭 ----
function LifeIngot(){
  $r = NewIcon; $b=$r[0]; $g=$r[1]
  $base=C '#6FAE8C'; $mid=C '#8FD8B0'; $edge=C '#2E5C46'; $acc=C '#4FE08C'; $glow=C '#C8FFE0'
  $front = @((New-Object Drawing.Point 6,24),(New-Object Drawing.Point 11,12),(New-Object Drawing.Point 21,12),(New-Object Drawing.Point 26,24))
  $top   = @((New-Object Drawing.Point 11,12),(New-Object Drawing.Point 14,7),(New-Object Drawing.Point 24,7),(New-Object Drawing.Point 21,12))
  Poly $g $base $front
  Poly $g $mid $top
  $g.DrawPolygon((P $edge 1),$front)
  $g.DrawPolygon((P $edge 1),$top)
  $g.DrawLine((P $glow 2),12,20,20,20)
  $g.DrawLine((P $acc 2),14,16,18,16)
  $g.Dispose(); SaveIcon $b 'akaishi_life_fusion_ingot'
}

# ---- 调试工具（扳手 + 诊断屏）----
function DebugTool(){
  $r = NewIcon; $b=$r[0]; $g=$r[1]
  $base=C '#8C99A6'; $mid=C '#6E7C89'; $edge=C '#232A30'; $acc=C '#3FB8E8'; $glow=C '#9BE4FF'
  $handle = @((New-Object Drawing.Point 6,26),(New-Object Drawing.Point 11,27),(New-Object Drawing.Point 22,14),(New-Object Drawing.Point 18,11))
  Poly $g $base $handle
  $g.DrawPolygon((P $edge 1),$handle)
  Ring $g $base 16 3 13 13 6 6
  $g.DrawLine((P $edge 1),23,12,26,15)
  $g.DrawLine((P $acc 2),10,23,17,15)
  $g.FillRectangle((B $glow),19,6,3,3)
  $g.Dispose(); SaveIcon $b 'akaishi_debug_tool'
}

# ---- 日记本 ----
function Diary(){
  $r = NewIcon; $b=$r[0]; $g=$r[1]
  $cover=C '#3A4A55'; $spine=C '#232A30'; $page=C '#D8D2C0'; $acc=C '#3FB8E8'; $glow=C '#9BE4FF'
  $g.FillRectangle((B $page),21,6,4,20)
  $g.FillRectangle((B $cover),6,4,20,24)
  $g.FillRectangle((B $spine),6,4,4,24)
  $g.DrawRectangle((P $spine 1),6,4,19,23)
  $g.FillRectangle((B $acc),18,15,6,3)
  $g.DrawEllipse((P $glow 2),12,12,7,7)
  $g.FillEllipse((B $glow),14,14,3,3)
  $g.Dispose(); SaveIcon $b 'akaishi_diary'
}

# ---- 升级芯片 ----
function ChipIcon([string]$name,[string]$boardCol,[string]$accentCol,[string]$glowCol,[string]$symbol){
  $r = NewIcon; $b=$r[0]; $g=$r[1]
  $board=C $boardCol; $acc=C $accentCol; $glow=C $glowCol; $edge=C '#12181E'
  $g.FillRectangle((B $edge),5,8,22,16)
  $g.FillRectangle((B $board),7,10,18,12)
  # 引脚
  for($i=0;$i -lt 3;$i++){
    $yy = 12 + $i*4
    $g.FillRectangle((B $glow),4,$yy,3,2)
    $g.FillRectangle((B $glow),25,$yy,3,2)
  }
  switch($symbol){
    'bolt' {
      $p = @((New-Object Drawing.Point 17,12),(New-Object Drawing.Point 12,18),(New-Object Drawing.Point 15,18),(New-Object Drawing.Point 14,22),(New-Object Drawing.Point 20,15),(New-Object Drawing.Point 16,15))
      Poly $g $acc $p
    }
    'speed' {
      for($i=0;$i -lt 2;$i++){
        $ox = 11 + $i*6
        $p = @((New-Object Drawing.Point ($ox+3),12),(New-Object Drawing.Point ($ox+8),16),(New-Object Drawing.Point ($ox+3),20),(New-Object Drawing.Point ($ox+1),20),(New-Object Drawing.Point ($ox+6),16),(New-Object Drawing.Point ($ox+1),12))
        Poly $g $acc $p
      }
    }
    'arrow' {
      $p = @((New-Object Drawing.Point 12,13),(New-Object Drawing.Point 17,13),(New-Object Drawing.Point 17,11),(New-Object Drawing.Point 22,16),(New-Object Drawing.Point 17,21),(New-Object Drawing.Point 17,19),(New-Object Drawing.Point 12,19))
      Poly $g $acc $p
    }
  }
  $g.FillRectangle((B $glow),8,11,4,2)
  $g.Dispose(); SaveIcon $b $name
}

# ---- sculk 生命体 ----
function SculkLifeform(){
  $r = NewIcon; $b=$r[0]; $g=$r[1]
  $d1=C '#0E3A3A'; $d2=C '#12524E'; $vein=C '#2EE6D0'; $core=C '#8CFFF0'
  $g.FillEllipse((B $d1),5,9,22,18)
  $g.FillEllipse((B $d2),8,6,14,13)
  $g.FillEllipse((B $d1),9,14,12,11)
  $g.DrawLine((P $vein 2),9,22,15,14)
  $g.DrawLine((P $vein 2),15,14,22,20)
  $g.DrawLine((P $vein 2),15,14,15,7)
  $g.DrawLine((P $vein 2),11,12,15,14)
  $g.FillEllipse((B $core),13,12,5,5)
  $g.FillEllipse((B $vein),14,13,3,3)
  $g.Dispose(); SaveIcon $b 'sculk_lifeform'
}

# ---- 生成 ----
ArmorIcon 'akaishi_helmet'   $steel 'helmet'
ArmorIcon 'akaishi_chestplate' $steel 'chest'
ArmorIcon 'akaishi_leggings' $steel 'legs'
ArmorIcon 'akaishi_boots'    $steel 'boots'

ArmorIcon 'akaishi_life_fusion_helmet'    $life 'helmet'
ArmorIcon 'akaishi_life_fusion_chestplate' $life 'chest'
ArmorIcon 'akaishi_life_fusion_leggings'  $life 'legs'
ArmorIcon 'akaishi_life_fusion_boots'     $life 'boots'
LifeIngot

DebugTool
Diary
ChipIcon 'akaishi_machine_energy_upgrade' '#2E5C7A' '#3FB8E8' '#9BE4FF' 'bolt'
ChipIcon 'akaishi_machine_speed_upgrade'  '#6A3E22' '#E8903F' '#FFC97F' 'speed'
ChipIcon 'akaishi_speed_upgrade'          '#5A4A22' '#E8C43F' '#FFE79B' 'arrow'
SculkLifeform

$all = @('akaishi_helmet','akaishi_chestplate','akaishi_leggings','akaishi_boots',
  'akaishi_life_fusion_helmet','akaishi_life_fusion_chestplate','akaishi_life_fusion_leggings','akaishi_life_fusion_boots','akaishi_life_fusion_ingot',
  'akaishi_debug_tool','akaishi_diary','akaishi_machine_energy_upgrade','akaishi_machine_speed_upgrade','akaishi_speed_upgrade','sculk_lifeform')

$cols = 5
$rows = [Math]::Ceiling($all.Count / $cols)
$cell = 72
$sheet = New-Object Drawing.Bitmap (($cols*$cell)),(($rows*$cell))
$sg = [Drawing.Graphics]::FromImage($sheet)
$sg.Clear([Drawing.Color]::FromArgb(255,10,13,16))
$sg.InterpolationMode = 'NearestNeighbor'
for($i=0;$i -lt $all.Count;$i++){
  $im = [Drawing.Image]::FromFile((Join-Path $tex ($all[$i]+'.png')))
  $sg.DrawImage($im,(($i % $cols)*$cell+4),([Math]::Floor($i/$cols)*$cell+4),64,64)
  $im.Dispose()
}
$sg.Dispose()
$sheet.Save($prev,[Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()
Write-Output "painted $($all.Count) textures; preview $prev"
