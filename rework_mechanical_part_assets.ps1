# 机械部件材质重制：工业机械 + 生物科技混合风
# material/* 16x16 可平铺主色源（BEWLR 合成取色）；shape/* 64x64 灰度 alpha 遮罩（提供轮廓与明暗）
# 保留原文件名，模型/渲染引用不变
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root   = $PSScriptRoot
$matDir = Join-Path $root 'common\src\main\resources\assets\akaishi\textures\mechanical_part\material'
$shpDir = Join-Path $root 'common\src\main\resources\assets\akaishi\textures\mechanical_part\shape'
$prev   = Join-Path $root 'gui_layouts\mechanical_part_rework_preview.png'
New-Item -ItemType Directory -Force -Path (Split-Path $prev) | Out-Null

function C([string]$h){ [Drawing.ColorTranslator]::FromHtml($h) }
function B([Drawing.Color]$c){ New-Object Drawing.SolidBrush $c }
function P([Drawing.Color]$c,[int]$w){ New-Object Drawing.Pen $c,$w }
function Mix([Drawing.Color]$a,[Drawing.Color]$b,[double]$t){
  if($t -lt 0){ $t = 0 }
  if($t -gt 1){ $t = 1 }
  return [Drawing.Color]::FromArgb(255,
    [int][Math]::Round($a.R + ($b.R - $a.R) * $t),
    [int][Math]::Round($a.G + ($b.G - $a.G) * $t),
    [int][Math]::Round($a.B + ($b.B - $a.B) * $t))
}
# 固定种子伪随机噪声表（避免在表达式中调用函数），保证可复现
$script:seed = 20240909
$script:RND = [double[]]::new(256)
for($i=0;$i -lt 256;$i++){
  $script:seed = ([int64]$script:seed * 1103515245 + 12345) % 2147483648
  $script:RND[$i] = $script:seed / 2147483648.0
}

# ==================== 材料色源（16x16 可平铺） ====================
$mats = @(
  @{ n='iron';                 s='brushed';    base='#9AA3AC'; dark='#6B747D'; light='#C8D0D8' },
  @{ n='alloy_steel';          s='crosshatch'; base='#7E8C9A'; dark='#4A5560'; light='#B4C0CC' },
  @{ n='resistant_steel';      s='rivet';      base='#5A646E'; dark='#333A42'; light='#8E98A2' },
  @{ n='precision_alloy';      s='grid';       base='#B8C0C8'; dark='#7C858E'; light='#E6ECF2' },
  @{ n='ceramic_composite';    s='ceramic';    base='#D6D3CA'; dark='#A6A198'; light='#F0EDE6' },
  @{ n='bio_ceramic';          s='bio';        base='#6E9A78'; dark='#3F5F4A'; light='#A8CCAE' },
  @{ n='redstone_alloy';       s='circuit';    base='#6E2420'; dark='#3E1210'; light='#B83A30'; node='#FF6A50' },
  @{ n='polymerized_redstone'; s='circuit2';   base='#8E1A18'; dark='#4A0C0B'; light='#C43A2C'; node='#FF4A3A' },
  @{ n='refined_core';         s='lattice';    base='#2E3A46'; dark='#141E28'; light='#4A5A6A'; node='#4FE0FF' },
  @{ n='psionic_composite';    s='psionic';    base='#6A4E9A'; dark='#372558'; light='#9A78C8'; node='#D8A8FF' }
)

