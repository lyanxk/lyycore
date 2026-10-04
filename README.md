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
- Four Miniature Production Blocks for concrete, flowers and bee products, ocean
  plants and blocks, and ocean monument resources, with separate JEI categories
- Infinite-durability Imaginary Disassembler with speed, 5x5 area and vein modes
  (26-neighbor connections, up to 64 blocks including the starting block;
  common stone, dirt and sand terrain is excluded from vein mining through
  the `lyycore:disassembler_vein_excluded` block tag)
- Imaginary Grapple with a 20-block hook, contact input, gradual traction and automatic landing retraction
- Optional JEI integration
- Ten milestone advancements grouped into materials, production and building,
  and rituals and crafting
- Client-only Origin Sea sky test with a fractured pink firmament, blue nebulae,
  mirrored horizon and GPU-animated falling shards: `/originsea on`.
  See [controls and rendering notes](docs/origin-sea.md).

Natural generation, internal transfers, machine storage and recipe costs use IE.
The generator supplies **512 FE (5.12 IE) per target per tick** by default, and the alloy forge
generates 1 IE every 20 ticks with a 1,000,000 IE capacity. Native machines connect
through the IE capability; external FE input and output convert at 100 FE per IE.
Incomplete input is retained as a byte containing 0..99 FE, and full IE stores
reject new FE input. The forge screen and JEI energy costs display IE.
The generator's `fePerTargetTick` setting is in FE. Native IE receivers get whole
IE with the fraction accumulated across ticks and saves: 25 ticks supply exactly
128 IE. FE-only receivers get 512 FE each tick. The erroneous `iePerTargetTick`
key migrates back to `fePerTargetTick`, preserving its original FE numerical value;
an existing explicit FE key takes precedence. The forge's legacy `passiveFe` key
migrates to `passiveIe`, retaining its numerical value. Existing forge energy and
recipe progress also retain their numerical values in IE.
Alloy ingots take 20 seconds and consume 100 IE per recipe; imaginary batteries
consume 50 IE per recipe and take 100 seconds.

The former Raw Imaginium item is now **Imaginary Crystal** (虚晶), registered as
`lyycore:imaginary_crystal`. The legacy item ID `lyycore:raw_imaginium` is a registry
alias, so existing item stacks load as the new item and save with its new ID while
retaining their count and components. Built-in recipes, ore drops, catalyst tags,
models and textures use the new ID. The condensation recipe is now
`lyycore:crystal_condensing/imaginary_crystal`; update datapack overrides to its new path.

## Development

### Advancement guide

Obtaining an imaginary crystal opens the `lyycore:progression/root` advancement
tab. Three silent grouping nodes arrange the remaining milestones side by side;
parents organize the display and do not impose completion prerequisites.
Titles, descriptions and short completion instructions are localized in Chinese
and English. Existing recipe-unlock advancements remain separate.

Materials reward workbench alloy crafting and crafting the alloy forge.
Production covers obtaining any of the five basic frames, an item collector,
crafting a basic imaginary energy generator, and obtaining any of the nine
miniature production blocks. Rituals cover a successful guardian summon,
defeating a guardian and obtaining a pure crystal (both required), and crafting
the imaginary crafting table.

Crafting milestones use recipe-crafted criteria, not possession checks. The
imaginary crafting table records its completed recipe and grants crafting credit
when a player takes the actual result, including shift-click and after save/load.
The gate records who inserted the crystal block and credits that player only
after a successful summon; a closer bystander receives no summon credit.

### Journey's End?

Nine imaginary crystals craft one `crystal_block`; eight imaginary alloy ingots
around that block craft the `imaginary_gate`. Place the gate at its bottom center
with a clear 5-by-5 vertical plane. Every part opens the same one-slot inventory;
breaking a part removes the structure and drops one gate plus its inventory.
In the Overworld, insert one crystal block to summon an Imaginary Guardian three
blocks in front. Peaceful difficulty, blocked spawn space, and an existing living
guardian prevent another summon without consuming the ingredient.

