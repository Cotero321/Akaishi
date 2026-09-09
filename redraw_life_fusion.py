import struct,zlib,os
root=os.path.dirname(os.path.abspath(__file__))
base=os.path.join(root,'common','src','main','resources','assets','akaishi','textures')
def png(path,w,h,pix):
 raw=b''.join(b'\0'+bytes(sum((list(pix[y*w+x]) for x in range(w)),[])) for y in range(h))
 def ch(t,d): return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
 data=b'\x89PNG\r\n\x1a\n'+ch(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+ch(b'IDAT',zlib.compress(raw,9))+ch(b'IEND',b'')
 open(path,'wb').write(data)
def armor(w,h,layer):
 p=[(0,0,0,0)]*(w*h)
 for y in range(h):
  for x in range(w):
   active=((x//8+y//8+layer)%2==0) or x in (0,w-1) or y in (0,h-1)
   if active:
    glow=(x+y+layer*7)%11<2
    p[y*w+x]=(38,116,100,255) if glow else ((24,35,43,255) if (x+y)%5 else (74,91,91,255))
 return p
armor_dir=os.path.join(base,'models','armor');os.makedirs(armor_dir,exist_ok=True)
for n in (1,2): png(os.path.join(armor_dir,f'life_fusion_layer_{n}.png'),128,64,armor(128,64,n))
item=os.path.join(base,'item')
for n in ('helmet','chestplate','leggings','boots'):
 png(os.path.join(item,f'akaishi_life_fusion_{n}.png'),32,32,armor(32,32,len(n)))
preview=os.path.join(root,'gui_layouts','life_fusion_armor_rework_preview.png')
png(preview,128,128,armor(128,128,3))
print('generated',preview)
