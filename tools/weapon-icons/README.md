# Weapon inventory icons

Only Emberfall and Thunder Scythe use these rendered icons. Bows are excluded.

`render_icons.py` runs inside Blender and creates separate scenes without deleting existing content. It renders the current baked item faces using their GUI orientation, orthographic framing, transparent film, and a soft three-light setup. Materials retain the original textures; a small emission contribution prevents shadowed alloy from turning black. The sword render uses its normal single-sword idle geometry, not its energy greatsword form.

Outputs and editable lighting scenes are saved to `F:/misc/BlockBench/weapon_inventory_renders/`. Copy the two PNGs to `src/main/resources/assets/lyycore/textures/item/icons/` after rendering. Current assets are 256×256 RGBA.

`InventoryIconModel` selects a generated image model only in GUI context. It reports flat item lighting before `GuiGraphics` chooses lights. Other contexts delegate to the original model; the existing custom held renderers and world-space scythe animations remain in use. `EmberfallItemRenderer` respects the delegated model returned by `applyTransform` in non-hand contexts.

Emberfall's `energy_weapon_root` scale tracks were multiplied by 1.3 for greatsword growth, heavy attacks and blocking. Zero-scale hidden poses remain zero. The ordinary sword, GUI model geometry and animation timing are unchanged.

Validation: both Blender renders were inspected; `compileJava processResources` passed. No in-game visual verification has been performed.
