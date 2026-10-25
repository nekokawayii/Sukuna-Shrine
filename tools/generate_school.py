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
    # ================= 四面教学楼围合式学校 =================
    # 中央操场：X 34..94, Z 30..98
    box(34, 1, 30, 94, 1, 98, "minecraft:grass_block")
    box(36, 1, 32, 92, 1, 96, "minecraft:gravel")        # 跑道
    box(42, 1, 38, 86, 1, 90, "minecraft:grass_block")    # 内场
    box(12, 1, 28, 118, 1, 29, "minecraft:stone_bricks")  # 南北通道
    box(12, 1, 99, 118, 1, 100, "minecraft:stone_bricks")
    box(31, 1, 28, 33, 1, 100, "minecraft:stone_bricks")  # 东西通道
    box(95, 1, 28, 97, 1, 100, "minecraft:stone_bricks")

    def wing(x1, z1, x2, z2, name_hint):
        """一个教学楼翼：地基 + 两层 + 平顶 + 栏杆 + 隔间/课桌/灯。"""
        box(x1, 0, z1, x2, 2, z2, "minecraft:stone_bricks")
        air(x1 + 1, 4, z1 + 1, x2 - 1, 19, z2 - 1)
        box(x1, 3, z1, x2, 3, z2, "minecraft:dark_oak_planks")     # 一层地板
        for (y1, y2) in ((4, 10), (12, 18)):                        # 两层外墙
            box(x1, y1, z1, x2, y2, z1, "minecraft:stone_bricks")
            box(x1, y1, z2, x2, y2, z2, "minecraft:stone_bricks")
            box(x1, y1, z1, x1, y2, z2, "minecraft:stone_bricks")
            box(x2, y1, z1, x2, y2, z2, "minecraft:stone_bricks")
        box(x1, 11, z1, x2, 11, z2, "minecraft:dark_oak_planks")   # 二层地板
        box(x1, 19, z1, x2, 19, z2, "minecraft:deepslate_tiles")   # 屋顶楼板（封顶）
        for (x, z) in ((x1, z1), (x2, z1), (x1, z2), (x2, z2)):
            box(x, 20, z, x, 22, z, "minecraft:stone_bricks")      # 角柱
        # 女儿墙
        box(x1, 20, z1, x2, 21, z1, "minecraft:stone_bricks")
        box(x1, 20, z2, x2, 21, z2, "minecraft:stone_bricks")
        box(x1, 20, z1, x1, 21, z2, "minecraft:stone_bricks")
        box(x2, 20, z1, x2, 21, z2, "minecraft:stone_bricks")
        # ---- 楼梯间：每翼靠内侧一段，从一层直上二层 ----
        sx = x1 + 4
        sz = z1 + 5
        box(sx - 1, 3, sz - 1, sx + 2, 10, sz + 8, "minecraft:stone_bricks")   # 楼梯井墙
        air(sx, 3, sz, sx + 1, 10, sz + 7)
        for i in range(7):
            y = 4 + i
            put(sx, y, sz + i, "minecraft:dark_oak_stairs",
                {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
            put(sx + 1, y, sz + i, "minecraft:dark_oak_stairs",
                {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
        air(sx, 11, sz + 7, sx + 1, 11, sz + 9)   # 二层地板开口
        box(sx - 1, 3, sz - 1, sx + 2, 3, sz + 8, "minecraft:dark_oak_planks")
        box(sx, 10, sz + 9, sx + 1, 17, sz + 9, "minecraft:air")
        lamp(sx + 2, 4, sz + 2)
        lamp(sx + 2, 12, sz + 2)

        # 窗（外侧两面）
        long_x = (x2 - x1) >= (z2 - z1)
        if long_x:
            for x in range(x1 + 4, x2 - 2, 8):
                for (y1, y2) in ((6, 8), (14, 16)):
                    box(x, y1, z1, x + 2, y2, z1, "minecraft:glass")
                    box(x, y1, z2, x + 2, y2, z2, "minecraft:glass")
        else:
            for z in range(z1 + 4, z2 - 2, 8):
                for (y1, y2) in ((6, 8), (14, 16)):
                    box(x1, y1, z, x1, y2, z + 2, "minecraft:glass")
                    box(x2, y1, z, x2, y2, z + 2, "minecraft:glass")
        # 每个翼两层的隔间与门（沿长边每 14 格一道墙）
        if long_x:
            cuts = list(range(x1 + 14, x2 - 6, 14))
            for (y1, y2) in ((4, 10), (12, 18)):
                for cx in cuts:
                    box(cx, y1, z1 + 1, cx, y2, z2 - 1, "minecraft:stone_bricks")
            for cx in cuts:
                for y in (4, 12):
                    air(cx, y, z1 + 2, cx, y + 2, z1 + 3)          # 门洞
            # 课桌（放在地板之上 y=4 / y=12）
            for cx in range(x1 + 4, x2 - 3, 14):
                for row, z in ((0, z1 + 5), (1, z1 + 9)):
                    y = 4 if row == 0 else 12
                    for k in range(3):
                        desk(cx + k * 2, y, z)
            for x in range(x1 + 6, x2 - 3, 12):
                lamp(x, 4, z2 - 3)
                lamp(x, 12, z2 - 3)
        else:
            cuts = list(range(z1 + 14, z2 - 6, 14))
            for (y1, y2) in ((4, 10), (12, 18)):
                for cz in cuts:
                    box(x1 + 1, y1, cz, x2 - 1, y2, cz, "minecraft:stone_bricks")
            for cz in cuts:
                for y in (4, 12):
                    air(x1 + 2, y, cz, x1 + 3, y + 2, cz)
            for cz in range(z1 + 4, z2 - 3, 14):
                for row, x in ((0, x1 + 5), (1, x1 + 9)):
                    y = 4 if row == 0 else 12
                    for k in range(3):
                        desk(x, y, cz + k * 2)
            for z in range(z1 + 6, z2 - 3, 12):
                lamp(x2 - 3, 4, z)
                lamp(x2 - 3, 12, z)
        # 一层门（朝操场一侧）
        return

    # 四个翼（外框 10..118 / 10..118，中央留操场）
    wing(10, 10, 118, 26, "north")
    wing(10, 102, 118, 118, "south")
    wing(10, 30, 30, 98, "west")
    wing(98, 30, 118, 98, "east")

    # ---------- 入口（南翼中间开门） ----------
    air(60, 4, 102, 68, 7, 102)
    air(60, 3, 103, 68, 3, 104)
    box(58, 4, 102, 59, 7, 102, "minecraft:stone_bricks")
    box(69, 4, 102, 70, 7, 102, "minecraft:stone_bricks")

    # ---------- 办公室/图书角（北翼中部，带箱子） ----------
    box(58, 3, 14, 70, 3, 22, "minecraft:dark_oak_planks")
    chest(60, 4, 20, "north")
    chest(62, 4, 20, "north")
    put(66, 4, 20, "minecraft:bookshelf")
    put(67, 4, 20, "minecraft:bookshelf")
    put(64, 4, 16, "minecraft:lectern",
        {"facing": "south", "has_book": "false", "powered": "false"})
    for x in (56, 72):
        lamp(x, 4, 12)

    # ---------- 操场设施 ----------
    for (x, z) in ((40, 36), (88, 36), (40, 92), (88, 92)):
        lamp(x, 2, z)
    for z in (44, 58, 72, 86):
        lamp(38, 2, z); lamp(90, 2, z)
    for x in range(46, 84, 12):
        box(x, 2, 46, x + 2, 3, 48, "minecraft:oak_leaves")
        box(x, 2, 82, x + 2, 3, 84, "minecraft:oak_leaves")
    # 操场中央旗杆
    box(63, 2, 63, 65, 2, 65, "minecraft:stone_bricks")
    box(64, 3, 64, 64, 9, 64, "minecraft:oak_fence")
    put(65, 9, 64, "minecraft:white_banner")

    # ================= 环形连廊：把四面教学楼连起来 =================
    def walkway(x1, z1, x2, z2):
        box(x1, 3, z1, x2, 3, z2, "minecraft:dark_oak_planks")     # 廊道地面
        air(x1 + 1, 4, z1 + 1, x2 - 1, 7, z2 - 1)                  # 廊道净空
        box(x1, 3, z1, x2, 7, z1, "minecraft:stone_bricks")        # 两侧矮墙
        box(x1, 3, z2, x2, 7, z2, "minecraft:stone_bricks")
        box(x1, 3, z1, x1, 7, z2, "minecraft:stone_bricks")
        box(x2, 3, z1, x2, 7, z2, "minecraft:stone_bricks")
        box(x1, 8, z1, x2, 8, z2, "minecraft:deepslate_tiles")     # 顶棚
        for x in range(x1 + 3, x2 - 1, 6):
            box(x, 4, z1, x + 1, 6, z1, "minecraft:glass")
            box(x, 4, z2, x + 1, 6, z2, "minecraft:glass")
        for x in range(x1 + 4, x2 - 1, 8):
            box(x, 7, (z1 + z2) // 2, x, 7, (z1 + z2) // 2, "minecraft:lantern",
                {"hanging": "true", "waterlogged": "false"})

    walkway(13, 27, 115, 29)    # 北连廊
    walkway(13, 99, 115, 101)   # 南连廊
    walkway(31, 31, 33, 97)     # 西连廊
    walkway(95, 31, 97, 97)     # 东连廊
    # 连廊与各翼之间的门洞
    for (x, z) in ((56, 26), (64, 26), (72, 26), (56, 102), (64, 102), (72, 102)):
        air(x, 4, z, x + 1, 6, z)
    for (x, z) in ((30, 46), (30, 64), (30, 82), (98, 46), (98, 64), (98, 82)):
        air(x, 4, z, x, 6, z + 1)

    # 走廊灯具（走廊为翼内通道，补充顶灯避免全黑）
    for x in range(16, 114, 12):
        box(x, 9, 28, x, 9, 29, "minecraft:lantern",
            {"hanging": "true", "waterlogged": "false"})
        box(x, 9, 99, x, 9, 100, "minecraft:lantern",
            {"hanging": "true", "waterlogged": "false"})
    for z in range(34, 96, 12):
        box(31, 9, z, 32, 9, z, "minecraft:lantern",
            {"hanging": "true", "waterlogged": "false"})
        box(96, 9, z, 97, 9, z, "minecraft:lantern",
            {"hanging": "true", "waterlogged": "false"})


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
