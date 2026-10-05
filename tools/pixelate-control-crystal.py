"""Pixelate the supplied control crystal image; requires Pillow and NumPy."""

import argparse
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image


PALETTE = [
    (109, 34, 9), (151, 46, 5), (185, 59, 0), (214, 76, 0),
    (237, 99, 0), (255, 130, 0), (255, 160, 0), (255, 187, 12),
    (255, 209, 39), (255, 229, 83), (255, 241, 139), (255, 253, 207),
]


def connected_region(mask: np.ndarray, seed: tuple[int, int]) -> np.ndarray:
    """Four-connected flood fill with explicit boolean arithmetic."""
    result = np.zeros(mask.shape, dtype=bool)
    pending = deque([seed])
    height, width = mask.shape
    while pending:
        x, y = pending.popleft()
        if x < 0 or y < 0 or x >= width or y >= height or result[y, x] or not mask[y, x]:
            continue
        result[y, x] = True
        pending.extend(((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)))
    return result


def isolate_crystal(source: Image.Image) -> Image.Image:
    rgb = np.asarray(source.convert('RGB'), dtype=np.int16)
    red, green, blue = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    # Select the saturated amber object, excluding the dark background/glow.
    selected = (red > 110) & (red - blue > 65) & (green - blue > 30)
    center = (source.width // 2, source.height // 2)
    if not selected[center[1], center[0]]:
        raise ValueError('Expected the crystal at the center of the reference.')
    # Keep the center-connected crystal; discard detached floating sparks.
    selected = connected_region(selected, center)
    # Fill any dark internal facets without filling the outside background.
    selected = ~connected_region(~selected, (0, 0))
    mask = Image.fromarray((selected * 255).astype(np.uint8))
    crystal = source.convert('RGBA')
    crystal.putalpha(mask)
    return crystal.crop(mask.getbbox())


def pixelate(crystal: Image.Image, size: int) -> Image.Image:
    # Box-filter once at target resolution, then use hard alpha and a fixed palette.
    available = size - max(2, size // 8)
    scale = available / max(crystal.size)
    dimensions = tuple(max(1, round(value * scale)) for value in crystal.size)
    reduced = crystal.resize(dimensions, Image.Resampling.BOX)
    pixels = np.asarray(reduced, dtype=np.int32)
    rgb = pixels[..., :3]
    palette = np.asarray(PALETTE, dtype=np.int32)
    distance = ((rgb[..., None, :] - palette) ** 2).sum(axis=-1)
    mapped = palette[distance.argmin(axis=-1)].astype(np.uint8)
    alpha = np.where(pixels[..., 3] >= 128, 255, 0).astype(np.uint8)
    mapped[alpha == 0] = 0
    icon = Image.fromarray(np.dstack((mapped, alpha)))
    canvas = Image.new('RGBA', (size, size))
    canvas.paste(icon, ((size - icon.width) // 2, (size - icon.height) // 2))
    output = np.asarray(canvas)
    assert set(np.unique(output[..., 3])) <= {0, 255}
    opaque = output[output[..., 3] == 255]
    assert len(opaque) > size * size // 4, 'Crystal disappeared during conversion.'
    assert len(np.unique(opaque, axis=0)) <= len(PALETTE)
    return canvas


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('reference', type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    work = root / 'build/control-crystal-texture-work'
    work.mkdir(parents=True, exist_ok=True)
    with Image.open(args.reference) as source:
        crystal = isolate_crystal(source)
    main_icon = pixelate(crystal, 32)
    destination = root / 'src/main/resources/assets/lyycore/textures/item/control_crystal.png'
    main_icon.save(destination)
    preview = Image.new('RGB', (256, 256), '#2D2F37')
    enlarged = main_icon.resize((256, 256), Image.Resampling.NEAREST)
    preview.paste(enlarged, (0, 0), enlarged)
    preview.save(work / 'preview.png')
    print(f'Saved {destination}: 32x32, <=12 opaque colors, binary alpha.')
    print('Saved nearest-neighbor preview in build/.')


if __name__ == '__main__':
    main()
