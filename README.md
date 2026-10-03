# LyyCore

Minecraft 1.21.1 / NeoForge utility mod for modpack progression.

## Features

- Imaginium ores and alloying recipes
- Imaginary Alloy Forge
- Imaginary Energy Cell and FE/IE conversion
- Imaginary Generator with multi-target output in a fixed 5-block radius (11x11x11 area)
- Item Collector
- Crystal Condensing Frame with waterlogging, receive-only FE input, a 1,024-crystal
  output buffer, and an independent JEI condensation category
- Four Resource Gathering Frames with selectable outputs, mixed-material cores,
  and separate JEI categories for trees, overworld terrain, minerals and the nether
- Infinite-durability Imaginary Disassembler with speed, 5x5 area and vein modes
  (26-neighbor connections, up to 64 blocks including the starting block;
  common stone, dirt and sand terrain is excluded from vein mining through
  the `lyycore:disassembler_vein_excluded` block tag)
- Optional JEI integration

The former Raw Imaginium item is now **Imaginary Crystal** (虚水晶), registered as
`lyycore:imaginary_crystal`. The legacy item ID `lyycore:raw_imaginium` is a registry
alias, so existing item stacks load as the new item and save with its new ID while
retaining their count and components. Built-in recipes, ore drops, catalyst tags,
models and textures use the new ID. The condensation recipe is now
`lyycore:crystal_condensing/imaginary_crystal`; update datapack overrides to its new path.

## Development

### Crystal Condensing Frame

Craft `lyycore:crystal_condensing_frame` with eight imaginary alloy ingots around
one imaginary battery. The frame stores up to 2,147,483,647 IE, receives FE from
all sides at 100 FE per IE, and generates 1 IE every 20 loaded ticks. It produces
one crystal per tick when enough energy and output space are available. The
built-in `lyycore:crystal_condensing/imaginary_crystal` recipe produces imaginary crystals
for 200 IE dry or 100 IE waterlogged.

Use a vanilla water bucket to add water and an empty bucket to remove it; other
right clicks open the screen. Water is not consumed. Production exports crystals
to containers on the frame's left and right, relative to its placement direction;
blocked output retries each second. The single output slot holds up to 1,024
crystals internally; clicks extract legal stacks, and Shift-click transfers as
many as the inventory can hold. Breaking the frame drops all stored crystals.

Condensation uses its own `lyycore:crystal_condensing` recipe type and serializer.
Edit `data/lyycore/recipe/crystal_condensing/imaginary_crystal.json` to change `result`,
`dry_cost`, or `wet_cost`; `result` is one item, and costs are positive IE values.
The machine and JEI read the same recipe. The built-in pack contains one recipe;
if a datapack adds more, the machine uses the lexicographically first recipe ID.
JEI shows the optional, reusable water bucket and both costs. Click the frame
illustration in the GUI to open that category when JEI is installed.

The model and three textures come from `F:/misc/BlockBench/pink_crystal_frame`.
The silver-and-rose GUI is drawn in code and shares its frame illustration with
JEI. Run `./gradlew.bat runGameTestServer build --console=plain` to validate it.

### Resource Gathering Frames

Crystal and resource frames inherit the shared
`CondensingFrameBlock`, `CondensingFrameBlockEntity`, and `CondensingFrameMenu`
implementations for block behavior, energy, the 1,024-item output buffer, export,
and inventory interaction. Each concrete entity supplies its own recipe logic.
All four resource variants use `ResourceGatheringFrameBlock`; the three overworld
variants implement waterlogging in their concrete block class, as does the crystal
frame. The common block base handles placement direction, menus, ticking, and drops. Existing block
IDs and storage keys remain unchanged.

A new resource frame waits for an output selection. Use **Select**
to open the paged item palette; the server validates the selection and saves its
recipe ID. Changing the selection preserves stored output and waits until that
output is removed before producing the new item. Shift-click transfers only what
fits in the player's inventory. Breaking a frame drops its stored output.

| Frame ID | Core ID | Independent recipe type | Default outputs |
| --- | --- | --- | --- |
| `lyycore:tree_gathering_frame` | `lyycore:tree_core` | `lyycore:tree_gathering` | 9 |
| `lyycore:overworld_gathering_frame` | `lyycore:overworld_core` | `lyycore:overworld_gathering` | 25 |
| `lyycore:mineral_gathering_frame` | `lyycore:mineral_core` | `lyycore:mineral_gathering` | 9 |
| `lyycore:nether_gathering_frame` | `lyycore:nether_core` | `lyycore:nether_gathering` | 19 |

Tree outputs are the eight overworld logs and bamboo. Terrain outputs include
natural stone variants, sand/red sand, gravel, soil variants, mud, clay, moss and
obsidian. Mineral outputs are coal, raw copper/iron/gold, redstone, lapis, diamond,
emerald and amethyst shards. Nether outputs include netherrack, soul sand/soil,
quartz, glowstone/dust, basalt, blackstone, magma, gravel, gold nuggets, ancient
debris, both stems and wart blocks, shroomlight and nether wart.

All four produce one item for 200 IE. The three overworld frames support water
buckets and waterlogging, reducing the cost to 100 IE. The nether frame is an
ordinary block without a waterlogged property or liquid interfaces; it uses the
normal Minecraft interaction path and always costs 200 IE.

Each frame is crafted from eight imaginary alloy ingots around its core. Each
core is a shapeless recipe of nine items from its frame's output range, with mixed
ingredients allowed. The item tags `lyycore:gathering/<kind>_products` define those
core ingredients. Production recipes live in `data/lyycore/recipe/<kind>_gathering/`
and have one `result`, `dry_cost`, and (for overworld frames) `wet_cost`. Keep the
corresponding tag in sync when changing the output range in a datapack.

JEI shows four separate categories and their matching frame catalysts. The GUI's
left frame rail opens the matching category; the palette and output slot have
their own interactions. JEI displays only the applicable water option and energy
costs. Models come from `F:/misc/BlockBench/miniature_frames`; core item models use
the same miniature landscapes with the outer white frame removed.

Validation: `./gradlew.bat runGameTestServer build --console=plain`. The resource
frame tests cover recipe isolation and network codecs, mixed core/frame crafting,
selection and persistence, full inventories, energy, export, and bucket behavior.

### Running the project

Requires Java 21.

```powershell
.\gradlew.bat clean build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

The built mod is written to `build/libs`. Runtime balance settings are generated in
`config/lyycore-common.toml`.
