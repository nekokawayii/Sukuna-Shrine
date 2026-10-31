# -*- coding: utf-8 -*-
"""生成直哉领域「时胞月宫殿」内部结构的 NBT（结构方块可直接载入，尺寸 ≤48）。"""
import gzip, struct, io, math

# ── 与 NaoyaDomain.build 一致的几何：地板(黑) + 子宫三圈褶皱(白) + 中央眼睛(白球+黑瞳) + 白路 ──
blocks = {}   # (x,y,z) -> block name

def put(x, y, z, name):
    blocks[(int(x), int(y), int(z))] = name

# 地板：黑混凝土圆盘（半径 22，留出球壁空间）
for dx in range(-22, 23):
    for dz in range(-22, 23):
        if dx * dx + dz * dz <= 22 * 22:
            put(dx, -1, dz, "minecraft:black_concrete")

# 子宫：三圈白色褶皱（半径 8/14/20，上下起伏）
for i in range(3):
    ring = 8 + i * 6
    samples = int(ring * 8)
    for s in range(samples):
        angle = s * math.pi * 2 / samples
        y = math.sin(angle * 3) * 3.0
        put(round(math.cos(angle) * ring), round(2 + y), round(math.sin(angle) * ring), "minecraft:white_concrete")

# 中央的眼睛：白眼球球壳（半径 3~4）+ 黑色瞳孔（朝 +Z）
for dy in range(-4, 5):
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            d = dx * dx + dy * dy + dz * dz
            if 9 <= d <= 16:
                put(dx, dy + 3, dz, "minecraft:white_concrete")
for dy in range(-1, 2):
    for dx in range(-1, 2):
        for dz in (2, 3):
            put(dx, dy + 3, dz, "minecraft:black_concrete")

# 路：从眼睛前方到球壁附近，3 格宽，黑底上铺白
for step in range(4, 25):   # 路最长到 z=24（结构方块单轴上限 48）
    for w in (-1, 0, 1):
        put(w, -1, step, "minecraft:white_concrete")

# ── 写入结构 NBT ─────────────────────────────────────────
names = sorted(set(blocks.values()))
palette_index = {name: i for i, name in enumerate(names)}

xs = [p[0] for p in blocks]; ys = [p[1] for p in blocks]; zs = [p[2] for p in blocks]
minx, miny, minz = min(xs), min(ys), min(zs)
sx = max(xs) - minx + 1; sy = max(ys) - miny + 1; sz = max(zs) - minz + 1
assert sx <= 48 and sy <= 48 and sz <= 48, (sx, sy, sz)

class NBTTag:
    def __init__(self):
        self.buf = io.BytesIO()
    def _byte(self, v): self.buf.write(struct.pack(">b", v))
    def _short(self, v): self.buf.write(struct.pack(">h", v))
    def _int(self, v): self.buf.write(struct.pack(">i", v))
    def _long(self, v): self.buf.write(struct.pack(">q", v))
    def _float(self, v): self.buf.write(struct.pack(">f", v))
    def _double(self, v): self.buf.write(struct.pack(">d", v))
    def _string(self, s):
        raw = s.encode("utf-8")
        self.buf.write(struct.pack(">H", len(raw)))
        self.buf.write(raw)
    def tag(self, tid, name=""):
        self._byte(tid)
        if tid != 0:
            self._string(name)
    def named_int(self, name, v):
        self.tag(3, name); self._int(v)
    def named_int_list(self, name, values):
        self.tag(9, name); self._byte(3); self._int(len(values))
        for v in values: self._int(v)
    def named_compound_list(self, name, count):
        self.tag(9, name); self._byte(10); self._int(count)
    def end(self):
        self.tag(0)
        return self.buf.getvalue()

out = io.BytesIO()
# 根：TAG_Compound "" （手动拼，因为需要逐个子标签）
root = NBTTag()
root.tag(10, "")
# size: TAG_List of int
root.named_int_list("size", [sx, sy, sz])
# palette: TAG_List of TAG_Compound {Name}
root.named_compound_list("palette", len(names))
for name in names:
    root.tag(8, "Name"); root._string(name)
    root.tag(0)
# blocks: TAG_List of TAG_Compound {pos, state}
positions = sorted(blocks.keys())
root.named_compound_list("blocks", len(positions))
for (x, y, z) in positions:
    root.tag(9, "pos"); root._byte(3); root._int(3)
    for v in (x - minx, y - miny, z - minz):
        root._int(v)
    root.named_int("state", palette_index[blocks[(x, y, z)]])
    root.tag(0)
# entities: 空列表
root.named_compound_list("entities", 0)
# DataVersion（1.20.1 = 3465）
root.named_int("DataVersion", 3465)
root.end()
raw = root.buf.getvalue()

paths = [
    r"C:\Users\Administrator\Desktop\naoya_domain.nbt",
    r"C:\Users\Administrator\Desktop\sukuna-Shrine\sukuna-Shrine\src\main\resources\data\sukuna\structures\naoya_domain.nbt",
]
import os
for p in paths:
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with gzip.open(p, "wb") as f:
        f.write(raw)
print("size:", (sx, sy, sz), "blocks:", len(positions), "palette:", len(names))
for p in paths:
    print("wrote", p, os.path.getsize(p), "bytes")
