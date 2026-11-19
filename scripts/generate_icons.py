#!/usr/bin/env python3
"""
رندرر اختصاصی آیکون سه‌بعدی مرمرین MarbleBr (بدون نیاز به کتابخانه خارجی).
خروجی‌ها:
- docs/images/icon-512.png (۵۱۲×۵۱۲ برای README)
- app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png و ic_launcher_round.png
"""

from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path


def write_png(path: Path, width: int, height: int, rgba_bytes: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    raw_rows = bytearray()
    stride = width * 4
    for y in range(height):
        raw_rows.append(0)  # Filter type 0 (None)
        raw_rows.extend(rgba_bytes[y * stride : (y + 1) * stride])

    def make_chunk(chunk_type: bytes, data: bytes) -> bytes:
        crc = zlib.crc32(chunk_type + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + chunk_type + data + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw_rows), level=9)
    png_data = (
        b"\x89PNG\r\n\x1a\n"
        + make_chunk(b"IHDR", ihdr)
        + make_chunk(b"IDAT", idat)
        + make_chunk(b"IEND", b"")
    )
    path.write_bytes(png_data)


def clamp01(val: float) -> float:
    return 0.0 if val < 0.0 else (1.0 if val > 1.0 else val)


def smoothstep(edge0: float, edge1: float, x: float) -> float:
    t = clamp01((x - edge0) / (edge1 - edge0))
    return t * t * (3.0 - 2.0 * t)


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def render_marble_icon(size: int, circular_mask: bool = False) -> bytes:
    buf = bytearray(size * size * 4)
    inv_size = 1.0 / size

    for py in range(size):
        ny = (py + 0.5) * inv_size * 2.0 - 1.0  # -1 .. 1
        for px in range(size):
            nx = (px + 0.5) * inv_size * 2.0 - 1.0  # -1 .. 1

            # Masking (Squircle or Circle)
            if circular_mask:
                dist_edge = math.hypot(nx, ny)
                alpha = 1.0 - smoothstep(0.95, 0.995, dist_edge)
            else:
                # Rounded superellipse / squircle
                ax = abs(nx)
                ay = abs(ny)
                squircle = (ax ** 4.8 + ay ** 4.8) ** (1.0 / 4.8)
                alpha = 1.0 - smoothstep(0.95, 0.995, squircle)

            if alpha <= 0.0:
                continue

            # 1. Background Material You Deep Sapphire-Indigo Gradient + Aurora
            bg_t = clamp01((nx + ny + 2.0) * 0.25)
            bg_r = lerp(11.0, 31.0, bg_t)
            bg_g = lerp(19.0, 48.0, bg_t)
            bg_b = lerp(43.0, 110.0, bg_t)

            aurora_d = math.hypot(nx + 0.12, ny + 0.16)
            aurora = math.exp(-aurora_d * aurora_d * 2.6)
            bg_r += aurora * 42.0
            bg_g += aurora * 95.0
            bg_b += aurora * 145.0

            r, g, b = bg_r, bg_g, bg_b

            # 2. Multi-layer Soft Drop Shadow below the marble sphere
            sh_dx = nx / 0.56
            sh_dy = (ny - 0.54) / 0.14
            sh_dist = math.hypot(sh_dx, sh_dy)
            if sh_dist < 1.3:
                shadow_strength = 0.55 * math.exp(-sh_dist * sh_dist * 2.1)
                r *= (1.0 - shadow_strength)
                g *= (1.0 - shadow_strength)
                b *= (1.0 - shadow_strength)

            # 3. 3D Marble Sphere Ray Intersection
            sp_cx, sp_cy = 0.0, -0.04
            sp_radius = 0.62
            sx = (nx - sp_cx) / sp_radius
            sy = (ny - sp_cy) / sp_radius
            r2 = sx * sx + sy * sy

            if r2 < 1.04:
                sphere_cov = 1.0 - smoothstep(0.97, 1.02, math.sqrt(r2))
                sz = math.sqrt(max(0.0, 1.0 - min(1.0, r2)))

                # Lighting vectors
                lx, ly, lz = -0.42, -0.48, 0.77
                ndotl = max(0.0, sx * lx + sy * ly + sz * lz)

                # Base Material You Marble Palette (Sapphire -> Indigo -> Teal subsurface)
                m_r = lerp(26.0, 92.0, ndotl)
                m_g = lerp(24.0, 168.0, ndotl)
                m_b = lerp(74.0, 248.0, ndotl)

                # Subsurface teal-amethyst glow in lower right hemisphere
                sub = clamp01((sx * 0.45 + sy * 0.55 + 0.4) * sz)
                m_r += sub * 45.0
                m_g += sub * 112.0
                m_b += sub * 105.0

                # Procedural 3D Marble Veins (multi-frequency sinusoidal turbulence)
                phase1 = sx * 4.6 + sy * 2.8 + sz * 2.2 + 0.65 * math.sin(sy * 5.5 + sz * 3.0)
                vein1 = math.exp(-((math.sin(phase1)) ** 2) * 18.0)

                phase2 = sx * 2.4 - sy * 5.1 + sz * 1.9 + 0.55 * math.cos(sx * 4.8 - sz * 2.5)
                vein2 = math.exp(-((math.sin(phase2)) ** 2) * 24.0)

                # Apply delicate cyan-silver and lavender marble veins
                m_r = lerp(m_r, 224.0, vein1 * 0.52 * sz)
                m_g = lerp(m_g, 246.0, vein1 * 0.58 * sz)
                m_b = lerp(m_b, 255.0, vein1 * 0.62 * sz)

                m_r = lerp(m_r, 116.0, vein2 * 0.45 * sz)
                m_g = lerp(m_g, 248.0, vein2 * 0.52 * sz)
                m_b = lerp(m_b, 229.0, vein2 * 0.52 * sz)

                # 4. Subtle Persistent-Session Infinity Loop glowing inside the marble core
                # Lemniscate of Bernoulli in (sx, sy + 0.10)
                ix = sx * 1.42
                iy = (sy + 0.10) * 1.55
                a = 0.54
                lem_lhs = (ix * ix + iy * iy) ** 2
                lem_rhs = (a * a) * (ix * ix - iy * iy)
                lem_dist = abs(lem_lhs - lem_rhs)
                infinity_glow = math.exp(-lem_dist * 85.0) * (sz ** 0.7)

                m_r = lerp(m_r, 245.0, clamp01(infinity_glow * 0.78))
                m_g = lerp(m_g, 255.0, clamp01(infinity_glow * 0.88))
                m_b = lerp(m_b, 252.0, clamp01(infinity_glow * 0.90))

                # Subtle keyhole dot below infinity loop
                kd = math.hypot(sx, sy - 0.25)
                key_glow = math.exp(-kd * kd * 140.0) * sz
                m_r = lerp(m_r, 255.0, clamp01(key_glow * 0.75))
                m_g = lerp(m_g, 255.0, clamp01(key_glow * 0.75))
                m_b = lerp(m_b, 255.0, clamp01(key_glow * 0.75))

                # 5. Glossy Specular Reflection & Fresnel Rim Light
                # Half-vector between light and view (0, 0, 1)
                hx, hy, hz = lx, ly, lz + 1.0
                hlen = math.sqrt(hx * hx + hy * hy + hz * hz)
                hx, hy, hz = hx / hlen, hy / hlen, hz / hlen
                ndoth = max(0.0, sx * hx + sy * hy + sz * hz)
                spec_broad = (ndoth ** 26.0) * 0.55
                spec_sharp = (ndoth ** 110.0) * 0.92

                fresnel = ((1.0 - sz) ** 3.2) * clamp01(0.5 + 0.5 * (sx + sy))

                m_r += (spec_broad + spec_sharp) * 255.0 + fresnel * 90.0
                m_g += (spec_broad + spec_sharp) * 255.0 + fresnel * 235.0
                m_b += (spec_broad + spec_sharp) * 255.0 + fresnel * 225.0

                r = lerp(r, m_r, sphere_cov)
                g = lerp(g, m_g, sphere_cov)
                b = lerp(b, m_b, sphere_cov)

            idx = (py * size + px) * 4
            buf[idx] = int(clamp01(r / 255.0) * 255.0 + 0.5)
            buf[idx + 1] = int(clamp01(g / 255.0) * 255.0 + 0.5)
            buf[idx + 2] = int(clamp01(b / 255.0) * 255.0 + 0.5)
            buf[idx + 3] = int(clamp01(alpha) * 255.0 + 0.5)

    return bytes(buf)


def main() -> None:
    root = Path(__file__).resolve().parent.parent

    # 1. Generate 512x512 README icon
    readme_icon_path = root / "docs" / "images" / "icon-512.png"
    write_png(readme_icon_path, 512, 512, render_marble_icon(512, circular_mask=False))
    print(f"Generated {readme_icon_path}")

    # 2. Generate all Android mipmap density icons
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    res_dir = root / "app" / "src" / "main" / "res"
    for folder, px_size in densities.items():
        square_path = res_dir / folder / "ic_launcher.png"
        round_path = res_dir / folder / "ic_launcher_round.png"
        write_png(square_path, px_size, px_size, render_marble_icon(px_size, circular_mask=False))
        write_png(round_path, px_size, px_size, render_marble_icon(px_size, circular_mask=True))
        print(f"Generated {folder} ({px_size}x{px_size})")


if __name__ == "__main__":
    main()
