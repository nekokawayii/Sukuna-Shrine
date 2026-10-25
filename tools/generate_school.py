# -*- coding: utf-8 -*-
"""生成「学校」结构模板（128×128 占地，主世界自然生成用）。

运行：python tools/generate_school.py --root . [--force]
输出：src/main/resources/data/sukuna/structures/school/main.nbt
"""
from __future__ import annotations

import argparse
import gzip
import json
import struct
from pathlib import Path

SIZE = (128, 40, 128)
DATA_VERSION = 3465
LOOT = "sukuna:chests/school"
blocks: dict[tuple[int, int, int], tuple[str, tuple, dict | None]] = {}


def put(x, y, z, name, properties=None, nbt=None):
    assert 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2], (x, y, z)
    props = tuple(sorted((str(k), str(v)) for k, v in (properties or {}).items()))
    blocks[(x, y, z)] = (name, props, nbt)


def box(x1, y1, z1, x2, y2, z2, name, properties=None):
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                put(x, y, z, name, properties)


def air(x1, y1, z1, x2, y2, z2):
    box(x1, y1, z1, x2, y2, z2, "minecraft:air")


def chest(x, y, z, facing):
    put(x, y, z, "minecraft:chest",
        {"facing": facing, "type": "single", "waterlogged": "false"},
        {"id": (8, "minecraft:chest"), "LootTable": (8, LOOT)})


