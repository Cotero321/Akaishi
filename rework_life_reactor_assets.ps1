# 64x64 生命/反应堆机器贴图重制：工业机械 + 生物科技混合风
# 保留原有贴图文件名与模型语义，仅提升分辨率与视觉质量
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = $PSScriptRoot
$tex  = Join-Path $root 'common\src\main\resources\assets\akaishi\textures\block'
$prev = Join-Path $root 'gui_layouts\life_reactor_rework_preview.png'
New-Item -ItemType Directory -Force -Path (Split-Path $prev) | Out-Null

function C([string]$h){ [Drawing.ColorTranslator]::FromHtml($h) }
function B([Drawing.Color]$c){ New-Object Drawing.SolidBrush $c }
function P([Drawing.Color]$c,[int]$w){ New-Object Drawing.Pen $c,$w }

# 金属外壳基底：外框 + 内板 + 四角铆钉
function Plate($g,[string]$frame,[string]$panel){
  $g.Clear((C '#161B21'))
  $g.FillRectangle((B (C $frame)),3,3,58,58)
  $g.FillRectangle((B (C $panel)),8,8,48,48)
  $rv = B (C '#93A2B0')
  foreach($x in 5,54){ foreach($y in 5,54){ $g.FillEllipse($rv,$x,$y,5,5) } }
  $rv.Dispose()
}

# 等距肋条（vert=true 竖排）
function Ribs($g,[string]$col,[int]$x,[int]$y,[int]$w,[int]$h,[int]$n,[bool]$vert){
  $p = P (C $col) 2
  for($i=1;$i -lt $n;$i++){
    if($vert){ $xx=[Math]::Floor($x+$w*$i/$n); $g.DrawLine($p,$xx,$y,$xx,($y+$h)) }
    else     { $yy=[Math]::Floor($y+$h*$i/$n); $g.DrawLine($p,$x,$yy,($x+$w),$yy) }
  }
  $p.Dispose()
}

# 发光屏（带扫描线）
function Screen($g,[string]$col,[int]$x,[int]$y,[int]$w,[int]$h){
  $g.FillRectangle((B (C '#0B1A1E')),($x-2),($y-2),($w+4),($h+4))
  $g.FillRectangle((B (C $col)),$x,$y,$w,$h)
  $p = P (C '#0B1A1E') 1
  for($yy=$y+2;$yy -lt ($y+$h);$yy+=3){ $g.DrawLine($p,$x,$yy,($x+$w),$yy) }
  $p.Dispose()
}

# 圆形端口（含高光）
function Port($g,[string]$col,[int]$cx,[int]$cy,[int]$r){
  $g.FillEllipse((B (C '#0E1418')),($cx-$r-3),($cy-$r-3),(($r+3)*2),(($r+3)*2))
  $g.FillEllipse((B (C $col)),($cx-$r),($cy-$r),($r*2),($r*2))
  $hr = [Math]::Max(2,[Math]::Floor($r/2))
  $g.FillEllipse((B (C '#EAFBFF')),($cx-$hr),($cy-$hr),($hr*2),($hr*2))
}

