# 64x64 能量机器家族贴图重制：工业机械 + 生物科技混合风
# 覆盖能量聚合器/锻造台/矩阵/序列器/超级发电机/创造模式能量单元，保留原文件名与模型语义
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = $PSScriptRoot
$tex  = Join-Path $root 'common\src\main\resources\assets\akaishi\textures\block'
$prev = Join-Path $root 'gui_layouts\energy_rework_preview.png'
New-Item -ItemType Directory -Force -Path (Split-Path $prev) | Out-Null

function C([string]$h){ [Drawing.ColorTranslator]::FromHtml($h) }
function B([Drawing.Color]$c){ New-Object Drawing.SolidBrush $c }
function P([Drawing.Color]$c,[int]$w){ New-Object Drawing.Pen $c,$w }

# 金属外壳基底：外框 + 内板 + 四角铆钉
function Plate($g,[string]$frame,[string]$panel){
  $g.Clear((C '#141A20'))
  $g.FillRectangle((B (C $frame)),3,3,58,58)
  $g.FillRectangle((B (C $panel)),8,8,48,48)
  $rv = B (C '#8FA0AE')
  foreach($x in 5,54){ foreach($y in 5,54){ $g.FillEllipse($rv,$x,$y,5,5) } }
  $rv.Dispose()
}

function Ribs($g,[string]$col,[int]$x,[int]$y,[int]$w,[int]$h,[int]$n,[bool]$vert){
  $p = P (C $col) 2
  for($i=1;$i -lt $n;$i++){
    if($vert){ $xx=[Math]::Floor($x+$w*$i/$n); $g.DrawLine($p,$xx,$y,$xx,($y+$h)) }
    else     { $yy=[Math]::Floor($y+$h*$i/$n); $g.DrawLine($p,$x,$yy,($x+$w),$yy) }
  }
  $p.Dispose()
}

function Screen($g,[string]$col,[int]$x,[int]$y,[int]$w,[int]$h){
  $g.FillRectangle((B (C '#08141A')),($x-2),($y-2),($w+4),($h+4))
  $g.FillRectangle((B (C $col)),$x,$y,$w,$h)
  $p = P (C '#08141A') 1
  for($yy=$y+2;$yy -lt ($y+$h);$yy+=3){ $g.DrawLine($p,$x,$yy,($x+$w),$yy) }
  $p.Dispose()
}

function Port($g,[string]$col,[int]$cx,[int]$cy,[int]$r){
  $g.FillEllipse((B (C '#0C1216')),($cx-$r-3),($cy-$r-3),(($r+3)*2),(($r+3)*2))
  $g.FillEllipse((B (C $col)),($cx-$r),($cy-$r),($r*2),($r*2))
  $hr = [Math]::Max(2,[Math]::Floor($r/2))
  $g.FillEllipse((B (C '#EAFBFF')),($cx-$hr),($cy-$hr),($hr*2),($hr*2))
}

# 箭头：down=true 指向中心（输入），false 指向外（输出）
function Arrow($g,[string]$col,[int]$cx,[int]$cy,[bool]$down){
  $p = P (C $col) 4
  $g.DrawLine($p,$cx,($cy-9),$cx,($cy+9))
  if($down){ $g.DrawLine($p,$cx,($cy+9),($cx-6),($cy+2)); $g.DrawLine($p,$cx,($cy+9),($cx+6),($cy+2)) }
  else     { $g.DrawLine($p,$cx,($cy-9),($cx-6),($cy-2)); $g.DrawLine($p,$cx,($cy-9),($cx+6),($cy-2)) }
  $p.Dispose()
}

