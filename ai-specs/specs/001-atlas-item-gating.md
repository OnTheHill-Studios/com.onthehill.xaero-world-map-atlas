# Spec 001 — Gate Xaero's World Map / Minimap Access Behind an "Atlas" Item

**Status:** `Ready`
**Spec Author:** Claude (Cowork, claude-sonnet-5)
**Date Authored:** 2026-08-21
**Implementing AI:** TBD
**Depends On:** None (Spec 000 is read only as this repo's structural example, per `CLAUDE.md` — nothing here extends or modifies Spec 000's classes)

---

## Context

> What already exists that this spec builds on. Reference exact file paths relative to the repo root. The implementing AI should read these files before writing any code.

- **Reads from:**
  - `ai-specs/standards/rules/fabric-mod-standards.md` — canonical project layout, entrypoints, networking type/receiver split, commands, registries
  - `ai-specs/standards/rules/minecraft-config-standards.md` — config GUI/command/JSON parity, server-vs-client field classification, consolidated GUI tabs, reset controls, save/close dirty-tracking
  - `ai-specs/standards/rules/minecraft-gui-standards.md` — `Screen`/`ClickableWidget` construction, tooltips, narration
  - `ai-specs/standards/rules/java-coding-standards.md` — naming, Javadoc, formatting, unit test conventions
  - `ai-specs/specs/000-tick-progress-example.md` — this repo's structural example for a spec of this shape (config/network/command/GUI layering); **do not modify anything it produced** — it is kept as scaffold, not part of this mod's real feature set (see `CLAUDE.md`'s "Worked Example: Spec 000")
  - `src/main/java/com/onthehill/xaeroworldmapbook/XaeroWorldMapBook.java` — existing common entrypoint (extended, not replaced)
  - `src/client/java/com/onthehill/xaeroworldmapbook/client/XaeroWorldMapBookClient.java` — existing client entrypoint (extended, not replaced)
  - `src/client/java/com/onthehill/xaeroworldmapbook/client/XaeroWorldMapBookMenuIntegration.java` — existing Mod Menu soft-dependency integration (its screen factory is repointed by this spec — see Implementation Requirements)
  - `src/main/resources/fabric.mod.json` — existing manifest (gains a hard dependency on Xaero's World Map and a soft/suggested dependency on Xaero's Minimap — see External Mod Integration below)
- **Writes to:** every file listed under `## Implementation Requirements` below
- **Existing stubs:** `XaeroWorldMapBookDataGenerator.onInitializeDataGenerator(...)` is currently an empty placeholder body — this spec is the first to populate it (recipe + advancement providers). `item/`, `datagen/`, and `progression/` do not exist yet in `main`; `client/integration/` does not exist yet in `client` — all four are new per `fabric-mod-standards.md`'s "add a package only once a real feature needs it" rule (`CLAUDE.md`'s Architecture Notes cites this same rule for why `progress/` was added in Spec 000).

---

## Objective

Wrap Xaero's World Map (hard dependency) and, optionally, Xaero's Minimap (soft dependency) behind a new in-world item, the **Atlas**. A player must craft an Atlas before they can open Xaero's World Map at all; the Minimap only becomes visible once the player is holding (or, alternatively, has sufficiently earned) that same Atlas. Two vanilla-style advancements mark the two milestones (crafting the first Atlas; traveling far enough on foot to have "earned" unrestricted Minimap access), and every location/keybind requirement in between is admin-configurable server policy, following this repo's established config GUI/command/JSON parity rule.

This spec is scoped to **gating and access control only**. It does not touch Spec 002's concern (changing how Xaero's maps *look* once a player can see them).

---

## External Mod Integration (Decompiling Policy, Verified Facts, Research Required)

> **Both Xaero's World Map and Xaero's Minimap are closed-source, All Rights Reserved mods.** The project owner has done their own research on this and made a call (2026-08-21): **decompiling their jars for the sole purpose of interoperability research — learning the class/method signatures and behavior needed to write this mod's own, unrelated gating feature — is authorized.** This is not this spec's own legal conclusion and this session did not independently verify it; it's the project owner's determination as the person actually shipping this mod, and it's recorded here as the operating rule for this spec. The line the project owner drew, and the one this spec holds to throughout: **decompile to learn interface/behavior information needed to interoperate, never to copy or recreate Xaero's own implementation logic, algorithms, or assets.** Concretely: reading a decompiled render method to find *where* to inject a cancellable check is in scope; copying that method's actual drawing logic into this mod, or reimplementing Xaero's map rendering ourselves from what was learned, is not — this mod gates access to Xaero's features, it doesn't reproduce them.
>
> With that authorization, this section no longer needs a "don't touch their internals" tier — it only needs to record what's been confirmed already and what the implementer must still confirm via decompilation before writing the corresponding Mixin/bridge code (the same discipline Spec 000 used for real Minecraft API names: verify against the real jar, don't guess).

### Verified

