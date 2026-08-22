# Atlas Icon Reference Material

`vanilla_book.png` and `vanilla_compass_frames/compass_00.png` through
`compass_31.png` are unmodified copies of the real vanilla
`minecraft:item/book` and `minecraft:item/compass_XX` textures, extracted
from this project's own cached client jar
(`~/.gradle/caches/fabric-loom/26.2/minecraft-client.jar`) — kept here as
plain reference material, per Spec 001's Algorithm Step 7. Nothing in this
folder is AI-generated; these are Mojang's own original assets.

## Hand-painting the Atlas icon

The Atlas's in-game icon is a **book with a smaller, animated compass
overlaid on top**, built from two texture layers per frame (Minecraft's
`item/generated` model's native `layer0`/`layer1` support):

- **`layer0` (the book)** — references vanilla's own `minecraft:item/book`
  texture directly. No art file to paint for this layer at all, unless a
  custom book design is wanted later (in which case a
  `textures/item/atlas_book.png` would need to be added and every leaf
  model's `layer0` repointed at it).
- **`layer1` (the compass overlay)** — 32 separate files, one per
  animation frame, at `src/main/resources/assets/xaero-world-map-book/textures/item/atlas_overlay_00.png`
  through `atlas_overlay_31.png`. **Currently populated with the real
  vanilla compass frames** (unmodified art, just resized/repositioned — see
  below), as a working placeholder until hand-painted art replaces them.
  Overwrite these 32 files with hand-painted art whenever ready; nothing
  else needs to change as long as the filenames/16×16 canvas stay the same.

### Canvas spec for each `atlas_overlay_XX.png`

- **16×16 pixels, PNG, with a real alpha channel.**
- The compass artwork should occupy **less than the full 16×16 canvas** —
  Minecraft draws each layer at its texture's own pixel size anchored to a
  fixed corner, it does not auto-center a smaller image. To make the
  compass read as "smaller and centered" (or off-center, if preferred),
  leave transparent padding around the drawn compass within the same
  16×16 frame, rather than shrinking the file's actual dimensions.
- Every pixel not part of the compass graphic must be fully transparent
  (alpha 0), not white or black — otherwise it renders as an opaque square
  behind/around the compass.

### The 32 frames

Each `atlas_overlay_XX.png` corresponds to a compass needle angle, the
same 32-step rotation vanilla's own compass uses (`vanilla_compass_frames/`
is the direct reference for what needle angle belongs in each frame index —
`compass_00.png` through `compass_31.png` map 1:1 to `atlas_overlay_00.png`
through `atlas_overlay_31.png`). The game automatically selects the correct
frame every render tick based on the held item's orientation relative to
world spawn — no code changes are needed once the art exists at these
filenames; the model wiring (`items/atlas.json` and the 32
`models/item/atlas_XX.json` leaf models) already routes to them.

If a coarser rotation (fewer than 32 frames) is ever wanted instead, the
threshold table in `items/atlas.json` would need to change to match —
ask before assuming that's a simple drop-in change.

### Current placeholder values (locked in)

The 32 `atlas_overlay_XX.png` files currently in the mod are vanilla's own
compass frames, resized so the **actual visible compass disc — not the raw
resize target — spans 56% of the 16×16 canvas**, anchored to the **top-left
corner** and nudged **1px right** from that corner.

**Why 56%, specifically:** it's the measured minimum scale at which the
compass's red needle survives nearest-neighbor downscaling in *every one*
of the 32 rotation frames. This was verified programmatically, not eyeballed
— shrinking further (even by amounts that sound small, like to 45%) drops
the 1px-wide needle entirely in several frames, because nearest-neighbor
resizing of something that thin is highly sensitive to exact pixel
alignment. The relationship isn't a clean monotonic falloff either: some
sizes between 56% and 100% also lose the needle in a handful of frames
while 56% itself does not, so 56% is a measured floor, not a rounded
guess.

Also worth knowing if this value is ever revisited: a naive "resize the
raw 16×16 compass texture to X% of the canvas" does **not** produce a
visible compass of that size, because vanilla's own compass texture already
has a few pixels of transparent padding baked in around the drawn disc
(its real content only fills about 14×12 of its own 16×16 canvas). The 56%
figure here was computed from the disc's actual measured content size, not
the raw texture's nominal resize target — an earlier pass through this
same tuning process mislabeled a genuinely-smaller result as "63%" for
exactly this reason before the math was corrected.

Change the scale/anchor/offset by hand-editing the padding directly in an
image editor, then overwriting the 32 files in place — no code changes
needed as long as the 16×16 canvas and filenames stay the same.