def desk(x, y, z):
    put(x, y, z, "minecraft:dark_oak_stairs",
        {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    put(x, y, z + 1, "minecraft:oak_slab",
        {"type": "bottom", "waterlogged": "false"})


def lamp(x, y, z):
    put(x, y, z, "minecraft:oak_fence")
    put(x, y + 1, z, "minecraft:lantern",
        {"hanging": "false", "waterlogged": "false"})


def school():
    blocks.clear()
    # ---------- 校园围墙与大门 ----------
    for x in range(2, 126):
        for z in (2, 126):
            box(x, 2, z, x, 3, z, "minecraft:cobblestone_wall")
    for z in range(2, 127):
        for x in (2, 126):
            box(x, 2, z, x, 3, z, "minecraft:cobblestone_wall")
    air(60, 2, 0, 68, 4, 4)          # 南侧大门洞
    box(58, 2, 1, 70, 4, 1, "minecraft:stone_bricks")

    # ---------- 主楼外壳 ----------
    X1, X2, Z1, Z2 = 8, 120, 8, 80
    box(X1, 0, Z1, X2, 2, Z2, "minecraft:stone_bricks")      # 地基
    air(X1 + 2, 3, Z1 + 2, X2 - 2, 19, Z2 - 2)               # 内部清空
    box(X1, 3, Z1, X2, 3, Z2, "minecraft:dark_oak_planks")   # 一层地板
    # 外墙（两层）
    for (y1, y2) in ((4, 10), (12, 18)):
        box(X1, y1, Z1, X2, y2, Z1, "minecraft:stone_bricks")
        box(X1, y1, Z2, X2, y2, Z2, "minecraft:stone_bricks")
        box(X1, y1, Z1, X1, y2, Z2, "minecraft:stone_bricks")
        box(X2, y1, Z1, X2, y2, Z2, "minecraft:stone_bricks")
    box(X1, 11, Z1, X2, 11, Z2, "minecraft:dark_oak_planks")  # 二层地板
    # 窗户（每 8 格一组）
    for x in range(X1 + 4, X2 - 3, 8):
        for (y1, y2) in ((6, 8), (14, 16)):
            box(x, y1, Z1, x + 2, y2, Z1, "minecraft:glass")
            box(x, y1, Z2, x + 2, y2, Z2, "minecraft:glass")
    for z in range(Z1 + 6, Z2 - 3, 10):
        for (y1, y2) in ((6, 8), (14, 16)):
            box(X1, y1, z, X1, y2, z + 2, "minecraft:glass")
            box(X2, y1, z, X2, y2, z + 2, "minecraft:glass")
    # 正门（南墙中央）
    air(60, 4, Z2, 68, 7, Z2)
    box(58, 4, Z2, 59, 7, Z2, "minecraft:stone_bricks")
    box(69, 4, Z2, 70, 7, Z2, "minecraft:stone_bricks")
    box(60, 8, Z2, 68, 8, Z2, "minecraft:stone_bricks")

    # ---------- 走廊 ----------
    for (y1, y2) in ((4, 10), (12, 18)):
        box(60, y1, Z1 + 2, 68, y2, Z2 - 2, "minecraft:air")
    box(64, 3, Z1 + 2, 64, 3, Z2 - 2, "minecraft:red_carpet")
    # 二层走廊楼板缺口 + 楼梯
    box(62, 11, 70, 66, 11, 74, "minecraft:air")
    for i in range(9):
        put(64, 3 + i, 74 - i, "minecraft:dark_oak_stairs",
            {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})

    # ---------- 教室隔墙（每层各 3 间） ----------
    for (y1, y2) in ((4, 10), (12, 18)):
        for z in (30, 52):
            box(X1 + 2, y1, z, 59, y2, z, "minecraft:stone_bricks")
            box(69, y1, z, X2 - 2, y2, z, "minecraft:stone_bricks")
            air(40, y1, z, 43, y1 + 2, z)     # 左侧门洞
            air(84, y1, z, 87, y1 + 2, z)     # 右侧门洞
        box(52, y1, Z1 + 2, 52, y2, Z2 - 2, "minecraft:stone_bricks")
        box(76, y1, Z1 + 2, 76, y2, Z2 - 2, "minecraft:stone_bricks")
        for z in (20, 42, 66):
            air(52, y1, z, 52, y1 + 2, z + 1)
            air(76, y1, z, 76, y1 + 2, z + 1)

    # ---------- 课桌与讲台 ----------
    for y in (3, 11):
        for cx in (24, 38, 60 + 0, 88, 104):
            for rz in (18, 26):
                desk(cx, y, rz)
                desk(cx + 6, y, rz)
        for z in (14, 38):
            for x in (14, 20, 94, 100, 110):
                desk(x, y, z)

    # ---------- 讲台与办公室 ----------
    box(40, 3, 12, 50, 3, 14, "minecraft:polished_deepslate")
    put(45, 4, 13, "minecraft:lectern",
        {"facing": "north", "has_book": "false", "powered": "false"})
    box(112, 3, 70, 118, 3, 76, "minecraft:dark_oak_planks")
    chest(114, 4, 74, "north")
    chest(116, 4, 74, "north")
    put(112, 4, 76, "minecraft:bookshelf")
    put(113, 4, 76, "minecraft:bookshelf")

    # ---------- 屋顶（阶梯坡） ----------
    for step in range(7):
        y = 19 + step
        x1, x2 = X1 + step, X2 - step
        z1, z2 = Z1 + step, Z2 - step
        if x1 > x2 or z1 > z2:
            break
        box(x1, y, z1, x2, y, z1, "minecraft:deepslate_tiles")
        box(x1, y, z2, x2, y, z2, "minecraft:deepslate_tiles")
        box(x1, y, z1, x1, y, z2, "minecraft:deepslate_tiles")
        box(x2, y, z1, x2, y, z2, "minecraft:deepslate_tiles")
        if step < 6:
            air(x1 + 1, y, z1 + 1, x2 - 1, y, z2 - 1)

    # ---------- 庭院 ----------
    box(60, 1, 84, 68, 1, 124, "minecraft:stone_bricks")
    for x in (30, 90):
        box(x, 1, 88, x, 1, 122, "minecraft:gravel")
    for (x, z) in ((40, 96), (86, 96), (40, 116), (86, 116)):
        lamp(x, 2, z)
    for x in range(14, 114, 12):
        box(x, 1, 120, x + 2, 2, 122, "minecraft:oak_leaves")
    # 主楼通往大门的路
    box(60, 3, Z2 + 1, 68, 3, 84, "minecraft:dark_oak_planks")

    # ---------- 静态检查：主走廊连续可走 ----------
    for z in range(Z1 + 2, Z2 - 1):
        assert blocks[(60, 4, z)][0] == "minecraft:air"
        assert blocks[(64, 3, z)][0] in ("minecraft:red_carpet", "minecraft:air", "minecraft:dark_oak_planks", "minecraft:dark_oak_stairs")


def text(value: str) -> bytes:
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


def make_template():
    school()
    palette, index, entries = [], {}, []
    for pos, (name, props, nbt) in sorted(blocks.items(), key=lambda i: (i[0][1], i[0][2], i[0][0])):
        key = (name, props)
        if key not in index:
            index[key] = len(palette)
            state = {"Name": (8, name)}
            if props:
                state["Properties"] = (10, {k: (8, v) for k, v in props})
            palette.append(state)
        entry = {"pos": (9, (3, list(pos))), "state": (3, index[key])}
        if nbt is not None:
            entry["nbt"] = (10, nbt)
        entries.append(entry)
    root = {
        "DataVersion": (3, DATA_VERSION),
        "size": (9, (3, list(SIZE))),
        "palette": (9, (10, palette)),
        "blocks": (9, (10, entries)),
        "entities": (9, (10, [])),
    }
    raw = b"\x0a" + text("") + payload(10, root)
    return gzip.compress(raw, mtime=0), {"size": list(SIZE), "blocks": len(entries), "palette": len(palette)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--force", action="store_true")
    args = parser.parse_args()
    target = args.root / "src/main/resources/data/sukuna/structures/school/main.nbt"
    if target.exists() and not args.force:
        raise FileExistsError(f"已存在，确认后加 --force：{target}")
    data, report = make_template()
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)
    print(json.dumps({"output": str(target), **report}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
