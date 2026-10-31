# -*- coding: utf-8 -*-
"""把用户改好的领域内部结构 NBT 烘成 Java 常量（zlib + base64）。

用法: python tools/bake_naoya_layout.py <结构文件.nbt>
输出: src/main/java/character/naoya/NaoyaDomainLayout.java
"""
import base64
import struct
import sys
import zlib

sys.path.insert(0, "tools")
from nbt_read import load

BLACK = "minecraft:black_concrete"
WHITE = "minecraft:white_concrete"
AMETHYST = "minecraft:amethyst_block"
STATE = {BLACK: 0, WHITE: 1, AMETHYST: 2}
NAMES = [BLACK, WHITE, AMETHYST]


def main(path, out):
    data = load(path)
    size = data["size"]
    palette = [p.get("Name") for p in data["palette"]]
    records = []
    for block in data["blocks"]:
        pos = block.get("pos")
        if not pos or len(pos) != 3:
            continue
        name = palette[block.get("state", 0)]
        if name not in STATE:
            continue                      # 空气/其它一律忽略
        x, y, z = pos
        records.append((x, y, z, STATE[name]))
    records.sort(key=lambda r: (r[1], r[2], r[0]))
    raw = bytearray()
    for x, y, z, s in records:
        raw += struct.pack(">BBBB", x, y, z, s)
    payload = base64.b64encode(zlib.compress(bytes(raw), 9)).decode("ascii")

    lines = []
    lines.append("package character.naoya;\n")
    lines.append("/**")
    lines.append(" * 禅院直哉领域「时胞月宫殿」的内部结构（由用户改好的结构文件烘焙，勿手改）。")
    lines.append(" *")
    lines.append(" * <p>尺寸 %d×%d×%d；非空气方块 %d 个；状态 0=黑混凝土 1=白混凝土 2=紫水晶块。" % (
        size[0], size[1], size[2], len(records)))
    lines.append(" * <p>坐标换算（相对领域圆心）：世界 = 圆心 + (x − 22, y − 1, z − 22)。")
    lines.append(" */")
    lines.append("final class NaoyaDomainLayout {")
    lines.append("    static final int SIZE_X = %d, SIZE_Y = %d, SIZE_Z = %d;" % (size[0], size[1], size[2]))
    lines.append("    static final int OFFSET_X = 22, OFFSET_Y = -1, OFFSET_Z = 22;")
    lines.append("    static final int COUNT = %d;" % len(records))
    lines.append("    static final String DATA =")
    for i in range(0, len(payload), 96):
        chunk = payload[i:i + 96]
        tail = ";" if i + 96 >= len(payload) else " +"
        lines.append('        "%s"%s' % (chunk, tail))
    lines.append("")
    lines.append("    private static byte[] bytes;")
    lines.append("")
    lines.append("    /** 解压后的字节：每 4 字节一块（x, y, z, 状态）。 */")
    lines.append("    static synchronized byte[] bytes() {")
    lines.append("        if (bytes != null) return bytes;")
    lines.append("        try {")
    lines.append("            byte[] compressed = java.util.Base64.getDecoder().decode(DATA);")
    lines.append("            java.util.zip.Inflater inflater = new java.util.zip.Inflater();")
    lines.append("            inflater.setInput(compressed);")
    lines.append("            byte[] out = new byte[COUNT * 4];")
    lines.append("            int offset = 0;")
    lines.append("            while (offset < out.length && !inflater.finished()) {")
    lines.append("                offset += inflater.inflate(out, offset, out.length - offset);")
    lines.append("            }")
    lines.append("            inflater.end();")
    lines.append("            bytes = out;")
    lines.append("        } catch (java.util.zip.DataFormatException exception) {")
    lines.append("            throw new IllegalStateException(\"领域结构损坏\", exception);")
    lines.append("        }")
    lines.append("        return bytes;")
    lines.append("    }")
    lines.append("")
    lines.append("    private NaoyaDomainLayout() {}")
    lines.append("}")
    text = "\n".join(lines) + "\n"
    with open(out, "w", encoding="utf-8") as handle:
        handle.write(text)
    print("blocks:", len(records), "raw:", len(raw), "zlib:", len(zlib.compress(bytes(raw), 9)),
          "base64:", len(payload))
    print("wrote", out)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else
         "src/main/java/character/naoya/NaoyaDomainLayout.java")