function Paint([string]$name,[string]$accent,[string]$kind){
  $b = New-Object Drawing.Bitmap 64,64
  $g = [Drawing.Graphics]::FromImage($b)
  $g.SmoothingMode = 'AntiAlias'
  switch($kind){
    'vent' {
      Plate $g '#5C6C77' '#222C33'
      Ribs $g '#6E8290' 12 12 40 40 5 $false
      $g.FillRectangle((B (C $accent)),12,29,40,6)
    }
    'control' {
      Plate $g '#5C6C77' '#1B242B'
      Screen $g $accent 14 12 36 16
      Ribs $g '#6E8290' 14 34 36 10 2 $false
      Port $g $accent 22 50 5
      Port $g '#E0A64A' 42 50 5
    }
    'side' {
      Plate $g '#5C6C77' '#222C33'
      Ribs $g '#6E8290' 10 12 44 40 5 $false
      $g.FillRectangle((B (C $accent)),10,10,44,3)
    }
    'bottom' {
      Plate $g '#3E4952' '#161D22'
      Ribs $g '#4A575F' 10 12 44 40 3 $false
    }
    'core' {
      Plate $g '#5C6C77' '#202A31'
      Port $g $accent 32 32 14
      $g.DrawEllipse((P (C $accent) 2),12,12,40,40)
    }
    'grid' {
      Plate $g '#5C6C77' '#202A31'
      Ribs $g '#6E8290' 12 12 40 40 4 $true
      Ribs $g '#6E8290' 12 12 40 40 4 $false
      $g.FillRectangle((B (C $accent)),20,20,24,24)
    }
    'rotor' {
      Plate $g '#5C6C77' '#202A31'
      Port $g $accent 32 32 16
      $p = P (C '#0E1418') 3
      $g.DrawLine($p,32,14,32,50); $g.DrawLine($p,14,32,50,32); $p.Dispose()
    }
    'anvil' {
      Plate $g '#5C6C77' '#1E262C'
      $g.FillRectangle((B (C '#39434B')),14,20,36,24)
      $g.FillRectangle((B (C $accent)),18,16,28,6)
      Port $g $accent 32 42 6
    }
    'girder' {
      Plate $g '#5C6C77' '#222C33'
      Ribs $g '#6E8290' 12 12 40 40 4 $false
      $p = P (C $accent) 4
      $g.DrawLine($p,12,52,52,12); $p.Dispose()
    }
    'dna' {
      Plate $g '#5C6C77' '#1E262C'
      $p = P (C $accent) 3
      $g.DrawLine($p,22,14,42,50); $g.DrawLine($p,42,14,22,50); $p.Dispose()
      Ribs $g '#6E8290' 20 18 24 28 4 $false
    }
  }
  $g.Dispose()
  $b.Save((Join-Path $tex ($name+'.png')),[Drawing.Imaging.ImageFormat]::Png)
  $b.Dispose()
}

$plan = @(
  @('akaishi_reactor_controller_top','#32DCBE','vent'),
  @('akaishi_reactor_controller_front','#32DCBE','control'),
  @('akaishi_reactor_controller_side','#32DCBE','side'),
  @('akaishi_reactor_controller_bottom','#32DCBE','bottom'),
  @('akaishi_life_activator_top','#3FE0A8','core'),
  @('akaishi_life_activator_side','#3FE0A8','side'),
  @('akaishi_life_activator_bottom','#3FE0A8','bottom'),
  @('akaishi_life_breeder_top','#6BD96B','grid'),
  @('akaishi_life_breeder_side','#6BD96B','side'),
  @('akaishi_life_centrifuge_top','#3FC8E0','rotor'),
  @('akaishi_life_centrifuge_side','#3FC8E0','side'),
  @('akaishi_life_centrifuge_bottom','#3FC8E0','bottom'),
  @('akaishi_life_fusion_anvil_top','#F0A93C','anvil'),
  @('akaishi_life_fusion_anvil_side','#F0A93C','side'),
  @('akaishi_life_fusion_anvil_bottom','#F0A93C','bottom'),
  @('akaishi_life_struct_top','#7FA8C0','girder'),
  @('akaishi_life_struct_side','#7FA8C0','side'),
  @('akaishi_transgene_factory_top','#D05CE8','dna'),
  @('akaishi_transgene_factory_side','#D05CE8','side'),
  @('akaishi_life_conversion_architecture_top','#9A6BE8','vent'),
  @('akaishi_life_conversion_architecture_front','#9A6BE8','control'),
  @('akaishi_life_conversion_architecture_side','#9A6BE8','side'),
  @('akaishi_life_conversion_architecture_bottom','#9A6BE8','bottom'),
  @('akaishi_life_conversion_architecture_formed_top','#B98CFF','vent'),
  @('akaishi_life_conversion_architecture_formed_front','#B98CFF','control'),
  @('akaishi_life_conversion_architecture_formed_side','#B98CFF','side'),
  @('akaishi_life_conversion_architecture_formed_bottom','#B98CFF','bottom'),
  @('akaishi_life_energy_cell_serializer_formed_top','#6BE0C8','vent'),
  @('akaishi_life_energy_cell_serializer_formed_front','#6BE0C8','control'),
  @('akaishi_life_energy_cell_serializer_formed_side','#6BE0C8','side'),
  @('akaishi_life_energy_cell_serializer_formed_bottom','#6BE0C8','bottom'),
  @('machine_bottom','#54636E','bottom')
)
foreach($it in $plan){ Paint $it[0] $it[1] $it[2] }

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