function Tile($d){
  $base = C $d.base; $dark = C $d.dark; $light = C $d.light
  $line = if($d.ContainsKey('light')){ C $d.light } else { $base }
  $node = if($d.ContainsKey('node')){ C $d.node } else { $light }
  $off = 0
  foreach($ch in $d.n.ToCharArray()){ $off += [int]$ch }
  $b = New-Object Drawing.Bitmap 16,16
  for($y=0;$y -lt 16;$y++){
    for($x=0;$x -lt 16;$x++){
      $n = $script:RND[($y * 16 + $x + $off) % 256]
      $n2 = $script:RND[($y * 16 + $x + $off + 97) % 256]
      $c = $base
      switch($d.s){
        'brushed'{
          # 横向拉丝：按行正弦 + 细噪声
          $streak = 0.5 + 0.5 * [Math]::Sin(2 * [Math]::PI * $y * 3 / 16.0)
          $t = 0.5 + ($streak - 0.5) * 0.35 + ($n - 0.5) * 0.30
          $c = Mix $dark $light $t
        }
        'crosshatch'{
          $t = 0.5 + ($n - 0.5) * 0.22
          if((($x + $y) % 8) -eq 0 -or (($x - $y + 16) % 8) -eq 0){ $t = 0.78 }
          $c = Mix $dark $light $t
        }
        'rivet'{
          $t = 0.5 + ($n - 0.5) * 0.16
          $rx = $x % 8; $ry = $y % 8
          if($rx -eq 0 -and $ry -eq 0){ $t = 0.96 }
          elseif(($rx -eq 1 -and $ry -eq 0) -or ($rx -eq 0 -and $ry -eq 1)){ $t = 0.28 }
          $c = Mix $dark $light $t
        }
        'grid'{
          $t = 0.55 + ($n - 0.5) * 0.12
          if(($x % 4) -eq 0 -or ($y % 4) -eq 0){ $t = 0.84 }
          $c = Mix $dark $light $t
        }
        'ceramic'{
          $t = 0.70 + ($n - 0.5) * 0.14
          if(($y % 8) -eq 7){ $t = 0.42 }          # 横向接缝
          if($n2 -gt 0.94){ $t = 0.34 }            # 釉面麻点
          $c = Mix $dark $light $t
        }
        'bio'{
          # 有机细胞：两处 8 格周期胞体
          $d1 = [Math]::Sqrt([Math]::Pow(($x - 4),2) + [Math]::Pow(($y - 4),2))
          $d2 = [Math]::Sqrt([Math]::Pow(($x - 12),2) + [Math]::Pow(($y - 12),2))
          $dd = [Math]::Min($d1,$d2)
          if($dd -lt 2.4){ $c = Mix $light $base 0.2 }
          elseif($dd -lt 3.4){ $c = Mix $dark $base 0.35 }
          else { $c = Mix $dark $light (0.55 + ($n - 0.5) * 0.12) }
        }
        'circuit'{
          if(($x % 8) -eq 0 -or ($y % 8) -eq 0){ $c = $line }
          else { $c = Mix $dark $base (0.35 + ($n * 0.35)) }
          if(($x % 8) -eq 0 -and ($y % 8) -eq 0){ $c = $node }
        }
        'circuit2'{
          if(($x % 4) -eq 0 -or ($y % 4) -eq 0){ $c = $line }
          else { $c = Mix $dark $base (0.40 + ($n * 0.30)) }
          if(($x % 8) -eq 0 -and ($y % 8) -eq 0){ $c = $node }
        }
        'lattice'{
          if(($x % 8) -eq 0 -or ($y % 8) -eq 0){ $c = $node }
          elseif(($x % 8) -eq 4 -and ($y % 8) -eq 4){ $c = Mix $light $node 0.5 }
          else { $c = Mix $dark $base (0.30 + ($n * 0.30)) }
        }
        'psionic'{
          $d1 = [Math]::Sqrt([Math]::Pow(($x - 4),2) + [Math]::Pow(($y - 4),2))
          $d2 = [Math]::Sqrt([Math]::Pow(($x - 12),2) + [Math]::Pow(($y - 12),2))
          $dd = [Math]::Min($d1,$d2)
          $t = 0.50 + ($n - 0.5) * 0.15
          if($dd -lt 2.5){ $t = 0.95 }
          elseif($dd -lt 4.0){ $t = 0.72 }
          $c = Mix $dark $light $t
          if($dd -lt 1.4){ $c = $node }
        }
      }
      $b.SetPixel($x,$y,$c)
    }
  }
  $b.Save((Join-Path $matDir ($d.n + '.png')),[Drawing.Imaging.ImageFormat]::Png)
  $b.Dispose()
}

foreach($m in $mats){ Tile $m }

# ==================== 形状遮罩（64x64 灰度 + alpha） ====================
# 合成公式：结果 = 材料色 x (0.3 + 0.7 x 形状亮度)；>200 再提亮 1.3x，<50 压暗 0.6x
$GB = '#A8A8A8'   # 主体
$GL = '#E2E2E2'   # 高光
$GD = '#6A6A6A'   # 暗部
$GE = '#3C3C3C'   # 轮廓
$GW = '#FFFFFF'   # 炽亮核心

