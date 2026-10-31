# -*- coding: utf-8 -*-
"""通用 NBT 读取器（只读），用于解析结构文件。"""
import gzip, struct, sys


class Reader:
    def __init__(self, buf):
        self.buf = buf
        self.pos = 0

    def take(self, n):
        out = self.buf[self.pos:self.pos + n]
        self.pos += n
        return out

    def u1(self): return struct.unpack(">b", self.take(1))[0]
    def u2(self): return struct.unpack(">H", self.take(2))[0]
    def i(self): return struct.unpack(">i", self.take(4))[0]
    def l(self): return struct.unpack(">q", self.take(8))[0]
    def f(self): return struct.unpack(">f", self.take(4))[0]
    def d(self): return struct.unpack(">d", self.take(8))[0]
    def s(self):
        n = self.u2()
        return self.take(n).decode("utf-8", errors="replace")

    def payload(self, tid):
        if tid == 1: return self.u1()
        if tid == 2: return struct.unpack(">h", self.take(2))[0]
        if tid == 3: return self.i()
        if tid == 4: return self.l()
        if tid == 5: return self.f()
        if tid == 6: return self.d()
        if tid == 7:
            n = self.i(); return list(self.take(n))
        if tid == 8: return self.s()
        if tid == 9:
            et = self.u1(); n = self.i()
            return [self.payload(et) for _ in range(n)]
        if tid == 10:
            out = {}
            while True:
                t = self.u1()
                if t == 0: return out
                name = self.s()
                out[name] = self.payload(t)
        if tid == 11:
            n = self.i(); return [self.i() for _ in range(n)]
        if tid == 12:
            n = self.i(); return [self.l() for _ in range(n)]
        raise ValueError("unknown tag %d at %d" % (tid, self.pos))


def load(path):
    raw = gzip.open(path, "rb").read()
    r = Reader(raw)
    t = r.u1()
    if t != 10:
        raise ValueError("root not compound: %d" % t)
    r.s()
    return r.payload(10)


if __name__ == "__main__":
    data = load(sys.argv[1])
    print("keys:", list(data.keys()))
    print("size:", data.get("size"), "DataVersion:", data.get("DataVersion"))
    palette = data.get("palette", [])
    print("palette:", len(palette), [p.get("Name") for p in palette][:12])
    blocks = data.get("blocks", [])
    print("blocks:", len(blocks), "样例:", blocks[:3])
    names = [p.get("Name", "?") for p in palette]
    from collections import Counter
    c = Counter(names[b.get("state", 0)] for b in blocks)
    for k, v in c.most_common(10):
        print("  ", k, v)
