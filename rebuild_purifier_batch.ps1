param([switch]$PreviewOnly)
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.Drawing
$block=Join-Path $PSScriptRoot 'common\src\main\resources\assets\akaishi\textures\block'
$out=Join-Path $PSScriptRoot 'gui_layouts'
if(!(Test-Path $out)){throw 'Missing gui_layouts'}
function Color($hex){[Drawing.ColorTranslator]::FromHtml('#'+$hex)}
function Rect($x,$y,$w,$h,$hex){$b=[Drawing.SolidBrush]::new((Color $hex));try{$script:g.FillRectangle($b,[int]$x,[int]$y,[int]$w,[int]$h)}finally{$b.Dispose()}}
$names=@();foreach($f in @('purifier','life_purifier','advanced_purifier')){foreach($s in @('side','top','bottom')){$names+='akaishi_'+$f+'_'+$s}}
$names+=@('matrix_casing','matrix_controller','matrix_controller_formed','matrix_structure_glass','energy_input','item_input','item_output')|ForEach-Object {'akaishi_purifier_'+$_}
& node (Join-Path $PSScriptRoot 'validate_purifier_batch.js')
if($LASTEXITCODE -ne 0){throw 'Preflight failed'}
foreach($name in $names){
 $path=Join-Path $block ($name+'.png');$src=[Drawing.Bitmap]::new($path);$bmp=[Drawing.Bitmap]::new(64,64);$script:g=[Drawing.Graphics]::FromImage($bmp)
 try{
  $accent='54b9de';$light='b4e6ee';if($name -match 'life'){$accent='62cb91';$light='c3f1b4'};if($name -match 'advanced'){$accent='b29ee2';$light='e0d2ee'}
  Rect 0 0 64 64 '141e29';Rect 2 2 60 60 '657889';Rect 4 4 56 56 '303f4e';Rect 5 5 54 2 '8a9eaa';Rect 5 57 54 2 '1b2733'
  foreach($y in @(10,48)){Rect 10 $y 44 6 '202e3a';Rect 11 $y 42 1 '4e6371';for($x=14;$x -lt 50;$x+=6){Rect $x ($y+2) 3 2 '81949b'}}
  foreach($x in @(7,53)){foreach($y in @(7,53)){Rect $x $y 4 4 '0d1721';Rect $x $y 3 1 'c0ccd0';Rect ($x+1) ($y+1) 1 2 '6b7f8b'}}
  foreach($x in @(6,55)){Rect $x 18 3 27 '192632';Rect $x 20 1 23 $accent;Rect ($x-1) 25 5 3 '7f909b';Rect ($x-1) 38 5 3 '7f909b'}
  if($name -match 'glass'){
   Rect 12 12 40 40 '397484';Rect 14 14 36 36 '203d4c';for($i=0;$i -lt 24;$i++){Rect (17+$i) (17+$i) 2 1 '9ed1d6'}
  }elseif($name -match 'controller'){
   Rect 12 17 40 28 '899ca6';Rect 14 19 36 24 '111f2c';Rect 16 21 24 18 '203c4a'
   $signal='5b747d';if($name -match '_formed'){$signal='7be8b3'}
   for($y=24;$y -lt 38;$y+=4){Rect 18 $y 18 1 '355863'}
   for($x=18;$x -lt 38;$x++){ $y=30;if($x -ge 25 -and $x -le 27){$y=25};if($x -ge 28 -and $x -le 30){$y=34};Rect $x $y 1 2 $signal }
   foreach($y in @(23,29,35)){Rect 43 $y 5 3 $signal};Rect 20 48 24 6 '172431';for($x=22;$x -lt 44;$x+=4){Rect $x 50 2 2 'a1b4bb'}
  }elseif($name -match 'item_'){
   Rect 13 19 38 26 '82929a';Rect 15 21 34 22 '101b26';for($y=24;$y -le 38;$y+=4){Rect 17 $y 30 1 '3e5261'}
   $ink='65c8dc';$dir=1;if($name -match 'output'){$ink='e5ae66';$dir=-1}
   Rect 23 30 18 4 $ink;for($i=0;$i -lt 8;$i++){Rect (32+$dir*$i) (24+$i) 3 2 $ink;Rect (32+$dir*$i) (38-$i) 3 2 $ink}
  }elseif($name -match 'energy'){
   Rect 17 17 30 30 '9a7e66';Rect 19 19 26 26 '15222e';foreach($x in @(23,37)){Rect $x 23 4 17 '71543e';Rect $x 24 2 14 'e9bd74'}
   for($i=0;$i -lt 12;$i++){Rect (34-[int]($i/2)) (21+$i) 4 2 'edcb83';Rect (31-[int]($i/2)) (31+$i) 4 2 'edcb83'}
  }elseif($name -match 'bottom|casing'){
   Rect 14 18 36 28 '17232e';Rect 15 18 34 2 '748998';for($y=23;$y -lt 44;$y+=4){Rect 18 $y 28 2 '4d6370';Rect 18 ($y+2) 28 1 '0e1922'}
   if($name -match 'casing'){Rect 28 20 8 24 '344a58';Rect 30 20 2 24 $accent}
  }elseif($name -match 'top'){
   for($y=15;$y -lt 49;$y++){for($x=15;$x -lt 49;$x++){$d=[Math]::Abs($x-31.5)+[Math]::Abs($y-31.5);if($d -lt 23){$hex='142733';if($d -gt 20){$hex='9db0b9'}elseif($d -gt 17){$hex=$accent};Rect $x $y 1 1 $hex}}}
   for($y=24;$y -lt 42;$y+=4){Rect 24 $y 16 2 '536e7b'};Rect 30 21 4 22 '91b3be'
  }else{
   Rect 12 17 40 29 '101e29';foreach($x in @(16,28,40)){Rect $x 19 8 24 '577b86';Rect ($x+1) 21 6 19 '244651';Rect ($x+2) 22 2 16 $accent;Rect ($x+4) 24 1 10 $light;Rect $x 19 8 3 'a3b5bb';Rect $x 41 8 3 '8498a4'
    if($name -match 'life'){for($i=0;$i -lt 5;$i++){Rect ($x+2+($i%2)*2) (24+$i*3) 2 2 $light}}
   };Rect 15 49 19 4 '0e1b25';Rect 16 50 12 2 $accent;Rect 39 49 10 4 $accent
  }
  # Alpha is sampled from the existing asset, never invented or flattened.
  for($y=0;$y -lt 64;$y++){for($x=0;$x -lt 64;$x++){$a=$src.GetPixel([int][Math]::Floor($x*$src.Width/64),[int][Math]::Floor($y*$src.Height/64)).A;$p=$bmp.GetPixel($x,$y);$bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb($a,$p.R,$p.G,$p.B))}}
  $src.Dispose();$bmp.Save((Join-Path $out ($name+'.png')),[Drawing.Imaging.ImageFormat]::Png)
  if(!$PreviewOnly){$bmp.Save($path,[Drawing.Imaging.ImageFormat]::Png)}
 }finally{$script:g.Dispose();$bmp.Dispose();$src.Dispose()}
}
$sheet=[Drawing.Bitmap]::new(1024,1000);$sg=[Drawing.Graphics]::FromImage($sheet);$font=[Drawing.Font]::new('Consolas',10);$brush=[Drawing.SolidBrush]::new([Drawing.Color]::White)
try{$sg.Clear((Color '17212c'));$sg.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor;$sg.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
 for($i=0;$i -lt $names.Count;$i++){$x=($i%4)*256;$y=[int][Math]::Floor($i/4)*250;$im=[Drawing.Bitmap]::new((Join-Path $out ($names[$i]+'.png')));try{$sg.DrawImage($im,$x+32,$y+5,192,192);$sg.DrawString($names[$i].Replace('akaishi_','').Replace('purifier_','purifier_'+[Environment]::NewLine),$font,$brush,$x+8,$y+200)}finally{$im.Dispose()}}
 $sheet.Save((Join-Path $out 'purifier_batch_contact_sheet.png'),[Drawing.Imaging.ImageFormat]::Png)
}finally{$sg.Dispose();$sheet.Dispose();$font.Dispose();$brush.Dispose()}
if(!$PreviewOnly){& node (Join-Path $PSScriptRoot 'validate_purifier_batch.js') --check-output;if($LASTEXITCODE -ne 0){throw 'Postflight failed'}}
Write-Output "Generated $($names.Count) named textures; PreviewOnly=$PreviewOnly"