function NewShape(){
  $b = New-Object Drawing.Bitmap 64,64
  $g = [Drawing.Graphics]::FromImage($b)
  $g.SmoothingMode = 'AntiAlias'
  $g.Clear([Drawing.Color]::Transparent)
  return @($b,$g)
}
function SaveShape($b,[string]$name){
  $b.Save((Join-Path $shpDir ($name + '.png')),[Drawing.Imaging.ImageFormat]::Png)
  $b.Dispose()
}
# 主体填充 + 内侧高光环 + 外轮廓
function Bevel($g,$path){
  $g.FillPath((B (C $GB)),$path)
  $st = $g.Save()
  $g.SetClip($path)
  $g.DrawPath((P (C $GL) 5),$path)
  $g.Restore($st)
  $g.DrawPath((P (C $GE) 2),$path)
}
function RRect([int]$x,[int]$y,[int]$w,[int]$h,[int]$r){
  $p = New-Object Drawing.Drawing2D.GraphicsPath
  $p.AddArc($x,       $y,       $r*2,$r*2,180,90)
  $p.AddArc($x+$w-$r*2,$y,       $r*2,$r*2,270,90)
  $p.AddArc($x+$w-$r*2,$y+$h-$r*2,$r*2,$r*2,0,90)
  $p.AddArc($x,       $y+$h-$r*2,$r*2,$r*2,90,90)
  $p.CloseFigure()
  return $p
}
function Poly([object[]]$coords){
  $n = [int]($coords.Count / 2)
  $pts = [Drawing.PointF[]]::new($n)
  for($i=0;$i -lt $n;$i++){
    $pts[$i] = [Drawing.PointF]::new([float]$coords[$i*2],[float]$coords[$i*2+1])
  }
  return ,$pts
}

# ---- 部件：核心 ----
function ShapeCore(){
  $r = NewShape; $b = $r[0]; $g = $r[1]
  $p = New-Object Drawing.Drawing2D.GraphicsPath
  $pts = @()
  for($i=0;$i -lt 6;$i++){
    $a = [Math]::PI / 6 + $i * [Math]::PI / 3
    $pts += (32 + 26 * [Math]::Cos($a)); $pts += (32 + 26 * [Math]::Sin($a))
  }
  $p.AddPolygon((Poly $pts))
  Bevel $g $p
  $g.DrawRectangle((P (C $GD) 3),17,17,30,30)
  foreach($pt in @(@(5,29),@(55,29),@(29,5),@(29,55))){
    $g.FillRectangle((B (C $GL)),$pt[0],$pt[1],4,4)
  }
  $g.FillEllipse((B (C $GW)),24,24,16,16)
  $g.DrawEllipse((P (C $GD) 2),24,24,16,16)
  SaveShape $b 'core'
}

# ---- 部件：模块（电路板） ----
function ShapeModule(){
  $r = NewShape; $b = $r[0]; $g = $r[1]
  $p = RRect 4 6 56 52 8
  Bevel $g $p
  $g.DrawLine((P (C $GD) 2),12,20,52,20)
  $g.DrawLine((P (C $GD) 2),12,32,52,32)
  $g.DrawLine((P (C $GD) 2),12,44,52,44)
  $g.DrawLine((P (C $GD) 2),20,20,20,32)
  $g.DrawLine((P (C $GD) 2),44,32,44,44)
  foreach($pt in @(@(16,16),@(46,28),@(30,40))){
    $g.FillRectangle((B (C $GL)),$pt[0],$pt[1],8,8)
    $g.DrawRectangle((P (C $GE) 1),$pt[0],$pt[1],8,8)
  }
  # 两侧引脚
  foreach($y in @(18,30,42)){
    $g.FillRectangle((B (C $GL)),0,$y,6,4)
    $g.FillRectangle((B (C $GL)),58,$y,6,4)
  }
  SaveShape $b 'module'
}

# ---- 部件：外壳（装甲板） ----
function ShapeShell(){
  $r = NewShape; $b = $r[0]; $g = $r[1]
  $p = New-Object Drawing.Drawing2D.GraphicsPath
  $p.AddPolygon((Poly @(8,6, 56,6, 60,12, 60,44, 32,60, 4,44, 4,12)))
  Bevel $g $p
  $g.DrawLine((P (C $GD) 2),32,12,32,52)
  $g.DrawLine((P (C $GD) 2),12,44,52,44)
  foreach($pt in @(@(13,13),@(47,13),@(13,38),@(47,38))){
    $g.FillEllipse((B (C $GL)),$pt[0],$pt[1],6,6)
    $g.DrawEllipse((P (C $GE) 1),$pt[0],$pt[1],6,6)
  }
  $g.FillRectangle((B (C $GL)),26,24,12,8)
  SaveShape $b 'shell'
}

