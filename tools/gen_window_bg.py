"""Generates res/drawable-nodpi/bg_window_glow.png: the night-blue gradient with two soft glows.
It is used as android:windowBackground. One pre-rendered bitmap is a single draw per pixel,
while the same look built from a layer-list of three gradients would paint every pixel three times.
Usage: python3 tools/gen_window_bg.py <res dir>"""
import math, random, struct, sys, zlib

W, H = 360, 800
TOP, MID, BOTTOM = (0x0A, 0x2A, 0x5E), (0x07, 0x1B, 0x3D), (0x04, 0x0E, 0x24)
GLOWS = [  # (center x, center y, radius as a fraction of the diagonal, color, strength)
    (0.85, 0.08, 0.42, (0x40, 0x8C, 0xFF), 0.55),
    (0.00, 0.62, 0.40, (0x00, 0xB4, 0xFF), 0.30),
]
random.seed(7)


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def pixel(x, y):
    t = y / (H - 1)
    color = lerp(TOP, MID, t / 0.45) if t < 0.45 else lerp(MID, BOTTOM, (t - 0.45) / 0.55)
    diag = math.hypot(W, H)
    for gx, gy, r, gc, strength in GLOWS:
        d = math.hypot(x - gx * W, y - gy * H) / (r * diag)
        if d < 1:
            a = strength * (1 - d) ** 2          # smooth falloff
            color = lerp(color, gc, a)
    # +-0.5 noise hides 8-bit banding in the long, dark gradient
    return bytes(max(0, min(255, round(c + random.uniform(-0.5, 0.5)))) for c in color)


rows = [b"\x00" + b"".join(pixel(x, y) for x in range(W)) for y in range(H)]
chunk = lambda t, d: struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
data = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 2, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(b"".join(rows), 9)) + chunk(b"IEND", b""))
with open(sys.argv[1] + "/drawable-nodpi/bg_window_glow.png", "wb") as f:
    f.write(data)