The stationary guardian has 300 health and 5 armor. Its 11-second invulnerable
summoning restores its health, then spins its orbiting crystals and repels nearby
players. It acquires players and other hostile mobs within 6 blocks (excluding
other Imaginary Guardians and allies), retaliates against attackers, and
disengages beyond 20 blocks. Out of combat it heals 10 health per second. Every
four seconds it fires four destructible, shield-blockable crystals for 10 physical
damage each; blocking disables the shield. Crystals travel at 0.35 blocks per tick
and home toward their launch target for the first 2 seconds, then continue straight
for the remainder of their 5-second lifetime. Independently, every two seconds in
combat it fires a horizontal eight-direction barrage, with one non-homing crystal
per direction at 45-degree intervals. These crystals use the same speed and damage
and disappear after two seconds of flight (or earlier on impact). Below half health
it also sends a delayed ground-spike wave every four seconds toward the target's
recorded position. Death of its
summoner or current player target dismisses it. Defeating it drops one pure crystal.

Craft the `imaginary_crafting_table` using `APA / IWI / ADA`: alloy ingots (A),
pure crystal (P), iron ingots (I), crafting table (W), and diamond (D). Its nine
single-item slots form an octagon around the center. Correctly placed ingredients
craft automatically in five seconds; the input slots lock during the animation,
then all ingredients are consumed and the result appears in the center. Inventory
and progress persist, and breaking the table drops its contents.

The silver-and-rose GUI includes a textured octagonal diagram, converging ingredient
animation and progress bar. Clicking the connector between the center and right
ingredient slots opens its JEI category. JEI reuses the
diagram and slot order for recipe display and ingredient transfer; transfer during
crafting shows a busy hint. The result returns to the center slot.

The alloy forge produces one `alloy_block` from one iron block and one imaginary
alloy ingot, consuming one crystal block as catalyst: **3 seconds, 1,000 FE**.
The generator's existing `lyycore:im_generator` recipe ID now uses imaginary
crafting instead of the workbench: alloy block in the center, four crystal blocks
at the cardinal points, and four imaginary batteries on the diagonals. JEI shows
this as a separate category with ingredient transfer. Existing placed generators
retain their power behavior.

Imaginary crafting JSON lists nine `ingredients`: center first, then clockwise
from the top, followed by a single-item `result`. Duration is always 100 ticks.
Use `[]` for positions that must remain empty. The imaginary disassembler now uses
this crafting type: battery in the center, crystal block above, alloy ingot below,
and all other positions empty. The energy cell also uses imaginary crafting,
with batteries in the center and four cardinal positions and alloy ingots on the
four diagonals. Both retain their recipe IDs and produce one item in five seconds.
Gate models come from `pink_stargate`; guardian, projectile, and spike meshes come
from `crystal_core_boss` under the shared Blockbench asset directory. The renderer
preserves their geometry and UVs, with orbit, summoning, spin, and spike motion in
code. No additional animation-library dependency is required.

### Crystal Condensing Frame

Craft `lyycore:crystal_condensing_frame` with eight imaginary alloy ingots around
one imaginary battery. The frame stores up to 2,147,483,647 IE, receives FE from
all sides at 100 FE per IE, and generates 1 IE every 20 loaded ticks. It produces
one crystal per tick when enough energy and output space are available. The
built-in `lyycore:crystal_condensing/imaginary_crystal` recipe produces imaginary crystals
for 200 IE dry or 100 IE waterlogged.

Use a vanilla water bucket to add water and an empty bucket to remove it; other
right clicks open the screen. Water is not consumed. Production exports crystals
to containers directly above and below the frame, regardless of placement direction;
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
JEI. Run `./gradlew.bat build --console=plain` to check compilation and packaging.

### Miniature Production Blocks

The four miniature production blocks use the existing resource selection screen,
receive IE and FE on every side, and keep their stored energy for production only.
They generate 1 IE per second, consume **100 IE per item**, store up to **1,024**
items, and export to the containers directly above and below. They have no
waterlogged state; the water in their models is decorative geometry.

- `miniature_concrete_factory`: all 16 concrete colors.
- `miniature_garden`: the 26 vanilla 1.21.1 flower-tag items, plus bee nests,
  beehives, honey bottles, honeycomb, honey blocks and honeycomb blocks (32 total).
- `miniature_ocean`: all 30 living/dead coral blocks, corals and fans, plus kelp,
  seagrass, sea pickles, sand, gravel, clay and magma blocks (37 total).
  This category contains no fish, ink, shells, hearts of the sea or tridents.
- `miniature_monument`: prismarine, prismarine bricks, dark prismarine, sea
  lanterns, prismarine shards/crystals, dry/wet sponges and gold blocks (9 total).