# ---- 部件：散热（鳍片 + 风扇） ----
function ShapeCooling(){
  $r = NewShape; $b = $r[0]; $g = $r[1]
  Bevel $g (RRect 4 4 56 56 8)
  foreach($x in @(11,19,27,35,43,51)){
    $g.FillRectangle((B (C $GL)),$x,9,4,46)
    $g.DrawRectangle((P (C $GD) 1),$x,9,4,46)
  }
  $g.FillEllipse((B (C $GD)),22,22,20,20)
  $g.FillEllipse((B (C $GL)),25,25,14,14)
  $g.FillEllipse((B (C $GW)),29,29,6,6)
  SaveShape $b 'cooling'
}

# ---- 器官：眼 ----
function ShapeEye(){
  $r = NewShape; $b = $r[0]; $g = $r[1]
  $p = New-Object Drawing.Drawing2D.GraphicsPath
  $p.AddEllipse(4,16,56,32)
  Bevel $g $p
  $g.FillEllipse((B (C $GL)),20,20,24,24)
  $g.DrawEllipse((P (C $GD) 2),20,20,24,24)
  $g.FillEllipse((B (C $GD)),25,25,14,14)
  $g.FillEllipse((B (C $GW)),28,28,8,8)
  $g.FillEllipse((B (C $GL)),21,21,5,5)
  SaveShape $b 'organ_eye'
}

# ---- 器官：心脏 ----
function ShapeHeart(){
  $r = NewShape; $b = $r[0]; $g = $r[1]
  $p = New-Object Drawing.Drawing2D.GraphicsPath
  $p.FillMode = [Drawing.Drawing2D.FillMode]::Winding
  $p.AddEllipse(8,12,28,28)
  $p.AddEllipse(28,12,28,28)
  $p.AddPolygon((Poly @(10,27, 54,27, 32,58)))
  Bevel $g $p
  $g.DrawLine((P (C $GD) 2),32,16,32,52)
  $g.DrawLine((P (C $GD) 2),20,26,32,34)
  $g.DrawLine((P (C $GD) 2),44,26,32,34)
  $g.FillEllipse((B (C $GW)),26,30,12,12)
  SaveShape $b 'organ_heart'
}

# ---- 器官：肺 ----
function ShapeLung(){
  $r = NewShape; $b = $r[0]; $g = $r[1]
  $p = New-Object Drawing.Drawing2D.GraphicsPath
  $p.FillMode = [Drawing.Drawing2D.FillMode]::Winding
  $p.AddRectangle([Drawing.RectangleF]::new(28,6,8,24))
  $p.AddEllipse(8,20,22,36)
  $p.AddEllipse(34,20,22,36)
  Bevel $g $p
  $g.DrawLine((P (C $GD) 3),32,26,18,36)
  $g.DrawLine((P (C $GD) 3),32,26,46,36)
  foreach($y in @(34,42,50)){
    $g.DrawLine((P (C $GD) 2),12,$y,28,$y)
    $g.DrawLine((P (C $GD) 2),36,$y,52,$y)
  }
  SaveShape $b 'organ_lung'
}

# ---- 器官：内脏（胃 + 肠） ----
function ShapeViscera(){
  $r = NewShape; $b = $r[0]; $g = $r[1]
  $p = New-Object Drawing.Drawing2D.GraphicsPath
  $p.AddEllipse(10,8,32,30)
  Bevel $g $p
  # 肠管：先暗色粗描边再亮色内芯
  $coil = New-Object Drawing.Drawing2D.GraphicsPath
  $coil.AddArc(12,36,24,18,180,180)
  $coil.AddArc(30,40,24,18,180,180)
  $coil.AddArc(18,44,28,16,0,180)
  $g.DrawPath((P (C $GE) 9),$coil)
  $g.DrawPath((P (C $GL) 5),$coil)
  $g.DrawLine((P (C $GD) 2),22,18,32,30)
  SaveShape $b 'organ_viscera'
}

# ---- 器官：肾 ----
function ShapeKidney(){
  $r = NewShape; $b = $r[0]; $g = $r[1]
  $p = New-Object Drawing.Drawing2D.GraphicsPath
  $p.FillMode = [Drawing.Drawing2D.FillMode]::Alternate
  $p.AddEllipse(14,8,36,48)
  $p.AddEllipse(10,24,24,18)   # 内侧凹陷
  Bevel $g $p
  $g.FillRectangle((B (C $GL)),36,48,6,12)
  $g.DrawRectangle((P (C $GE) 1),36,48,6,12)
  $g.DrawLine((P (C $GD) 2),26,20,40,28)
  $g.DrawLine((P (C $GD) 2),26,40,40,34)
  SaveShape $b 'organ_kidney'
}

