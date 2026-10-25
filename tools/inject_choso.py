# -*- coding: utf-8 -*-
"""往结构 NBT 的 entities 列表里塞一只胀相（sukuna:choso）。"""
import gzip
import struct
import sys

PATH = sys.argv[1]
POS = (5.5, 2.0, 10.5)

def rb(data, i, fmt):
    n = struct.calcsize(fmt)
    return struct.unpack_from(fmt, data, i), i + n

def read_payload(t, data, i):
    if t == 1: return data[i], i + 1
    if t == 2: return rb(data, i, ">h")[0][0], i + 2
    if t == 3: return rb(data, i, ">i")[0][0], i + 4
    if t == 4: return rb(data, i, ">q")[0][0], i + 8
    if t == 5: return rb(data, i, ">f")[0][0], i + 4
    if t == 6: return rb(data, i, ">d")[0][0], i + 8
    if t == 7:
        n, i = rb(data, i, ">i"); n = n[0]
        return list(data[i:i + n]), i + n
    if t == 8:
        n, i = rb(data, i, ">H"); n = n[0]
        return data[i:i + n].decode("utf-8"), i + n
    if t == 9:
        ct = data[i]; n, i = rb(data, i + 1, ">i"); n = n[0]
        out = []
        for _ in range(n):
            v, i = read_payload(ct, data, i); out.append(v)
        return (ct, out), i
    if t == 10:
        out = {}
        while True:
            tt = data[i]; i += 1
            if tt == 0: break
            k, i = read_payload(8, data, i)
            v, i = read_payload(tt, data, i)
            out[k] = (tt, v)
        return out, i
    if t == 11:
        n, i = rb(data, i, ">i"); n = n[0]
        out = []
        for _ in range(n):
            v, i = rb(data, i, ">i"); out.append(v[0])
        return out, i
    if t == 12:
        n, i = rb(data, i, ">i"); n = n[0]
        out = []
        for _ in range(n):
            v, i = rb(data, i, ">q"); out.append(v[0])
        return out, i
    raise ValueError(f"未知标签 {t}")

def text(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b

def write_payload(t, v):
    if t == 1: return bytes([v & 0xFF])
    if t == 2: return struct.pack(">h", v)
    if t == 3: return struct.pack(">i", v)
    if t == 4: return struct.pack(">q", v)
    if t == 5: return struct.pack(">f", v)
    if t == 6: return struct.pack(">d", v)
    if t == 7: return struct.pack(">i", len(v)) + bytes(v)
    if t == 8: return text(v)
    if t == 9:
        ct, items = v
        return bytes([ct]) + struct.pack(">i", len(items)) + b"".join(write_payload(ct, x) for x in items)
    if t == 10:
        body = b""
        for k, (tt, vv) in v.items():
            body += bytes([tt]) + text(k) + write_payload(tt, vv)
        return body + b"\x00"
    if t == 11:
        return struct.pack(">i", len(v)) + b"".join(struct.pack(">i", x) for x in v)
    if t == 12:
        return struct.pack(">i", len(v)) + b"".join(struct.pack(">q", x) for x in v)
    raise ValueError(f"未知标签 {t}")

raw = gzip.decompress(open(PATH, "rb").read())
assert raw[0] == 10
i = 3  # tag(1) + 空名字长度(2)
root, _ = read_payload(10, raw, i)

entity = {
    "id": (8, "sukuna:choso"),
    "Pos": (9, (6, list(POS))),
    "Rotation": (9, (5, [0.0, 0.0])),
    "PersistenceRequired": (1, 1),
}
if "entities" in root:
    ct, items = root["entities"][1]
    items.append(entity)
    root["entities"] = (9, (ct, items))
    print("entities 已存在，追加，共", len(items))
else:
    root["entities"] = (9, (10, [entity]))
    print("新建 entities")

out = b"\x0a" + text("") + write_payload(10, root)
open(PATH, "wb").write(gzip.compress(out, mtime=0))
print("已写入", PATH, len(out), "字节原始 NBT")