- Both mods publish to a Maven repository at `https://chocolateminecraft.com/maven` (confirmed directly from each mod's own Modrinth "For Developers" section, 2026-08-21) — this is also the dependency this mod compiles against, so decompiling and depending on it are the same artifact, not two different levels of access.
- Dependency coordinates: `xaero.map:xaeroworldmap-fabric-<mc_version>:<mod_version>` (World Map) and `xaero.minimap:xaerominimap-fabric-<mc_version>:<mod_version>` (Minimap) — `<mc_version>` for this project is `26.2` (`gradle.properties`); the exact `<mod_version>` to pin must still be looked up the same way `modmenu_version` was (a real, current release number for `26.2`, not guessed — see `gradle.properties`'s own comment on that prior mistake).
- Xaero's World Map's own out-of-the-box default keybind to open its screen is **`M`** (confirmed from its Modrinth page) — this is *why* this spec's own gated keybind defaults to the same key: the intent is for our gate to sit in front of the same key players already expect, not add a second, competing one.
- Xaero mods also support a server-driven "fair play" restriction mode via specially-formatted system chat messages (confirmed to exist via third-party plugins built on it, e.g. `ForceXaeroFairPlay`) — this is a coarser, whole-feature on/off mechanism aimed at anti-cheat, not per-item/per-advancement gating, and the exact message format is not publicly documented. **Do not use this mechanism for this spec** — it cannot express "gated behind holding a specific item," only blunt fair-play toggles. It is noted here only so it isn't rediscovered and mistaken for a better fit later.

### Requires Verification (decompile to confirm, before writing the corresponding Mixin/bridge code)

- The real class and method that renders Xaero's Minimap HUD element every frame, to Mixin a cancellable gate into (`@Inject(at = "HEAD", cancellable = true)`, matching this repo's existing `ExampleClientMixin`/`ExampleMixin` placeholder pattern). Injecting a gate here is in scope; copying the method's own drawing logic is not.
- The real class/method that actually opens Xaero's World Map screen (e.g. `Minecraft.getInstance().setScreen(new XaeroWorldMapScreen())` or a static helper) — this spec's own keybind handler and item-use handler both call it directly once the player passes the gate, and the same method is where the single choke-point gate (Step 3) is applied so it also covers Xaero's own native keybind.
- Whether Xaero's World Map exposes a public setter for its own keybind assignment (preferable to gating the open-screen method if it exists, since it's a smaller, less invasive change) — use it if present; otherwise the choke-point gate at the open-screen method (previous bullet) is the fallback, and is sufficient on its own regardless.
- Whether Xaero's Minimap exposes a public, already-existing "hidden"/"enabled" flag on a settings singleton (preferable to a render-cancelling Mixin if it exists, since it would let Xaero's own code stay in control of layout/animation state instead of us abruptly cancelling its render call mid-frame) — use it if present; otherwise the render-cancelling Mixin (first bullet) is the fallback.

Record what was actually found for each of the four items above in this spec's Post-Implementation Notes before relying on any class/method name as fact — this is the same "verify against the real jar, don't guess" discipline Spec 000 eventually had to apply retroactively (see `ai-specs/HANDOFF.md`'s 2026-08-15 rows) after an earlier pass guessed API names that turned out wrong.

---

## Data Contract

### Inputs

| Component | Fields Used | Access |
|-----------|-------------|--------|
| `MapAccessConfig` | `keybindOpenEnabled`, `keybindItemRequirement`, `minimapAccessRequirement`, `chunksRequiredForWellTraveled` | Read (every gate check, both server- and client-side via synced copy), read/write (admin GUI + `/xaero-world-map-book-admin mapaccess` commands) |
| Player's current hand/hotbar/inventory contents | Whichever slots the active `keybindItemRequirement`/`minimapAccessRequirement` value requires checking | Read only, every relevant client tick — no networking needed, the client already knows its own inventory |
| Player's advancement progress (`craft_atlas`, `well_traveled`) | Done/not-done | Read (gate checks on both sides — Minecraft already syncs a player's own advancement progress to their client automatically; confirm the exact client-side accessor for this MC version before relying on it) |
| `ChunkVisitLedger` (per player, per dimension) | The full visited-chunk set | Read/write, server-side only, on the tick-driven travel tracker |

### Outputs

| Component | Fields Modified | Notes |
|-----------|----------------|-------|
| `ModItems.ATLAS` | — | New item, craftable via a shapeless recipe (see Algorithm Step 1) |
| Player's advancement progress | `craft_atlas` awarded | Awarded server-side either by crafting the shapeless Atlas recipe **or** by otherwise obtaining an Atlas (`/give`, dropped-item pickup, etc.) — see Algorithm Step 1 |
| Player's advancement progress | `well_traveled` awarded | Awarded server-side once `ChunkVisitLedger`'s distinct-chunk count reaches `MapAccessConfig.chunksRequiredForWellTraveled`, **and** `craft_atlas` is already awarded (see Algorithm Step 4) |
| `ChunkVisitLedger` (attached per player) | Visited-chunk set, grows monotonically | Never shrinks; persisted across logout/relog and server restarts |
| `MapAccessConfig` | Any admin-configurable field | Written by the admin GUI, `/xaero-world-map-book-admin mapaccess` commands, and JSON load-fallback — all three routed through the same `MapAccessConfig.validate(...)`, per the config standard |
| Xaero's World Map (external) | Screen open/closed | Opened by this mod, once the gate passes, via right-click on the Atlas or the gated keybind |
| Xaero's Minimap (external) | Render visibility | Gated every client tick via a public visibility setter if Xaero exposes one, otherwise a render-cancelling Mixin — see External Mod Integration |

