"""Generates res/drawable-xxhdpi/bg_boarding_pass.9.png: a white ticket shape with notches on both sides.
The image is white so the layout can tint it (light/dark theme). Usage: python3 tools/gen_ninepatch.py <res dir>"""
import struct, sys, zlib

W, H = 96, 120          # content size in px (xxhdpi: 32dp x 40dp)
RADIUS = 24             # corner radius (8dp)
NOTCH = 18              # notch radius (6dp), centered vertically on each side
SS = 4                  # supersampling for anti-aliased edges


def inside(x, y):
    # rounded rectangle
    cx = min(max(x, RADIUS), W - RADIUS)
    cy = min(max(y, RADIUS), H - RADIUS)
    if (x - cx) ** 2 + (y - cy) ** 2 > RADIUS ** 2:
        return False
    # side notches
    for nx in (0, W):
        if (x - nx) ** 2 + (y - H / 2) ** 2 < NOTCH ** 2:
            return False
    return True


img = [[(0, 0, 0, 0)] * (W + 2) for _ in range(H + 2)]
for y in range(H):
    for x in range(W):
        hits = sum(inside(x + (i + 0.5) / SS, y + (j + 0.5) / SS) for i in range(SS) for j in range(SS))
        img[y + 1][x + 1] = (255, 255, 255, round(255 * hits / SS ** 2))

BLACK = (0, 0, 0, 255)
for x in range(30, 66):                       # horizontal stretch: between the corners
    img[0][x + 1] = BLACK
for y in list(range(26, 40)) + list(range(80, 94)):   # vertical stretch: above and below the notches
    img[y + 1][0] = BLACK
for x in range(20, 76):                       # content padding (horizontal)
    img[H + 1][x + 1] = BLACK
for y in range(20, 100):                      # content padding (vertical)
    img[y + 1][W + 1] = BLACK


def png(rows):
    h, w = len(rows), len(rows[0])
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in rows)
    chunk = lambda t, d: struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


with open(sys.argv[1] + "/drawable-xxhdpi/bg_boarding_pass.9.png", "wb") as f:
    f.write(png(img))