function Paint([string]$name,[string]$accent,[string]$kind,[bool]$bright=$false){
  $b = New-Object Drawing.Bitmap 64,64
  $g = [Drawing.Graphics]::FromImage($b)
  $g.SmoothingMode = 'AntiAlias'
  $frame = if($bright){'#6E8492'}else{'#5C6C77'}
  $panel = if($bright){'#243039'}else{'#1B242B'}
  switch($kind){
    'casing' {
      Plate $g $frame $panel
      $p = P (C $accent) 3
      $g.DrawLine($p,32,8,32,24); $g.DrawLine($p,32,40,32,56)
      $g.DrawLine($p,8,32,24,32); $g.DrawLine($p,40,32,56,32); $p.Dispose()
      Port $g $accent 32 32 8
    }
    'core' {
      Plate $g $frame $panel
      $g.DrawEllipse((P (C $accent) 2),10,10,44,44)
      $g.DrawEllipse((P (C $accent) 2),16,16,32,32)
      Port $g $accent 32 32 10
    }
    'assembly' {
      Plate $g $frame $panel
      foreach($x in 14,28,42){ foreach($y in 14,28,42){ Port $g $accent ($x+5) ($y+5) 4 } }
    }
    'matrix' {
      Plate $g $frame $panel
      $p = P (C $accent) 2
      for($x=12;$x -le 52;$x+=8){ for($y=12;$y -le 52;$y+=8){ $g.DrawEllipse($p,$x,$y,3,3) } }
      $p.Dispose()
    }
    'controller' {
      Plate $g $frame $panel
      Screen $g $accent 12 10 40 18
      Ribs $g '#6E8290' 12 34 40 8 2 $false
      Port $g $accent 20 50 5
      Port $g '#E0A64A' 32 50 5
      Port $g '#D85B6B' 44 50 5
    }
    'glass' {
      $g.Clear([Drawing.Color]::FromArgb(255,10,16,20))
      $g.FillRectangle((B ([Drawing.Color]::FromArgb(110,60,200,210))),3,3,58,58)
      $p = P ([Drawing.Color]::FromArgb(200,150,240,240)) 2
      $g.DrawRectangle($p,3,3,57,57); $g.DrawLine($p,6,56,56,6); $p.Dispose()
    }
    'port_in' {
      Plate $g $frame $panel
      $g.FillRectangle((B (C $accent)),14,16,36,32)
      $g.DrawRectangle((P (C '#0C1216') 3),14,16,36,32)
      Arrow $g '#0C1216' 32 32 $true
    }
    'port_out' {
      Plate $g $frame $panel
      $g.FillRectangle((B (C $accent)),14,16,36,32)
      $g.DrawRectangle((P (C '#0C1216') 3),14,16,36,32)
      Arrow $g '#0C1216' 32 32 $false
    }
    'barrel' {
      Plate $g $frame $panel
      $p = P (C $accent) 6
      $g.DrawLine($p,10,22,54,22); $g.DrawLine($p,10,42,54,42); $p.Dispose()
      Port $g $accent 32 32 7
    }
    'cell_top' {
      Plate $g $frame $panel
      Ribs $g '#6E8290' 12 12 40 40 4 $false
      for($i=0;$i -lt 4;$i++){ $g.FillRectangle((B (C $accent)),14,(14+$i*10),36,6) }
    }
    'cell_side' {
      Plate $g $frame $panel
      Ribs $g '#6E8290' 10 12 44 40 5 $true
      $g.FillRectangle((B (C $accent)),10,10,44,3)
    }
    'cell_front' {
      Plate $g $frame $panel
      Screen $g $accent 14 12 36 16
      for($i=0;$i -lt 3;$i++){ $g.FillRectangle((B (C $accent)),14,(36+$i*7),36,4) }
    }
    'cell_bottom' {
      Plate $g '#3E4952' '#161D22'
      Ribs $g '#4A575F' 10 12 44 40 3 $false
    }
    'creative' {
      Plate $g $frame $panel
      $p = P (C $accent) 5
      $g.DrawEllipse($p,14,22,18,18); $g.DrawEllipse($p,32,22,18,18); $p.Dispose()
      Port $g $accent 32 32 4
    }
  }
  $g.Dispose()
  $b.Save((Join-Path $tex ($name+'.png')),[Drawing.Imaging.ImageFormat]::Png)
  $b.Dispose()
}

$E  = '#3FB8E8'   # 能量主色（电蓝青）
$L  = '#4FE08C'   # 生命能量主色（绿）
$plan = @(
  @('akaishi_energy_generator','#3FB8E8','casing',$false),
  @('akaishi_energy_generator_formed','#3FB8E8','casing',$true),
  @('akaishi_energy_cell_super','#3FB8E8','core',$false),
  @('akaishi_energy_assembly','#3FB8E8','assembly',$false),
  @('akaishi_energy_assembly_formed','#3FB8E8','assembly',$true),
  @('akaishi_gen_matrix_casing','#3FB8E8','matrix',$false),
  @('akaishi_gen_matrix_controller_basic','#3FB8E8','controller',$false),
  @('akaishi_gen_matrix_controller_basic_formed','#3FB8E8','controller',$true),
  @('akaishi_gen_matrix_controller_advanced','#7FE0FF','controller',$false),
  @('akaishi_gen_matrix_controller_advanced_formed','#7FE0FF','controller',$true),
  @('akaishi_gen_matrix_structure_glass','#3FB8E8','glass',$false),
  @('akaishi_gen_energy_output','#E0A64A','port_out',$false),
  @('akaishi_gen_fuel_input','#3FB8E8','port_in',$false),
  @('akaishi_exhausted_barrel','#C8803C','barrel',$false),
  @('akaishi_super_generator_core','#7FE0FF','core',$false),
  @('akaishi_super_generator_core_formed','#7FE0FF','core',$true),
  @('akaishi_energy_cell_serializer_bottom','#3FB8E8','cell_bottom',$false),
  @('akaishi_energy_cell_serializer_top','#3FB8E8','cell_top',$false),
  @('akaishi_energy_cell_serializer_front','#3FB8E8','cell_front',$false),
  @('akaishi_energy_cell_serializer_side','#3FB8E8','cell_side',$false),
  @('akaishi_energy_cell_serializer_formed_bottom','#7FE0FF','cell_bottom',$false),
  @('akaishi_energy_cell_serializer_formed_top','#7FE0FF','cell_top',$true),
  @('akaishi_energy_cell_serializer_formed_front','#7FE0FF','cell_front',$true),
  @('akaishi_energy_cell_serializer_formed_side','#7FE0FF','cell_side',$true),
  @('creative_akaishi_energy_cell','#F0C24A','creative',$false),
  @('creative_life_energy_cell','#4FE08C','creative',$false)
)
foreach($it in $plan){ Paint $it[0] $it[1] $it[2] $it[3] }

$cols = 8
$rows = [Math]::Ceiling($plan.Count / $cols)
$sheet = New-Object Drawing.Bitmap (($cols*64)),(($rows*64))
$sg = [Drawing.Graphics]::FromImage($sheet)
$sg.Clear([Drawing.Color]::FromArgb(255,10,13,16))
for($i=0;$i -lt $plan.Count;$i++){
  $im = [Drawing.Image]::FromFile((Join-Path $tex ($plan[$i][0]+'.png')))
  $sg.DrawImage($im,(($i % $cols)*64),([Math]::Floor($i/$cols)*64),64,64)
  $im.Dispose()
}
$sg.Dispose()
$sheet.Save($prev,[Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()
Write-Output "painted $($plan.Count) textures; preview $prev"
