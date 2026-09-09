# Native 64x64 legacy machine painter; preserves directional front faces.
Add-Type -AssemblyName System.Drawing
$root = Join-Path $PSScriptRoot 'common\src\main\resources\assets\akaishi\textures\block'
$models = Join-Path $PSScriptRoot 'common\src\main\resources\assets\akaishi\models\block'
$preview = Join-Path $PSScriptRoot 'gui_layouts\legacy_energy_machine_preview.png'
function Rect($g,$x,$y,$w,$h,$color){
 $brush = New-Object Drawing.SolidBrush([Drawing.ColorTranslator]::FromHtml($color))
 try {$g.FillRectangle($brush,$x,$y,$w,$h)} finally {$brush.Dispose()}
}
function Build($name,$face,$accent,$life){
 $b=New-Object Drawing.Bitmap(64,64)
 $g=[Drawing.Graphics]::FromImage($b)
 $g.Clear([Drawing.ColorTranslator]::FromHtml('#343C45'))
 Rect $g 2 2 60 60 '#9CA7AF'; Rect $g 4 4 56 56 '#181F29'
 Rect $g 7 7 50 50 '#576572'; Rect $g 9 9 46 46 '#394753'
 if($face -eq 'bottom'){
  for($x=14;$x -lt 52;$x+=8){Rect $g $x 12 3 40 '#A0ACB6';Rect $g ($x+3) 12 2 40 '#19232D'}
 } elseif($face -eq 'top'){
  Rect $g 14 14 36 36 '#17222B';Rect $g 17 17 30 30 $accent;Rect $g 21 21 22 22 '#283742'
  for($y=24;$y -lt 42;$y+=5){Rect $g 24 $y 16 2 '#9FAEB6'}
 } elseif($life){
  Rect $g 17 12 30 40 '#142A2A';Rect $g 21 16 22 32 $accent
  Rect $g 24 18 4 27 '#C3F5C9';Rect $g 30 20 10 24 '#256B59'
  Rect $g 19 12 26 4 '#A8B9B3';Rect $g 19 48 26 4 '#A8B9B3'
  if($face -eq 'front'){Rect $g 30 24 4 16 '#D9FFD6';Rect $g 25 30 14 4 '#D9FFD6'}
 } else {
  Rect $g 14 15 36 30 '#14202B';Rect $g 18 19 28 22 $accent
  Rect $g 23 23 18 14 '#1C303B';Rect $g 26 25 4 10 '#D4ECED'
  for($x=17;$x -lt 49;$x+=6){Rect $g $x 49 3 3 $accent}
 }
 foreach($x in 5,56){foreach($y in 5,56){Rect $g $x $y 3 3 '#D4DADE';Rect $g $x ($y+1) 3 1 '#525C66'}}
 $g.Dispose()
 $b.Save((Join-Path $root ($name+'_'+$face+'.png')),[Drawing.Imaging.ImageFormat]::Png)
 return $b
}
$legacy=@('akaishi_energy_generator','akaishi_energy_processor','akaishi_energy_assembly','akaishi_compressor','akaishi_pulverizer','akaishi_transformer','akaishi_energy_cell_basic','akaishi_energy_cell_advanced','akaishi_energy_cell_super')
# Repair references only: do not overwrite the previously reviewed 27 PNGs.
foreach($name in $legacy){
 $model=@{parent='minecraft:block/cube_bottom_top';textures=@{side="akaishi:block/${name}_side";top="akaishi:block/${name}_top";bottom="akaishi:block/${name}_bottom"}}
 $json=ConvertTo-Json $model -Depth 8
 [IO.File]::WriteAllText((Join-Path $models ($name+'.json')),$json+[Environment]::NewLine,(New-Object Text.UTF8Encoding($false)))
}
# Existing models already consume these textures, including the life-cell north face.
$sets=@(@('akaishi_energy_liquefier','#AD76DA',$false),@('akaishi_life_energy_cell','#64C999',$true))
$items=@()
foreach($s in $sets){
 $faces=if($s[2]){@('side','top','bottom','front')}else{@('side','top','bottom')}
 foreach($f in $faces){$items+=Build $s[0] $f $s[1] $s[2]}
}
$sheet=New-Object Drawing.Bitmap(512,128);$g=[Drawing.Graphics]::FromImage($sheet)
$g.Clear([Drawing.ColorTranslator]::FromHtml('#121419'))
for($i=0;$i -lt $items.Count;$i++){$g.DrawImageUnscaled($items[$i],($i*72),24);$items[$i].Dispose()}
$g.Dispose();$sheet.Save((Join-Path $PSScriptRoot 'gui_layouts\legacy_life_machine_preview.png'),[Drawing.Imaging.ImageFormat]::Png);$sheet.Dispose()
Write-Output 'Wired 27 existing textures in 9 models; painted 7 native 64x64 textures for liquefier and three life-cell tiers (shared family textures).'