Craft each corresponding `concrete_core`, `garden_core`, `ocean_core` or
`monument_core` from any nine items in that category's output range, mixed freely,
in a crafting table. Then put the core in the center of the Imaginary Crafting
Table with eight imaginary alloy ingots around it; five seconds produces one block.
Each core uses the miniature scene without the outer frame as its item model.

Production recipes are separate `concrete_production`, `garden_production`,
`ocean_production` and `monument_production` types. Their JSON uses the shared
resource recipe format, defaulting to 100 IE. Core ingredient tags live under
`lyycore:gathering/<kind>_products`. Models and textures come from the extra
variants in `F:/misc/BlockBench/miniature_frames`.

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

Build validation: `./gradlew.bat build --console=plain`. In-game checks should cover
mixed core/frame crafting, selection and persistence, full inventories, energy,
export, and bucket behavior.

### Running the project

Requires Java 21.

```powershell
.\gradlew.bat clean build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

Run the four headless gameplay regression tests (mining permissions, crafting
credit, offering ownership and weather balls) with:

```powershell
.\gradlew.bat -I tools/review-regressions/init.gradle runReviewRegressions --console=plain
```

These checks use a separate world under `build/review-regressions/run`; their
classes and generated structure are excluded from normal mod builds.

The built mod is written to `build/libs`. Runtime balance settings are generated in
`config/lyycore-common.toml`.

### Miniature upgrades for condensing and gathering frames

`miniature_crystal_factory`, `miniature_tree_factory`, `miniature_overworld_factory`,
`miniature_mineral_factory` and `miniature_nether_factory` reuse their original
frame models, output recipe pools and screens. Craft each on the imaginary crafting
table with the original frame in the center and eight **alloy blocks** around it
(5 seconds). Each batch costs 100 IE and produces 4 items, wet or dry. These five
upgrades require external energy and generate no passive IE. Storage holds 1024
items; production waits when fewer than four spaces remain. Water/bucket behavior
is retained, and the Nether variant remains non-waterloggable.

All frames and miniature production machines share one output routine: export
upwards first, then downwards, and retry blocked output once per second even when
out of energy. The upgraded machines have separate JEI categories showing the
four-item batch and its cost, using the original production recipes.

## Imaginary Grapple

Right-click with `lyycore:imaginary_grapple` to fire a hook up to 20 blocks. The
held grapple disappears in first- and third-person views while its hook is out;
inventory icons remain visible. Solid blocks and immovable entities act as
anchors. Movable entities (including dropped items) are pulled toward the player
instead, with retraction when they arrive. Fully knockback-resistant or
non-pushable living entities, such as Imaginary Guardians, cannot be reeled in.
Entity detection stops at the first blocking surface.

Player traction runs locally just before vanilla player travel and starts from
actual velocity, including the previous tick's gravity, drag and collisions.
Vanilla movement applies those effects once; the server synchronizes attachment,
jump and release state instead of sending replacement player velocities each tick.
The cruise target is 0.95 blocks/tick. Motor acceleration is capped at 0.16, enough
to overcome normal gravity and drag, with distance-based braking on direct pulls.
Left/right steering and one jump are accepted for 0.2 seconds (4 ticks) after
contact. Existing upward momentum supplies the jump without a second boost.
That jump switches to an arc centered on the actual hook contact point, using
the player-center-to-hook distance at activation as the reference radius. A tangential
motor keeps the flight moving; centripetal force and a damped radius correction
maintain the arc. Radial acceleration is capped at 0.2, and tight arcs use a lower
cruise target. Radius deviations are corrected gradually rather than snapping
back to a circle, and never increase the motor's speed. Within 0.8 blocks, direct
traction remains active to avoid a degenerate swing.
Neutral input does not consume the jump opportunity, and repeated jump input
cannot stack boosts. Normal movement input stays blocked only while traction is active.
Direct traction aims the player's bounding-box center at the pull goal, using half
the current horizontal body width as the arrival radius. Crossing that radius also
counts as arrival, so a fast movement packet cannot skip the release point. Ten
consecutive ticks of collision with less than 0.01 blocks of forward progress end
a blocked direct pull; this check does not apply to swings. Arrival, obstruction,
loss of the anchor or the 40-block assisted travel limit stop traction and restore
normal movement immediately. If already grounded, the hook retracts at once;
otherwise it retains fall protection until landing, then permits another shot.
Missed shots retract at maximum range.
If a block collision shape obstructs the chain between the player and hook, the
chain breaks and all grapple forces stop without changing the current velocity.
Swinging releases immediately upon crossing the top above the hook, measured in
the initial approach direction. Turning the camera cannot release a
swing early. Direct pulls and entity reeling still break when the hook enters the
rear 45-degree cone (at least 135 degrees from horizontal facing); pitch is ignored.
The contact face itself is allowed, but routing behind the anchor block is not.
Broken chains disappear and restore movement; another shot waits until landing
unless the player is already grounded. Entity
reeling also stops on obstruction and leaves the target's momentum untouched.
The item has infinite durability and no cooldown. Death, disconnects, dimension
changes and teleports clean up the hook; grapple flights reset fall distance.

Craft it on the imaginary crafting table with a chain in the center and eight
imaginary alloy ingots around it (5 seconds); JEI displays the same recipe.
The prepared `F:/misc/BlockBench/grapple_chain` models provide the held item,
animated four-claw head and alternating chain links. The grab animation uses the
supplied contact/rebound keyframes, oriented to the hit surface.

## Imaginary Research Table

`lyycore:imaginary_research_table` provides a paginated research interface with
material and experience-level or experience-point costs, confirmation, and persistent player unlocks.
Its floating book smoothly faces the nearest player within three blocks.
Research entries are supplied by data packs. The Advanced Imaginary Gate,
Ender Sentry, Imaginary Reaper and Change the Weather are built in.
See [research definitions and behavior](docs/research.md).

Memory (default K) lists completed research and can transcribe supported entries
into owner-bound notes. The Laboratory (`lyycore:production_lab`) occupies
a 5×3 footprint and converts those notes into the research-defined product using
a single slot and circular progress display. Skill-unlocking research enables
the style HUD, style cycling (default V), and the shared special-skill key
(default mouse button 5). The laboratory is made with imaginary crafting: alloy ingot / crystal block /
alloy ingot on top, glass bottle / alloy block / glass bottle in the middle, and
three iron blocks below. The style HUD uses B/M/T/O letter glyphs in a diamond.
Skill-unlocking research and concrete skill effects remain undefined and are not prefilled.

The Advanced Imaginary Gate research costs blue ice, a mushroom stem, one water
bottle, 64 nether wart and 300 XP points. The Ender Sentry research costs a dragon
egg, 16 crystal blocks, 64 glass bottles, 64 paper and 64 iron ingots, with no XP
cost. Each transcribed report takes 100 seconds to process in the Laboratory.

The advanced gate adds a searchable biome selector and converts one full-height
column per second within an eight-block radius. Progress is saved and can be
paused. It uses vanilla biome storage granularity and does not replace terrain
blocks. Existing guardian summoning is shared with the ordinary gate; additional
summons await defined offerings and targets.

The sentry uses the vanilla dragon egg appearance. Research unlocks a personal,
invulnerable dragon that can be summoned or recalled at any sentry. It grows only
while summoned: one day per 24,000 ticks, adulthood at day five and the final
stage at day ten. It guards against monsters within five blocks, attacking once
per second for 5/10 damage. Adults can fly and hover; final-stage dragons drop
an egg every 1200 seconds spent summoned. Growth and egg progress survive recall
and death. `/lyy ender day <0-10>` changes the command user's age with operator
permission. Mining the sentry produces no drop.

The epic Imaginary Reaper keeps single-block, 5×5 and vein mining modes (right-click).
Shift + right-click opens its settings: efficiency 8–128, attack damage 2–42, and
Fortune V / Silk Touch. It is unbreakable and rejects additional enchantments.
Melee attacks chain to at most five targets in total, with three-block links and
90% damage retained per link. Research costs 64 lightning bottles, one disassembler
and four netherite ingots, followed by 100 seconds of laboratory processing.
Collect lightning bottles by right-clicking glass bottles during a thunderstorm;
collection brings a real lightning strike to the player.

Change the Weather costs blue ice, a magma block and 2000 XP points. Its report
produces a Storm Ball after 20 seconds. Storm Balls and Sun Balls convert 1:1
through shapeless crafting; throwing one sets an hour of thunderstorms or clear
weather respectively.
