"""Builds the release icons from the application's own PNGs.

Writes build/icons/dis6502.ico (Windows) and build/icons/dis6502.icns
(macOS) from src/images/main-32x32.png, -48x48.png and -64x64.png. Both
formats embed the PNG data unchanged - nothing is redrawn or rescaled.
The release workflow passes them to jpackage; Linux takes the 64x64 PNG
directly. Run from the repository root after changing those PNGs:

    python build/make-icons.py
"""

import os
import struct

SIZES = (32, 48, 64)
# icns PNG entry types by pixel size; icns has no PNG type for 48x48.
ICNS_TYPES = {32: b"icp5", 64: b"icp6"}


def read_png(size):
    with open(f"src/images/main-{size}x{size}.png", "rb") as f:
        data = f.read()
    width, height = struct.unpack(">II", data[16:24])
    if (width, height) != (size, size):
        raise ValueError(f"main-{size}x{size}.png is {width}x{height}.")
    return data


def make_ico(pngs):
    header = struct.pack("<HHH", 0, 1, len(pngs))
    offset = 6 + 16 * len(pngs)
    entries = b""
    for size, data in pngs:
        entries += struct.pack("<BBBBHHII", size, size, 0, 0, 1, 32, len(data), offset)
        offset += len(data)
    return header + entries + b"".join(data for _, data in pngs)


def make_icns(pngs):
    body = b"".join(ICNS_TYPES[size] + struct.pack(">I", 8 + len(data)) + data
                    for size, data in pngs if size in ICNS_TYPES)
    return b"icns" + struct.pack(">I", 8 + len(body)) + body


def main():
    pngs = [(size, read_png(size)) for size in SIZES]
    os.makedirs("build/icons", exist_ok=True)
    with open("build/icons/dis6502.ico", "wb") as f:
        f.write(make_ico(pngs))
    with open("build/icons/dis6502.icns", "wb") as f:
        f.write(make_icns(pngs))
    print("Wrote build/icons/dis6502.ico and build/icons/dis6502.icns.")


if __name__ == "__main__":
    main()
