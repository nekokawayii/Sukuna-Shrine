"""生成死灭洄游「黑色立方体结界」结构：48x48x48 空心黑混凝土壳 + +Z 面红色缝隙。"""
import gzip, os, struct

S = 48
BLACK = "minecraft:black_concrete"
GLASS = "minecraft:black_stained_glass"
RED = "minecraft:red_concrete"
palette = [BLACK, GLASS, RED, "jujutsu_kaisen_cursed_paths:culling_seam"]
pidx = {n: i for i, n in enumerate(palette)}

blocks = []
def put(x, y, z, name):
    blocks.append((x, y, z, pidx[name]))

for x in range(S):
    for y in range(S):
        for z in range(S):
            edge = x in (0, S - 1) or z in (0, S - 1) or y in (0, S - 1)
            if not edge:
                continue
            # 顶部用黑玻璃（半透明感），其余黑混凝土
            name = GLASS if y == S - 1 else BLACK
            put(x, y, z, name)

# +Z 面（z=S-1）中央开一道 3 宽 3 高 的缝隙，外圈红混凝土
cx, cy = S // 2, 2
for dx in range(-2, 3):
    for dy in range(-1, 4):
        x, y, z = cx + dx, cy + dy, S - 1
        inside = abs(dx) <= 1 and 0 <= dy <= 2
        if inside:
            # 缝隙本体：可走进去的判定方块
            put(x, y, z, "jujutsu_kaisen_cursed_paths:culling_seam")
        else:
            put(x, y, z, RED)

# 内部地板（y=1）铺黑混凝土，避免cube内部悬空
for x in range(1, S - 1):
    for z in range(1, S - 1):
        put(x, 1, z, BLACK)

# ---- NBT 写入 ----
def tag_string(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b

def tag_int(v): return struct.pack(">i", v)
def tag_double(v): return struct.pack(">d", v)

def payload_int_list(vals):
    # TAG_List 的 payload：元素类型(3=Int) + 长度 + 元素
    return struct.pack(">b", 3) + struct.pack(">i", len(vals)) + b"".join(struct.pack(">i", v) for v in vals)

out = bytearray()
out += b"\x0a" + tag_string("")  # root compound, empty name

# DataVersion
out += b"\x03" + tag_string("DataVersion") + tag_int(3465)

# size
out += b"\x09" + tag_string("size") + payload_int_list([S, S, S])

# palette: list of compound {Name: string}
out += b"\x09" + tag_string("palette") + b"\x0a" + struct.pack(">i", len(palette))
for name in palette:
    out += b"\x08" + tag_string("Name") + tag_string(name)
    out += b"\x00"  # end compound

# blocks: list of compound {pos:[I], state:int}
out += b"\x09" + tag_string("blocks") + b"\x0a" + struct.pack(">i", len(blocks))
for x, y, z, state in blocks:
    out += b"\x09" + tag_string("pos") + payload_int_list([x, y, z])
    out += b"\x03" + tag_string("state") + tag_int(state)
    out += b"\x00"

# entities: empty list of compounds
out += b"\x09" + tag_string("entities") + b"\x0a" + struct.pack(">i", 0)

out += b"\x00"  # end root

dst = "src/main/resources/data/jujutsu_kaisen_cursed_paths/structures/culling_cube/main.nbt"
os.makedirs(os.path.dirname(dst), exist_ok=True)
with open(dst, "wb") as f:
    f.write(gzip.compress(bytes(out), 9, mtime=0))
print("nbt bytes", os.path.getsize(dst), "blocks", len(blocks))