### New Types Required

- `com.onthehill.xaeroworldmapbook.item.ModItems` — item registry; registers `ATLAS`
- `com.onthehill.xaeroworldmapbook.config.MapAccessConfig` — server-authoritative config object + JSON load/save + shared validation
- `com.onthehill.xaeroworldmapbook.config.KeybindItemRequirement` — enum `{HOTBAR, INVENTORY, ADVANCEMENT}`
- `com.onthehill.xaeroworldmapbook.config.MinimapAccessRequirement` — enum `{MAIN_HAND, MAIN_OR_OFFHAND, HOTBAR, INVENTORY, ADVANCEMENT_ONLY}`
- `com.onthehill.xaeroworldmapbook.network.MapAccessConfigSyncPayload` (S2C), `MapAccessConfigUpdatePayload` (C2S), `OpenMapAccessAdminScreenPayload` (S2C) — `CustomPayload` records, same shape as Spec 000's networking types
- `com.onthehill.xaeroworldmapbook.network.MapAccessNetworking` — payload type + server receiver registration
- `com.onthehill.xaeroworldmapbook.command.MapAccessCommands` — registers `/xaero-world-map-book-admin mapaccess ...` subcommands under the existing admin root (does not modify Spec 000's `ModCommands`)
- `com.onthehill.xaeroworldmapbook.progression.ChunkVisitLedger` — pure core: a packed-`long` chunk-key set plus a `recordVisit(long chunkKey) -> boolean isNew` method; no Minecraft types
- `com.onthehill.xaeroworldmapbook.progression.ChunkVisitTracker` — impure per-player driver: computes each online player's current `(dimension, chunkX, chunkZ)` every server tick, feeds it to that player's persisted `ChunkVisitLedger`, and triggers `well_traveled` when the threshold is crossed
- `com.onthehill.xaeroworldmapbook.progression.MapAccessEvaluator` — pure static gate-decision functions; the actual unit-tested core of this spec (see Test Requirements)
- `com.onthehill.xaeroworldmapbook.progression.WellTraveledTrigger` — custom `SimpleCriterionTrigger` (no vanilla trigger fits "visited N distinct chunks")
- `com.onthehill.xaeroworldmapbook.datagen.AtlasRecipeProvider` — generates the shapeless Atlas recipe
- `com.onthehill.xaeroworldmapbook.datagen.ModAdvancementProvider` — generates both advancements
- `com.onthehill.xaeroworldmapbook.client.config.MapAccessClientState` — per-client-tick cache of "can open world map now" / "can show minimap now," recomputed once per tick (not once per render call — see Algorithm Step 6's performance note)
- `com.onthehill.xaeroworldmapbook.client.network.MapAccessClientNetworkHandler` — client receiver registration for the two S2C payloads
- `com.onthehill.xaeroworldmapbook.client.screen.MapAccessConfigScreen` — single-section admin settings screen (no Client tab — every field in this spec is server-authoritative)
- `com.onthehill.xaeroworldmapbook.client.integration.XaeroWorldMapBridge` — the one place this mod calls into Xaero's World Map's real open-screen entrypoint, once identified (see External Mod Integration); also holds the "this open call is ours, let it through" bypass flag `XaeroWorldMapOpenGateMixin` reads
- `com.onthehill.xaeroworldmapbook.client.integration.XaeroMinimapVisibilityHook` — flips Xaero's own public minimap-visible flag every client tick per `MapAccessClientState`, if Xaero exposes one (see External Mod Integration); otherwise unused, and `XaeroMinimapVisibilityMixin` (below) applies instead
- `com.onthehill.xaeroworldmapbook.client.mixin.XaeroWorldMapOpenGateMixin` — the single choke-point Mixin: cancels Xaero's own open-screen call unless either this mod's bypass flag is set (our own right-click/keybind call) or `MapAccessClientState` says the keybind gate currently passes; this is what makes Xaero's own native `M` keybind subject to the same rules as this mod's, without needing to touch its key assignment at all
- `com.onthehill.xaeroworldmapbook.client.mixin.XaeroMinimapVisibilityMixin` — cancels the Minimap's per-frame render call when `MapAccessClientState` says the gate fails; only needed if Xaero's Minimap has no public visibility setter (see External Mod Integration)

---

## Algorithm

### Step 1 — Crafting the Atlas

Shapeless recipe, any compass (`#minecraft:compass` — vanilla only has the one compass item, but tag by convention in case that changes) + one empty map (`minecraft:map` — **not** `minecraft:filled_map`) + one book (`minecraft:book`) → 1 `xaero-world-map-book:atlas`. Generated via `AtlasRecipeProvider` (datagen), not hand-authored JSON, per this repo's existing `fabric-datagen` entrypoint.

The `craft_atlas` advancement grants on **either** of two criteria (an OR — vanilla advancement JSON expresses this as two separate top-level entries under `requirements`, each its own one-element list, rather than one list containing both): `minecraft:recipe_crafted` tied to this recipe's ID (fires specifically on crafting it, not merely unlocking the recipe), **or** `minecraft:inventory_changed` matching an Atlas stack (fires on receiving one by any other means — `/give`, loot, trading, a dropped-item pickup). Both are deliberately included so a player who is handed an Atlas administratively still gets the advancement and everything gated behind it, rather than being stuck unable to progress just because they didn't personally craft one.

### Step 2 — Opening Xaero's World Map (right-click)

Registered once, client-side only, via Fabric API's item-use callback: when the used stack `is(ModItems.ATLAS)`, call `XaeroWorldMapBridge.openWorldMap()` unconditionally — right-clicking an item you are, by definition, holding already satisfies every possible holding-location requirement this spec defines, so no further gate check is needed on this path. Returns a success interaction result; does nothing server-side (the item has no server-side behavior of its own).

`openWorldMap()` sets a short-lived "this open call is ours, let it through" bypass flag immediately before invoking Xaero's real open method, and clears it in a `finally` block right after — `XaeroWorldMapOpenGateMixin` (Step 3) reads this flag to distinguish this mod's own deliberate calls from Xaero's own native keybind still trying to call the same method.

### Step 3 — Opening Xaero's World Map (keybind)

A new client keybinding, default `M`, category matching the mod's display name. Every client tick (not every render frame — see Step 6), `MapAccessClientState` recomputes whether the keybind is currently allowed to fire:

1. If `MapAccessConfig.keybindOpenEnabled` is `false`, the keybind never fires.
2. Otherwise, per `MapAccessConfig.keybindItemRequirement`:
   - `HOTBAR` — the Atlas must currently occupy one of the player's 9 hotbar slots.
   - `INVENTORY` — the Atlas must currently occupy any of the player's 36 main-inventory slots (hotbar + storage rows; armor and offhand are separate slots and don't count toward this check specifically, since offhand is covered by the *Minimap*'s own "main or offhand" option, not this one).
   - `ADVANCEMENT` — the player must have been awarded `craft_atlas` (no item location check at all — once earned, always usable by keybind, regardless of whether an Atlas is on hand right now).
3. If the check passes, the keybind press calls `XaeroWorldMapBridge.openWorldMap()`, setting the bypass flag first (see below), exactly like the right-click path.
4. If the check **fails**, or `keybindOpenEnabled` is `false`, nothing opens — but the player gets a short action-bar message explaining why (see "Feedback on a blocked attempt" below), throttled so mashing the key doesn't spam it.

**Xaero World Map's own native `M` keybind still needs to be covered by these same rules, or a player can bypass every one of them just by pressing the key Xaero itself still listens for.** Rather than trying to unbind Xaero's key (which only stops *that* key specifically, and nothing else if Xaero ever adds another way to trigger the same open call), `XaeroWorldMapOpenGateMixin` gates the single choke point both paths funnel through — the real open-screen method itself (see External Mod Integration):

- If this mod's own bypass flag is set (Step 2's right-click path, or this step's own keybind path after its own check already passed), the call is let through unconditionally — the gate was already applied before either of those paths made the call.
- Otherwise (Xaero's own native keybind, or anything else that might call the same method), the Mixin applies the exact same `keybindOpenEnabled` + `keybindItemRequirement` check as this step's numbered list above, using `MapAccessClientState`'s cached result.

This means Xaero's own default `M` keybind keeps working exactly as it always has *as a keybind* (still visible, still bound, still rebindable in Xaero's own settings) — nothing about its assignment changes — but every attempt to actually open the map, from any source, now passes through the same gate.

**Feedback on a blocked attempt:** rather than silently doing nothing (which reads as a bug to a player who doesn't know why nothing happened), a blocked attempt — from this mod's own keybind failing its check — shows a short, translatable action-bar message (e.g. `gui.xaero-world-map-book.map_access_denied`, wording left to the implementer but should name what's missing, e.g. "you need an Atlas in your hotbar") via `Minecraft.getInstance().gui.setOverlayMessage(...)`. Throttled to at most once per `MapAccessClientState.DENIAL_MESSAGE_COOLDOWN_TICKS` (see Constants) so holding the key down doesn't spam the overlay.

### Step 4 — Tracking chunks traveled (not chunks generated)

`ChunkVisitTracker` runs on `ServerTickEvents.END_SERVER_TICK`, but only actually does its per-player work every `ChunkVisitTracker.CHECK_INTERVAL_TICKS` ticks (see Constants), not every single one:

- **Why not every tick:** a chunk is 16 blocks wide, and no legitimate player movement crosses that in under several ticks even at extreme speed (a sprinting player covers roughly 0.28 blocks/tick; even a fast elytra-firework dive rarely exceeds a few blocks/tick) — so checking less often than every tick costs essentially no detection latency (worst case, a few ticks' delay before a newly-entered chunk is recorded, imperceptible for a milestone counter) while cutting the per-tick, per-online-player iteration cost proportionally. To be precise about what this optimizes: the per-tick check itself (compute a chunk position, compare two cached longs) is already cheap in absolute terms — this throttle is a "why not, it's free" reduction in a naturally-idle loop, not a fix for an actual measured bottleneck, since nothing here does expensive work except on an actual chunk change, which is already rate-limited by movement physics regardless of how often the outer loop runs.
- On every check, for each online player, compute `(player.level().dimension(), new ChunkPos(player.blockPosition()))` and compare against an **in-memory, unpersisted** "last known chunk" cache — only on an actual chunk change is the persisted ledger touched at all.

1. Pack `(chunkX, chunkZ)` into a single `long` the same way `ChunkPos.asLong()` already does (confirm this method still exists with this exact packing under this MC version before relying on it); key the per-dimension ledger by that packed long.
2. `ChunkVisitLedger.recordVisit(packedChunkKey)` returns whether this chunk was new. If new, the player's persisted attachment data is marked dirty (see Step 5) and the running distinct-chunk count (summed across every dimension's ledger) increments by one.
3. Deliberately **not** based on `ServerLevel`'s already-generated-chunk count or render/simulation distance — those reflect what the *world* has generated (including pregeneration a server operator ran with no player ever present), not where a specific player has actually walked, flown, or ridden. Per-player ledgers sidestep this entirely: a chunk only counts once a specific player's own position has actually been inside it.
4. If the running count reaches `MapAccessConfig.chunksRequiredForWellTraveled` **and** the player already holds `craft_atlas` (per the spec's own floor rule — see Step 5), award `well_traveled` via `WellTraveledTrigger`, exactly once (advancement awarding is naturally idempotent, but the tracker should also stop doing the threshold comparison once already awarded, to avoid repeating a no-op check every tick for the remainder of the player's session).

### Step 5 — Minimap visibility (holding the Atlas, or having earned it)

The Minimap gate has a **floor requirement** and a **mode-specific requirement**, both of which must pass:

- **Floor (always required, regardless of mode):** the player must have been awarded `craft_atlas`. Per the literal ask ("a user should never have access to the Xaero Minimap if they do not also have access to Xaero World Map first"), this floor is checked unconditionally, in every mode below — `craft_atlas` is this spec's chosen proxy for "has World Map access at all," since actually opening World Map is itself always a moment-to-moment check (Steps 2–3), not a durable state to query.
- **Mode-specific, per `MapAccessConfig.minimapAccessRequirement`:**
  - `MAIN_HAND` — Atlas must be the current main-hand stack.
  - `MAIN_OR_OFFHAND` — Atlas must be the current main-hand **or** offhand stack. (Default — see Constants.)
  - `HOTBAR` — Atlas must occupy one of the 9 hotbar slots (need not be the currently-selected slot).
  - `INVENTORY` — Atlas must occupy any of the 36 main-inventory slots.
  - `ADVANCEMENT_ONLY` — no holding check at all; the player must have been awarded `well_traveled` instead. Since `well_traveled` itself can only ever be awarded after `craft_atlas` (Step 4, point 4), the floor requirement above is automatically satisfied whenever this mode's own check passes — there is no way to reach `well_traveled` without the floor, so this is a real guarantee, not just a convention.

`MapAccessEvaluator.canShowMinimap(...)` is the single pure function implementing "floor AND mode-specific check" — every one of the five modes above is exercised directly in its unit tests (see Test Requirements).

### Step 6 — Applying the Minimap gate without doing per-frame inventory scans

`MapAccessClientState` recomputes both `canUseKeybind` and `canShowMinimap` once per `ClientTickEvents.END_CLIENT_TICK`, caching the two booleans — every consumer below only ever reads this cached boolean, never recomputing the underlying inventory/hand/advancement checks per frame. This is the same "don't do unnecessary per-frame/per-tick work" discipline Spec 000 established for its own broadcast interval, applied here to render/setting-update frequency instead of network frequency.

How the cached boolean actually reaches Xaero's Minimap depends on what Requires Verification turns up (see External Mod Integration):

- **Preferred, if it exists:** `XaeroMinimapVisibilityHook` calls Xaero's own public visibility setter once per client tick with the cached value — ordinary use of a published setting, no Mixin needed for this part at all.
- **Otherwise:** `XaeroMinimapVisibilityMixin` reads the cached `canShowMinimap` boolean at the head of Xaero's per-frame render call and cancels if it's `false` — this keeps the Mixin body a single field read regardless of how expensive the underlying checks are or how many times per tick Xaero's renderer is invoked.

### Step 7 — Atlas icon: reference material only, not the final asset

Per the project owner's own request, this spec does **not** author the final `atlas.png` icon — that's being hand-made separately. What this spec's implementer must do instead:

1. Copy the real vanilla book item texture and the real vanilla compass item texture (**confirm the exact current asset path for this Minecraft version first** — the compass in particular has changed how it packages its texture/animation across Minecraft's history, and must not be assumed without checking the real client jar/asset index this project's Loom toolchain actually resolves) from the cached vanilla client jar into a new reference folder: **`docs/images/atlas-icon-reference/`** (sibling to the existing `docs/images/`, itself established by `fabric-mod-standards.md`'s "Documentation Assets & Icons" rule as the right place for anything not meant to ship inside the built jar — this reference material is exactly that: working material for a human to open in an image editor, never packaged by Loom since only `src/` is a build input).
2. Name them clearly, e.g. `docs/images/atlas-icon-reference/vanilla_book.png` and `docs/images/atlas-icon-reference/vanilla_compass.png` (or the real per-frame filename(s), if this MC version's compass turns out to still be frame-based rather than a single static texture — document whichever it actually is).
3. Until the hand-made icon exists, place a **temporary placeholder** at the real in-jar path, `src/main/resources/assets/xaero-world-map-book/textures/item/atlas.png` (a duplicate of the vanilla book texture is a reasonable stand-in — a recognizable book-shaped icon beats a missing-texture pink/black checker while this is in progress), so the mod still loads cleanly in the meantime. Swap it for the real hand-made icon once delivered; this spec's acceptance criteria do not require the final icon, only that a valid (even if temporary) texture exists.

---

## Constants

| Constant | Value | Unit | Rationale |
|----------|-------|------|-----------|
| `MapAccessConfig.DEFAULT_KEYBIND_OPEN_ENABLED` | `true` | boolean | The keybind is the more discoverable of the two open paths; on by default |
| `MapAccessConfig.DEFAULT_KEYBIND_ITEM_REQUIREMENT` | `HOTBAR` | enum | Matches the literal ask's own stated default behavior ("it should only be usable when the Atlas is in the hotbar") before the other modes were introduced as configurable alternatives |
| `MapAccessConfig.DEFAULT_MINIMAP_ACCESS_REQUIREMENT` | `MAIN_OR_OFFHAND` | enum | Matches the literal ask's own base phrasing, "actively holding the Atlas in one of their hands" — one of their hands means either hand |
| `MapAccessConfig.DEFAULT_CHUNKS_REQUIRED_FOR_WELL_TRAVELED` | `300` | chunks | Placeholder milestone-sized default (roughly a modest early-game exploration goal) — **the project owner should tune this**, it is not derived from anything authoritative |
| `MapOpenKeybind.DEFAULT_KEY` | `M` | key | Matches Xaero World Map's own native default — see External Mod Integration for why both keys ending up gated the same way makes this a non-issue rather than a conflict |
| `ChunkVisitTracker.CHECK_INTERVAL_TICKS` | `5` | ticks | See Step 4's rationale — costs a few ticks of detection latency, saves a proportional share of the per-tick, per-online-player iteration cost, for a milestone counter where neither matters |
| `MapAccessClientState.DENIAL_MESSAGE_COOLDOWN_TICKS` | `60` | ticks | 3 real-time seconds — long enough that holding/mashing the blocked keybind doesn't spam the action bar, short enough that a player who changes what they're holding gets fresh feedback quickly |

---

## Implementation Requirements

Files the implementing AI must create or modify. All paths relative to the repo root.

### Create

- `src/main/java/com/onthehill/xaeroworldmapbook/item/ModItems.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/config/MapAccessConfig.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/config/KeybindItemRequirement.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/config/MinimapAccessRequirement.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/network/MapAccessConfigSyncPayload.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/network/MapAccessConfigUpdatePayload.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/network/OpenMapAccessAdminScreenPayload.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/network/MapAccessNetworking.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/command/MapAccessCommands.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/progression/ChunkVisitLedger.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/progression/ChunkVisitTracker.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/progression/MapAccessEvaluator.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/progression/WellTraveledTrigger.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/datagen/AtlasRecipeProvider.java`
- `src/main/java/com/onthehill/xaeroworldmapbook/datagen/ModAdvancementProvider.java`
- `src/client/java/com/onthehill/xaeroworldmapbook/client/config/MapAccessClientState.java`
- `src/client/java/com/onthehill/xaeroworldmapbook/client/network/MapAccessClientNetworkHandler.java`
- `src/client/java/com/onthehill/xaeroworldmapbook/client/screen/MapAccessConfigScreen.java`
- `src/client/java/com/onthehill/xaeroworldmapbook/client/integration/XaeroWorldMapBridge.java`
- `src/client/java/com/onthehill/xaeroworldmapbook/client/integration/XaeroMinimapVisibilityHook.java` (only if Requires Verification finds a public visibility setter — otherwise skip and rely on the Mixin below instead)
- `src/client/java/com/onthehill/xaeroworldmapbook/client/mixin/XaeroWorldMapOpenGateMixin.java`
- `src/client/java/com/onthehill/xaeroworldmapbook/client/mixin/XaeroMinimapVisibilityMixin.java` (only if Requires Verification finds no public visibility setter — otherwise skip and rely on `XaeroMinimapVisibilityHook` above)
- `src/test/java/com/onthehill/xaeroworldmapbook/progression/ChunkVisitLedgerTest.java`
- `src/test/java/com/onthehill/xaeroworldmapbook/progression/MapAccessEvaluatorTest.java`
- `docs/images/atlas-icon-reference/` (reference textures — see Algorithm Step 7; not shipped in the jar)
- A temporary placeholder `src/main/resources/assets/xaero-world-map-book/textures/item/atlas.png` (see Algorithm Step 7)
- `src/main/resources/assets/xaero-world-map-book/models/item/atlas.json` (may be datagen-produced instead of hand-authored, implementer's choice)

### Modify

- `src/main/java/com/onthehill/xaeroworldmapbook/XaeroWorldMapBook.java` — register `ModItems`, load `MapAccessConfig` on `SERVER_STARTED`, register `MapAccessNetworking` types/receivers, register `MapAccessCommands`, register `WellTraveledTrigger`, hook `ChunkVisitTracker` into `END_SERVER_TICK`
- `src/client/java/com/onthehill/xaeroworldmapbook/client/XaeroWorldMapBookClient.java` — register the gated keybind, the item-use callback for right-click, `MapAccessClientNetworkHandler`'s receivers, and the `END_CLIENT_TICK` hook that refreshes `MapAccessClientState`
- `src/client/java/com/onthehill/xaeroworldmapbook/client/XaeroWorldMapBookMenuIntegration.java` — repoint its Mod Menu screen factory at `MapAccessConfigScreen` (this mod's real settings) instead of Spec 000's `ProgressConfigScreen` (the demo)
- `src/main/resources/fabric.mod.json` — add a hard `depends` entry for Xaero's World Map; add Xaero's Minimap to `suggests` (soft, matching the existing Mod Menu pattern) plus a `clientCompileOnly` dependency in `build.gradle`
- `build.gradle` — add the `https://chocolateminecraft.com/maven` repository; add `implementation`/`clientCompileOnly` dependencies for Xaero's World Map (hard) and Xaero's Minimap (soft) respectively, using the real, verified-current mod versions for `minecraft_version=26.2` (look these up the same way `modmenu_version` was corrected — see `gradle.properties`)
- `gradle.properties` — add `xaero_world_map_version`, `xaero_minimap_version`
- `src/client/resources/xaero-world-map-book.client.mixins.json` — add `XaeroWorldMapOpenGateMixin`, and `XaeroMinimapVisibilityMixin` if it ends up needed (see Create list above)
- `src/main/resources/assets/xaero-world-map-book/lang/en_us.json` — add: item name (`item.xaero-world-map-book.atlas`), advancement title/description for both advancements, `MapAccessConfigScreen`'s field labels/tooltips, keybind category + name (`key.categories.xaero-world-map-book`, `key.xaero-world-map-book.open_world_map`), the blocked-attempt action-bar message (`gui.xaero-world-map-book.map_access_denied`)
- `src/client/java/com/onthehill/xaeroworldmapbook/client/XaeroWorldMapBookDataGenerator.java` — register `AtlasRecipeProvider` and `ModAdvancementProvider`

---

## Test Requirements

Per `java-coding-standards.md`: for every unit of pure, Minecraft-independent logic, write exactly **1 happy path**, **2 boundary/limit**, and **1 negative/toxicity** test, named `methodName_stateUnderTest_expectedBehavior`. `ChunkVisitLedger` and `MapAccessEvaluator` are the only new classes in this spec with no Minecraft object dependency — every other new class touches `net.minecraft.*`/Fabric API/Xaero types directly and is not unit-testable without a running game instance, per the same guidance Spec 000 followed.

### `ChunkVisitLedger`

#### Happy Path
- **`recordVisit_neverSeenChunkKey_returnsTrueAndIncreasesCount`** — a brand-new chunk key is recorded as new, and the ledger's size grows by one

#### Boundary / Limit Tests
- **`recordVisit_alreadyVisitedChunkKey_returnsFalseAndCountUnchanged`** — revisiting the same key a second time does not double-count it
- **`recordVisit_zeroChunksVisitedYet_startsAtCountZero`** — a freshly-constructed ledger reports zero, not an uninitialized/garbage value

#### Negative / Toxicity Test
- **`recordVisit_manyDistinctKeysAcrossSimulatedDimensions_countsEachExactlyOnce`** — feeding in a large batch of keys built to collide under a naive same-dimension-only packing scheme (i.e. two different "dimensions" reusing the same raw chunk-position long) must still be counted as distinct entries, proving the ledger's keying actually disambiguates dimensions rather than silently merging them

### `MapAccessEvaluator`

#### Happy Path
- **`canUseKeybind_hotbarModeWithAtlasInHotbar_returnsTrue`** — the straightforward default-configuration case

#### Boundary / Limit Tests
- **`canUseKeybind_keybindDisabledRegardlessOfItemLocation_returnsFalse`** — `keybindOpenEnabled = false` short-circuits every other condition
- **`canUseKeybind_advancementModeWithoutAtlasButAdvancementEarned_returnsTrue`** — `ADVANCEMENT` mode does not require holding the item at all

#### Negative / Toxicity Test
- **`canUseKeybind_hotbarModeWithAtlasOnlyInMainInventoryRow_returnsFalse`** — an Atlas one row below the hotbar must not satisfy `HOTBAR` mode; proves the boundary between hotbar slots and the rest of the inventory is enforced, not approximated

#### Happy Path
- **`canShowMinimap_mainOrOffhandModeWithAtlasInOffhandAndFloorMet_returnsTrue`** — the default-configuration case, offhand specifically (main-hand is the more obvious case and is covered by the boundary test below)

#### Boundary / Limit Tests
- **`canShowMinimap_floorNotMet_returnsFalseRegardlessOfMode`** — no `craft_atlas` advancement means false in every one of the five modes, including `ADVANCEMENT_ONLY`
- **`canShowMinimap_advancementOnlyModeWithAtlasHeldButNoWellTraveled_returnsFalse`** — physically holding the Atlas does not substitute for `well_traveled` when the mode is `ADVANCEMENT_ONLY`

#### Negative / Toxicity Test
- **`canShowMinimap_mainHandModeWithAtlasInOffhandOnly_returnsFalse`** — the strictest mode must not be satisfied by the other hand; proves `MAIN_HAND` and `MAIN_OR_OFFHAND` are not accidentally implemented identically

---

## Acceptance Criteria

The spec is complete when all of the following are true:

- [ ] Crafting a compass + empty map + book at a crafting table (shapeless, any order) yields one Atlas
- [ ] Crafting the first Atlas awards the `craft_atlas` advancement; so does obtaining one by any other means (`/give`, loot, trading, pickup) — both criteria are implemented and either alone is sufficient
- [ ] Right-clicking the Atlas (in either hand) opens Xaero's World Map unconditionally
- [ ] This mod's own `M` keybind opens Xaero's World Map only when `keybindOpenEnabled` is true and the current `keybindItemRequirement` mode's check passes; a blocked attempt shows a throttled action-bar message explaining why
- [ ] All three `keybindItemRequirement` modes (`HOTBAR`, `INVENTORY`, `ADVANCEMENT`) are implemented and independently reachable via the admin GUI, `/xaero-world-map-book-admin mapaccess` commands, and JSON config
- [ ] Xaero's own native `M` keybind no longer independently opens the map — `XaeroWorldMapOpenGateMixin` gates its call to the same open-screen method this mod's own paths call, so the same `keybindOpenEnabled`/`keybindItemRequirement` rules apply regardless of which key or code path triggered it
- [ ] The Minimap gate (floor: `craft_atlas` awarded; mode-specific: the active `minimapAccessRequirement` check) is applied — via a public visibility setter if Xaero exposes one, otherwise via `XaeroMinimapVisibilityMixin` — and a player can never see the Minimap without `craft_atlas`, under any mode, including `ADVANCEMENT_ONLY`; all five `minimapAccessRequirement` modes are implemented and reachable via the admin GUI, commands, and JSON config
- [ ] Chunk-travel tracking counts only chunks a specific player's own position has occupied — verified by a test proving pregenerated-but-never-visited chunks (or chunks only ever loaded via render/simulation distance around another player) do not count
- [ ] Reaching `chunksRequiredForWellTraveled` distinct chunks, while `craft_atlas` is already held, awards `well_traveled` exactly once
- [ ] `chunksRequiredForWellTraveled` is admin-configurable via all three required surfaces
- [ ] The visited-chunk ledger persists across logout/relog and a server restart
- [ ] `MapAccessConfigScreen` has per-field and section-wide reset controls and a dirty-tracked save/close prompt, matching the pattern Spec 000 established
- [ ] Mod Menu's entry now opens `MapAccessConfigScreen` (this mod's real settings), not Spec 000's demo screen
- [ ] Xaero's World Map is declared as a hard dependency in `fabric.mod.json`; Xaero's Minimap is a soft dependency (loads and degrades gracefully if absent — the Minimap-visibility gate simply has nothing to gate)
- [ ] A placeholder Atlas icon exists so the mod loads cleanly; vanilla book and compass reference textures have been copied to `docs/images/atlas-icon-reference/` for the project owner to hand-paint the final icon from
- [ ] Every item under External Mod Integration's "Requires Verification" has been resolved against the real decompiled jars and recorded in Post-Implementation Notes before the corresponding Mixin/bridge code is considered trustworthy; nothing about what was learned (beyond the interface/behavior information needed to write these hooks) was copied into this mod's own logic or assets
- [ ] All 10 required tests pass across `ChunkVisitLedger` and `MapAccessEvaluator`
- [ ] No XML/Javadoc violations on public members in the new classes
- [ ] No `snake_case` identifiers outside the deliberate asset-path/JSON-key/advancement-ID exception

---

## Post-Implementation Notes

> **This section is filled in by the implementing AI after the work is done.** Left blank — this spec has not been implemented yet.
