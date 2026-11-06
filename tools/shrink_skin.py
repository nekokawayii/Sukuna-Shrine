import struct, zlib, sys

src = r"C:\Users\Administrator\.dsh\attachments\v1\objects\76\76ad9b995d0084c8c56e2b8e734f9f4f35567307730a85184b183e52d3d5e2b1"
dst = r"src/main/resources/assets/sukuna/textures/entity/sukuna_true_form.png"

raw = open(src, "rb").read()
assert raw[:8] == b"\x89PNG\r\n\x1a\n"
pos = 8
w = h = bit = ctype = None
idat = b""
while pos < len(raw):
    ln = struct.unpack(">I", raw[pos:pos+4])[0]; typ = raw[pos+4:pos+8]
    data = raw[pos+8:pos+8+ln]; pos += 12 + ln
    if typ == b"IHDR":
        w, h, bit, ctype = struct.unpack(">IIBB", data[:10])
    elif typ == b"IDAT":
        idat += data
    elif typ == b"IEND":
        break
assert bit == 8 and ctype in (2, 6), (bit, ctype)
bpp = 4 if ctype == 6 else 3
data = zlib.decompress(idat)
stride = w * bpp
rows = []
prev = bytearray(stride)
p = 0
for y in range(h):
    f = data[p]; p += 1
    line = bytearray(data[p:p+stride]); p += stride
    if f == 1:
        for i in range(bpp, stride): line[i] = (line[i] + line[i-bpp]) & 255
    elif f == 2:
        for i in range(stride): line[i] = (line[i] + prev[i]) & 255
    elif f == 3:
        for i in range(stride):
            a = line[i-bpp] if i >= bpp else 0
            line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
    elif f == 4:
        for i in range(stride):
            a = line[i-bpp] if i >= bpp else 0
            b = prev[i]
            c = prev[i-bpp] if i >= bpp else 0
            pp = a + b - c
            pa, pb, pc = abs(pp-a), abs(pp-b), abs(pp-c)
            pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
            line[i] = (line[i] + pr) & 255
    rows.append(bytes(line))
    prev = line

# 最近邻缩到 64x64
T = 64
out = []
for y in range(T):
    sy = min(h-1, y * h // T)
    row = bytearray([0])
    for x in range(T):
        sx = min(w-1, x * w // T)
        px = rows[sy][sx*bpp: sx*bpp+3]
        row += bytes(px) + (bytes([255]) if bpp == 3 else bytes([rows[sy][sx*bpp+3]]))
    out.append(bytes(row))
rawout = b"".join(out)

def chunk(t, d):
    return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t+d) & 0xffffffff)

png = b"\x89PNG\r\n\x1a\n"
png += chunk(b"IHDR", struct.pack(">IIBBBBB", T, T, 8, 6, 0, 0, 0))
png += chunk(b"IDAT", zlib.compress(rawout, 9))
png += chunk(b"IEND", b"")
open(dst, "wb").write(png)
print("written", dst, len(png), "bytes")
