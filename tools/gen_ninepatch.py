"""Generates res/drawable-xxhdpi/bg_boarding_pass.9.png: a frosted-glass ticket with notches on both sides.
Translucent white fill plus a brighter 1dp rim, so the night-blue window shows through.
Usage: python3 tools/gen_ninepatch.py <res dir>"""
import struct, sys, zlib

W, H = 120, 180         # content size in px (xxhdpi: 40dp x 60dp)
RADIUS = 48             # corner radius (16dp)
NOTCH = 18              # notch radius (6dp), centered vertically on each side
SS = 4                  # supersampling for anti-aliased edges


RIM = 3                 # rim width (1dp)
FILL_ALPHA, RIM_ALPHA = 0.13, 0.30


def inside(x, y, inset=0):
    # rounded rectangle, shrunk by `inset`
    r = RADIUS - inset
    cx = min(max(x, RADIUS), W - RADIUS)
    cy = min(max(y, RADIUS), H - RADIUS)
    if x < inset or y < inset or x > W - inset or y > H - inset:
        return False
    if (x - cx) ** 2 + (y - cy) ** 2 > r ** 2:
        return False
    # side notches, grown by `inset`
    for nx in (0, W):
        if (x - nx) ** 2 + (y - H / 2) ** 2 < (NOTCH + inset) ** 2:
            return False
    return True


img = [[(0, 0, 0, 0)] * (W + 2) for _ in range(H + 2)]
for y in range(H):
    for x in range(W):
        pts = [(x + (i + 0.5) / SS, y + (j + 0.5) / SS) for i in range(SS) for j in range(SS)]
        outer = sum(inside(px, py) for px, py in pts) / SS ** 2
        inner = sum(inside(px, py, RIM) for px, py in pts) / SS ** 2
        alpha = inner * FILL_ALPHA + (outer - inner) * RIM_ALPHA
        img[y + 1][x + 1] = (255, 255, 255, round(255 * alpha))

BLACK = (0, 0, 0, 255)
for x in range(55, 65):                       # horizontal stretch: between the corners
    img[0][x + 1] = BLACK
for y in list(range(54, 64)) + list(range(116, 126)):  # vertical stretch: between corners and notches
    img[y + 1][0] = BLACK
for x in range(20, 100):                      # content padding (horizontal)
    img[H + 1][x + 1] = BLACK
for y in range(20, 160):                      # content padding (vertical)
    img[y + 1][W + 1] = BLACK


def png(rows):
    h, w = len(rows), len(rows[0])
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in rows)
    chunk = lambda t, d: struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


with open(sys.argv[1] + "/drawable-xxhdpi/bg_boarding_pass.9.png", "wb") as f:
    f.write(png(img))
