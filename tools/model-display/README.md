# GUI lighting parity

Save the Blockbench source with the intended Display / GUI light first. The
current batch, starting with the creature summoning beacon, uses front lighting:
`front_gui_light: true` in BB and `gui_light: "front"` on the final Java item model.
The mapping is in `gui_lighting_manifest.json`. The handcraft table is registered
in game as `reforging_table`.

After importing/exporting model resources, run:

```text
python tools/model-display/sync_gui_lighting.py --apply
python tools/model-display/sync_gui_lighting.py --check
```

Add future models to the manifest. These commands synchronize only GUI lighting;
they retain model geometry, texture colors, emissive surfaces, world shading and
all artist-authored display transforms, including the existing single X mirror.
The machine importer also reads the BB setting directly rather than relying on
the parent model's default lighting.

The revolver has no game item registration here yet. Its exported BB model and
`blue_revolver.display.json` carry the matching GUI light for its eventual loader.
GLB alone has no Minecraft GUI-light configuration.

Blender inventory icons for Emberfall and Thunder Scythe are excluded. Frost Chain
already uses a 2D icon and is excluded as well; do not restore its obsolete 3D coil
as an inventory model. The comparison captures and pre-change backups are in
`F:/misc/BlockBench/inventory_lighting_fix_20261015/`.

Matching configuration does not establish pixel-identical rendering across BB,
Minecraft, and shader/resource packs. Verify in-game after resource reload; do not
report a BB capture as an in-game screenshot.

## Mirrored item surfaces and two-cell mirror lighting

GUI light configuration alone was insufficient. `MirroredItemModel` corrects
quad winding after an odd reflection in the artist's display transform. It keeps
the original normals and complete UV/light vertex records, and propagates the
correction to all item render passes. The correction is registered for the three
live 3D game items above, not world block models or Blender icons. When adding a
new reflected 3D item, register it in that wrapper as well as the manifest.

The world mirror previously rendered the entire two-cell geometry from the lower
block while the upper block had an empty model. Vertices above Y=1 were therefore
lit from the lower cell. `import_mirror.py` clips the original Java model's faces
into cell-local lower and upper meshes, preserving total surface area and UVs.
The original full Java model remains in `item/mirror_full.json` for inventory use.
The Blockbench source, item transforms, four facing variants, and collision shapes
are unchanged. The reforging table's placed block model remains unchanged.

Regenerate and verify after changing the mirror:

```text
python tools/model-display/import_mirror.py
python tools/model-display/check_mirror_cells.py
python tools/model-display/sync_gui_lighting.py --check
gradlew -I tools/model-display/lighting-check.gradle checkGuiLighting
```

The Java check uses Minecraft's real PoseStack and baked-quad classes without a
game window. It verifies winding versus transformed normals, the pre-existing GUI
Y reflection, positive/even scale transforms, pass wrapping, and unchanged vertex
attributes. Client code changes require a game restart, not just F3+T.
