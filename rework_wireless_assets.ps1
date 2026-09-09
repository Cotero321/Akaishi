# 64x64 无线网络机器家族贴图重制：工业机械 + 生物科技混合风
# 覆盖无线控制器/核心/终端/桥接/端口/外壳/结构玻璃，保留原文件名与模型语义
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = $PSScriptRoot
$tex  = Join-Path $root 'common\src\main\resources\assets\akaishi\textures\block'
$prev = Join-Path $root 'gui_layouts\wireless_rework_preview.png'
New-Item -ItemType Directory -Force -Path (Split-Path $prev) | Out-Null

function C([string]$h){ [Drawing.ColorTranslator]::FromHtml($h) }
function B([Drawing.Color]$c){ New-Object Drawing.SolidBrush $c }
function P([Drawing.Color]$c,[int]$w){ New-Object Drawing.Pen $c,$w }

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

# 信号弧（右上角发射）
function Wave($g,[string]$col,[int]$cx,[int]$cy){
  $p = P (C $col) 3
  for($r=7;$r -le 19;$r+=6){ $g.DrawArc($p,($cx-$r),($cy-$r),($r*2),($r*2),200,140) }
  $p.Dispose()
}

function Paint([string]$name,[string]$accent,[string]$kind,[bool]$bright=$false){
  $b = New-Object Drawing.Bitmap 64,64
  $g = [Drawing.Graphics]::FromImage($b)
  $g.SmoothingMode = 'AntiAlias'
  $frame = if($bright){'#6E8492'}else{'#5C6C77'}
  $panel = if($bright){'#243039'}else{'#1B242B'}
  switch($kind){
    'shell' {
      Plate $g $frame $panel
      Ribs $g '#6E8290' 12 12 40 40 4 $false
      Port $g $accent 32 32 6
    }
    'core' {
      Plate $g $frame $panel
      Port $g $accent 32 32 13
      $g.DrawEllipse((P (C $accent) 2),13,13,38,38)
    }
    'ctrl_top' {
      Plate $g $frame $panel
      Wave $g $accent 32 34
      Port $g $accent 32 34 5
    }
    'ctrl_front' {
      Plate $g $frame $panel
      Screen $g $accent 12 12 40 16
      Ribs $g '#6E8290' 12 34 40 8 2 $false
      Port $g $accent 22 50 5
      Port $g '#E0A64A' 42 50 5
    }
    'ctrl_side' {
      Plate $g $frame $panel
      Ribs $g '#6E8290' 10 12 44 40 5 $true
      $g.FillRectangle((B (C $accent)),10,10,44,3)
    }
    'ctrl_bottom' {
      Plate $g '#3E4952' '#161D22'
      Ribs $g '#4A575F' 10 12 44 40 3 $false
    }
    'terminal' {
      Plate $g $frame $panel
      Screen $g $accent 12 12 40 30
      Ribs $g '#6E8290' 12 48 40 4 2 $false
    }
    'glass' {
      $g.Clear([Drawing.Color]::FromArgb(255,10,16,20))
      $g.FillRectangle((B ([Drawing.Color]::FromArgb(110,130,110,230))),3,3,58,58)
      $p = P ([Drawing.Color]::FromArgb(200,200,190,250)) 2
      $g.DrawRectangle($p,3,3,57,57); $g.DrawLine($p,6,56,56,6); $p.Dispose()
    }
    'port_in' {
      Plate $g $frame $panel
      $g.FillRectangle((B (C $accent)),16,16,32,32)
      $g.DrawRectangle((P (C '#0C1216') 3),16,16,32,32)
      $p = P (C '#0C1216') 4
      $g.DrawLine($p,32,20,32,44); $g.DrawLine($p,32,44,25,37); $g.DrawLine($p,32,44,39,37); $p.Dispose()
    }
    'port_out' {
      Plate $g $frame $panel
      $g.FillRectangle((B (C $accent)),16,16,32,32)
      $g.DrawRectangle((P (C '#0C1216') 3),16,16,32,32)
      $p = P (C '#0C1216') 4
      $g.DrawLine($p,32,20,32,44); $g.DrawLine($p,32,20,25,27); $g.DrawLine($p,32,20,39,27); $p.Dispose()
    }
    'loss' {
      Plate $g $frame $panel
      $p = P (C $accent) 6
      $g.DrawLine($p,18,18,46,46); $g.DrawLine($p,46,18,18,46); $p.Dispose()
    }
    'security' {
      Plate $g $frame $panel
      $pts = @(
        (New-Object Drawing.Point 32,12),(New-Object Drawing.Point 48,20),
        (New-Object Drawing.Point 46,42),(New-Object Drawing.Point 32,52),
        (New-Object Drawing.Point 18,42),(New-Object Drawing.Point 16,20)
      )
      $g.FillPolygon((B (C $accent)),$pts)
      Port $g '#0C1216' 32 32 6
    }
    'grid' {
      Plate $g $frame $panel
      Ribs $g '#6E8290' 12 12 40 40 4 $true
      Ribs $g '#6E8290' 12 12 40 40 4 $false
      foreach($x in 14,30,46){ foreach($y in 14,30,46){ Port $g $accent $x $y 3 } }
    }
    'range' {
      Plate $g $frame $panel
      $p = P (C $accent) 2
      foreach($r in 8,14,20){ $g.DrawEllipse($p,(32-$r),(32-$r),($r*2),($r*2)) }
      $p.Dispose()
      Port $g $accent 32 32 4
    }
    'bridge' {
      Plate $g $frame $panel
      $p = P (C $accent) 4
      $g.DrawLine($p,12,20,52,20); $g.DrawLine($p,12,44,52,44)
      $g.DrawLine($p,20,20,20,44); $g.DrawLine($p,44,20,44,44); $p.Dispose()
      Port $g $accent 32 32 7
    }
  }
  $g.Dispose()
  $b.Save((Join-Path $tex ($name+'.png')),[Drawing.Imaging.ImageFormat]::Png)
  $b.Dispose()
}

$W  = '#8A6BE8'   # 无线主色（紫罗兰）
$plan = @(
  @('akaishi_wireless_shell','#8A6BE8','shell',$false),
  @('akaishi_wireless_core','#8A6BE8','core',$false),
  @('akaishi_wireless_controller_top','#8A6BE8','ctrl_top',$false),
  @('akaishi_wireless_controller_front','#8A6BE8','ctrl_front',$false),
  @('akaishi_wireless_controller_side','#8A6BE8','ctrl_side',$false),
  @('akaishi_wireless_controller_bottom','#8A6BE8','ctrl_bottom',$false),
  @('akaishi_wireless_controller_formed','#B49BFF','core',$true),
  @('akaishi_wireless_terminal','#8A6BE8','terminal',$false),
  @('akaishi_wireless_terminal_formed','#B49BFF','terminal',$true),
  @('akaishi_wireless_structure_glass','#8A6BE8','glass',$false),
  @('akaishi_wireless_input_port','#47A8FF','port_in',$false),
  @('akaishi_wireless_output_port','#FF557A','port_out',$false),
  @('akaishi_wireless_input_loss','#E0A64A','loss',$false),
  @('akaishi_wireless_output_loss','#D85B6B','loss',$false),
  @('akaishi_wireless_security','#3FC8E0','security',$false),
  @('akaishi_wireless_chunk_loader','#8A6BE8','grid',$false),
  @('akaishi_wireless_chunk_range','#8A6BE8','range',$false),
  @('akaishi_wireless_dim_bridge','#8A6BE8','bridge',$false)
)
foreach($it in $plan){ Paint $it[0] $it[1] $it[2] $it[3] }

$cols = 6
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
