import os, hashlib, sys

A = r"C:/tmp-merge/mod"
B = r"C:/Users/Administrator/Desktop/sukuna-Shrine/sukuna-Shrine"
SKIP = {'.git', 'build', '.gradle', 'run', 'logs', '.dsh-meow', 'libs',
        'research-photon', '.model-tools', '.preview-deps', '__pycache__'}


def files(root):
    out = set()
    for dp, dn, fn in os.walk(root):
        dn[:] = [d for d in dn if d not in SKIP]
        for f in fn:
            p = os.path.join(dp, f)
            out.add(os.path.relpath(p, root).replace(os.sep, '/'))
    return out


def md5(p):
    h = hashlib.md5()
    with open(p, 'rb') as f:
        for c in iter(lambda: f.read(1 << 20), b''):
            h.update(c)
    return h.hexdigest()


fa, fb = files(A), files(B)
only_a = sorted(fa - fb)
only_b = sorted(fb - fa)
both = fa & fb
diff = [p for p in sorted(both) if md5(os.path.join(A, p)) != md5(os.path.join(B, p))]
print('zip only:', len(only_a), 'ours only:', len(only_b), 'both:', len(both), 'diff:', len(diff))
print('\n--- zip only (first 60) ---')
for p in only_a[:60]:
    print(' +', p)
print('\n--- diff (first 30) ---')
for p in diff[:30]:
    print(' ~', p)
print('\n--- ours only java (first 15) ---')
for p in only_b:
    if p.endswith('.java'):
        print(' -', p)
        if only_b.index(p) > 15:
            break
