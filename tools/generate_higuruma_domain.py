#!/usr/bin/env python3
"""生成「诛伏赐死法庭」结构 NBT（供结构方块载入后自己调）。

结构：半径 25 的黑色混凝土球壳 + 桦木地板 + 法庭（法官席/过道/旁听长椅/柱子）
      + 一圈 8 座 15 格断头台。空位不写（= 结构空位，保持原世界地形）。
"""
import gzip, struct, math
from pathlib import Path

DATA_VERSION = 3465
R = 25
SIZE = (R * 2 + 1, R * 2 + 1, R * 2 + 1)   # 51^3，中心 (25,25,25)
CENTER = (R, R, R)


def text(value):
    data = value.encode("utf-8")
    assert len(data) <= 65535
    return struct.pack(">H", len(data)) + data


def payload(tag_type, value):
    if tag_type == 3:
        return struct.pack(">i", value)
    if tag_type == 8:
        return text(value)
    if tag_type == 9:
        child_type, items = value
        return bytes([child_type]) + struct.pack(">i", len(items)) + b"".join(payload(child_type, i) for i in items)
    if tag_type == 10:
        return b"".join(bytes([k]) + text(n) + payload(k, v) for n, (k, v) in value.items()) + b"\x00"
    raise ValueError(tag_type)


blocks = {}
def put(x, y, z, name):
    blocks[(x, y, z)] = name


def build():
    cx, cy, cz = CENTER
    for dx in range(-R, R + 1):
        for dy in range(-R, R + 1):
            for dz in range(-R, R + 1):
                dist = math.sqrt(dx * dx + dy * dy + dz * dz)
                if dist > R:
                    continue
                x, y, z = cx + dx, cy + dy, cz + dz
                if dist > R - 1.5:
                    put(x, y, z, "minecraft:black_concrete")
                    continue
                if dy == -1:
                    put(x, y, z, "minecraft:birch_planks")
                    continue
                if dy == 0:
                    if -16 <= dz <= -12 and abs(dx) <= 7:
                        put(x, y, z, "minecraft:dark_oak_planks")
                    elif -12 < dz < 0 and abs(dx) <= 1:
                        put(x, y, z, "minecraft:polished_andesite")
                    elif 2 <= dz <= 16 and abs(dx) <= 8 and dz % 3 == 2:
                        put(x, y, z, "minecraft:dark_oak_planks")
                    else:
                        put(x, y, z, "minecraft:birch_planks")
                    continue
                if 1 <= dy <= 2 and -16 <= dz <= -12 and abs(dx) <= 7:
                    put(x, y, z, "minecraft:dark_oak_planks")
                elif dy == 2 and -16 <= dz <= -14 and abs(dx) <= 6:
                    put(x, y, z, "minecraft:dark_oak_planks")
                elif 1 <= dy <= 14 and abs(dx) == 9 and dz in (-10, 10):
                    put(x, y, z, "minecraft:stripped_dark_oak_log")
    # 8 座断头台（15 格高）
    for i in range(8):
        a = math.pi * 2 * i / 8
        bx = cx + round(math.cos(a) * (R - 5))
        bz = cz + round(math.sin(a) * (R - 5))
        for yoff in range(1, 16):
            for k in (0, 1):
                name = "minecraft:iron_bars" if yoff >= 15 else "minecraft:dark_oak_planks"
                put(bx + k, cy + yoff, bz, name)
        put(bx, cy + 16, bz, "minecraft:iron_bars")


def make():
    build()
    palette, index, entries = [], {}, []
    for pos, name in sorted(blocks.items(), key=lambda i: (i[0][1], i[0][2], i[0][0])):
        if name not in index:
            index[name] = len(palette)
            palette.append({"Name": (8, name)})
        entries.append({"pos": (9, (3, list(pos))), "state": (3, index[name])})
    root = {
        "DataVersion": (3, DATA_VERSION),
        "size": (9, (3, list(SIZE))),
        "palette": (9, (10, palette)),
        "blocks": (9, (10, entries)),
        "entities": (9, (10, [])),
    }
    return gzip.compress(b"\x0a" + text("") + payload(10, root), mtime=0), len(entries)


if __name__ == "__main__":
    data, count = make()
    target = Path("src/main/resources/data/sukuna/structures/higuruma_domain/main.nbt")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)
    desk = Path.home() / "Desktop" / "诛伏赐死法庭.nbt"
    desk.write_bytes(data)
    print("size", SIZE, "blocks", count, "palette", "->", target, "|", desk)