# ---- 器官：手臂（可镜像） ----
function ShapeArm([string]$name,[bool]$mirror){
  $r = NewShape; $b = $r[0]; $g = $r[1]
  if($mirror){ $g.TranslateTransform(64,0); $g.ScaleTransform(-1,1) }
  $p = New-Object Drawing.Drawing2D.GraphicsPath
  $p.AddPolygon((Poly @(20,4, 44,4, 44,22, 40,26, 40,44, 46,48, 46,58, 18,58, 18,48, 24,44, 24,26, 20,22)))
  Bevel $g $p
  $g.DrawLine((P (C $GD) 2),24,26,40,26)
  $g.DrawLine((P (C $GD) 2),24,44,40,44)
  $g.DrawLine((P (C $GD) 2),26,52,38,52)
  $g.FillRectangle((B (C $GL)),26,8,12,6)
  SaveShape $b $name
}

# ---- 器官：腿（可镜像） ----
function ShapeLeg([string]$name,[bool]$mirror){
  $r = NewShape; $b = $r[0]; $g = $r[1]
  if($mirror){ $g.TranslateTransform(64,0); $g.ScaleTransform(-1,1) }
  $p = New-Object Drawing.Drawing2D.GraphicsPath
  $p.AddPolygon((Poly @(18,4, 46,4, 44,32, 40,44, 52,44, 52,58, 16,58, 16,46, 24,44, 20,32)))
  Bevel $g $p
  $g.DrawLine((P (C $GD) 2),20,32,44,32)
  $g.DrawLine((P (C $GD) 2),22,44,40,44)
  $g.DrawLine((P (C $GD) 2),20,52,48,52)
  $g.FillRectangle((B (C $GL)),22,8,16,6)
  SaveShape $b $name
}

# ---- 通用器官（32x32） ----
function ShapeOrganDefault(){
  $b = New-Object Drawing.Bitmap 32,32
  $g = [Drawing.Graphics]::FromImage($b)
  $g.SmoothingMode = 'AntiAlias'
  $g.Clear([Drawing.Color]::Transparent)
  $p = New-Object Drawing.Drawing2D.GraphicsPath
  $p.AddEllipse(4,4,24,24)
  Bevel $g $p
  $g.FillRectangle((B (C $GW)),13,9,6,14)
  $g.FillRectangle((B (C $GW)),9,13,14,6)
  $b.Save((Join-Path $shpDir 'organ_default.png'),[Drawing.Imaging.ImageFormat]::Png)
  $b.Dispose()
}

ShapeCore
ShapeModule
ShapeShell
ShapeCooling
ShapeEye
ShapeHeart
ShapeLung
ShapeViscera
ShapeKidney
ShapeArm 'organ_left_arm'  $false
ShapeArm 'organ_right_arm' $true
ShapeLeg 'organ_left_leg'  $false
ShapeLeg 'organ_right_leg' $true
ShapeOrganDefault

# ==================== 预览拼图 ====================
$order = @(
  'material/iron','material/alloy_steel','material/resistant_steel','material/precision_alloy',
  'material/ceramic_composite','material/bio_ceramic','material/redstone_alloy',
  'material/polymerized_redstone','material/refined_core','material/psionic_composite',
  'shape/core','shape/module','shape/shell','shape/cooling',
  'shape/organ_eye','shape/organ_heart','shape/organ_lung','shape/organ_viscera',
  'shape/organ_kidney','shape/organ_left_arm','shape/organ_right_arm',
  'shape/organ_left_leg','shape/organ_right_leg','shape/organ_default'
)
$cols = 6
$rows = [Math]::Ceiling($order.Count / $cols)
$cell = 76
$sheet = New-Object Drawing.Bitmap ($cols * $cell),($rows * $cell)
$sg = [Drawing.Graphics]::FromImage($sheet)
$sg.Clear([Drawing.Color]::FromArgb(255,12,15,19))
$sg.InterpolationMode = 'NearestNeighbor'
for($i=0;$i -lt $order.Count;$i++){
  $rel = $order[$i]
  $file = if($rel.StartsWith('material/')){ Join-Path $matDir (($rel -replace 'material/','') + '.png') }
          else { Join-Path $shpDir (($rel -replace 'shape/','') + '.png') }
  $im = [Drawing.Image]::FromFile($file)
  $cx = ($i % $cols) * $cell + 6
  $cy = [Math]::Floor($i / $cols) * $cell + 6
  $sg.DrawImage($im,$cx,$cy,64,64)
  $im.Dispose()
}
$sg.Dispose()
$sheet.Save($prev,[Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()
Write-Output "painted $($mats.Count) materials + 14 shapes; preview $prev"
