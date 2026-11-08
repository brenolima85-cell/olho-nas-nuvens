#!/usr/bin/env python3
"""Desenha o ícone e o banner originais do protótipo.

Não usa logo de clínica nem arte de outros jogos. Os PNG gerados são a marca
provisória (texto), não um logo oficial. Pillow é necessário só para regenerar.
"""

from __future__ import annotations

import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    sys.stderr.write("Pillow não está instalado. Os PNG já versionados não dependem disso.\n")
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"

SKY_TOP = (62, 134, 197)
SKY_BOTTOM = (246, 226, 192)
SUN = (255, 224, 138)
CLOUD = (247, 251, 255)
NAVY = (27, 51, 68)
IRIS = (31, 122, 107)
IRIS_INNER = (73, 196, 180)
PUPIL = (26, 32, 48)
LID = (240, 194, 168)
HILL = (126, 175, 114)

FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
    "/Library/Fonts/Arial Bold.ttf",
]


def font(size: int) -> ImageFont.ImageFont:
    for candidate in FONT_CANDIDATES:
        path = Path(candidate)
        if path.is_file():
            return ImageFont.truetype(str(path), size)
    sys.stderr.write("Nenhuma fonte TTF encontrada para o banner.\n")
    sys.exit(1)


def mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def vertical_gradient(image: Image.Image) -> None:
    draw = ImageDraw.Draw(image)
    height = image.height
    for y in range(height):
        draw.line([(0, y), (image.width, y)], fill=mix(SKY_TOP, SKY_BOTTOM, y / max(1, height - 1)))


def eye(draw: ImageDraw.ImageDraw, cx: float, cy: float, radius: float) -> None:
    wing = radius * 0.95
    draw.polygon(
        [
            (cx - radius * 0.2, cy),
            (cx - radius * 0.4, cy - radius * 0.35),
            (cx - radius - wing, cy - radius * 0.05),
            (cx - radius * 0.85, cy + radius * 0.28),
        ],
        fill=CLOUD,
        outline=NAVY,
    )
    draw.polygon(
        [
            (cx + radius * 0.2, cy),
            (cx + radius * 0.4, cy - radius * 0.35),
            (cx + radius + wing, cy - radius * 0.05),
            (cx + radius * 0.85, cy + radius * 0.28),
        ],
        fill=CLOUD,
        outline=NAVY,
    )
    draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=(255, 253, 248), outline=NAVY, width=max(2, int(radius * 0.08)))
    draw.ellipse((cx - radius * 0.52, cy - radius * 0.48, cx + radius * 0.52, cy + radius * 0.56), fill=IRIS)
    draw.ellipse((cx - radius * 0.30, cy - radius * 0.22, cx + radius * 0.30, cy + radius * 0.38), fill=IRIS_INNER)
    draw.ellipse((cx - radius * 0.16, cy - radius * 0.08, cx + radius * 0.16, cy + radius * 0.24), fill=PUPIL)
    draw.ellipse((cx - radius * 0.28, cy - radius * 0.34, cx - radius * 0.08, cy - radius * 0.14), fill=(255, 255, 255))
    draw.chord(
        (cx - radius, cy - radius, cx + radius, cy + radius),
        start=200,
        end=340,
        fill=LID,
    )


def icon(size: int) -> Image.Image:
    image = Image.new("RGB", (size, size), SKY_TOP)
    vertical_gradient(image)
    draw = ImageDraw.Draw(image)
    sun = size * 0.78
    draw.ellipse((sun, size * 0.08, sun + size * 0.28, size * 0.36), fill=SUN)
    draw.ellipse((size * 0.08, size * 0.62, size * 0.42, size * 0.86), fill=CLOUD)
    eye(draw, size * 0.52, size * 0.48, size * 0.24)
    return image


def banner() -> Image.Image:
    image = Image.new("RGB", (320, 180), SKY_TOP)
    vertical_gradient(image)
    draw = ImageDraw.Draw(image)
    draw.ellipse((250, 18, 310, 78), fill=SUN)
    draw.ellipse((18, 36, 78, 78), fill=CLOUD)
    draw.ellipse((48, 28, 120, 84), fill=CLOUD)
    draw.rectangle((0, 150, 320, 180), fill=HILL)
    eye(draw, 78, 112, 34)
    draw.rounded_rectangle((124, 40, 308, 138), radius=16, fill=(255, 249, 240))
    title = font(22)
    clinic = font(15)
    note = font(12)
    draw.text((136, 48), "Olho nas Nuvens", font=title, fill=NAVY)
    draw.text((136, 80), "Clínica de Olhos", font=clinic, fill=NAVY)
    draw.text((136, 106), "marca provisória", font=note, fill=NAVY)
    return image


def main() -> None:
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, size in densities.items():
        path = RES / folder / "ic_launcher.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        icon(size).save(path, "PNG")
    banner_path = RES / "drawable-xhdpi" / "tv_banner.png"
    banner_path.parent.mkdir(parents=True, exist_ok=True)
    banner().save(banner_path, "PNG")
    print(f"ícones e banner gravados em {RES}")


if __name__ == "__main__":
    main()
