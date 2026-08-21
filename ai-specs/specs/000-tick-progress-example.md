# Spec 000 — Example Tick Progress Value (Server-Authoritative Rate, Client-Selectable Visualization)

**Status:** `Implemented`
**Spec Author:** Claude (Cowork, claude-sonnet-5)
**Date Authored:** 2026-08-13
**Implementing AI:** Claude (Cowork, claude-sonnet-5)
**Depends On:** None

---

> **Why Spec 000, not Spec 001:** every mod bootstrapped from this template starts its own spec numbering at `001`. This spec is the template repository's own — a fully-implemented, worked example showing how a spec for this Fabric/Java pipeline should be structured, since `ai-specs/standards/template.md` is shared with Unity packages and is written in Unity/C# terms (namespaces, `SystemBase`, `.roo/rules-code/`). Read this spec as that translation, not as a spec queued for implementation.

---

## Context

> What already exists that this spec builds on. Reference exact file paths relative to the repo root. The implementing AI should read these files before writing any code.

- **Reads from:**
  - `ai-specs/standards/rules/fabric-mod-standards.md` — canonical project layout, entrypoints, networking type/receiver split, commands
  - `ai-specs/standards/rules/minecraft-config-standards.md` — config GUI/command/JSON parity, client-vs-server field classification, consolidated GUI tabs, reset controls, save/close dirty-tracking
  - `ai-specs/standards/rules/minecraft-gui-standards.md` — `Screen`/`ClickableWidget` construction, tooltips, narration
  - `ai-specs/standards/rules/java-coding-standards.md` — naming, Javadoc, formatting, unit test conventions
  - `src/main/java/com/onthehill/templatemod/TemplateMod.java` — existing common entrypoint (extended, not replaced)
  - `src/client/java/com/onthehill/templatemod/client/TemplateModClient.java` — existing client entrypoint (extended, not replaced)
  - `src/main/resources/fabric.mod.json` — existing manifest
- **Writes to:** every file listed under `## Implementation Requirements` below
- **Existing stubs:** `TemplateMod.onInitialize()` and `TemplateModClient.onInitializeClient()` were empty placeholders before this spec; both now also wire up this example feature. `ExampleMixin`/`ExampleClientMixin` are unrelated placeholders and are untouched by this spec.

---

## Objective

Give this template a small, complete, end-to-end example feature that exercises every layer the studio's Fabric standards care about — server-authoritative state, config classification, networking type/receiver separation, op-gated commands under the correct root, and a consolidated config GUI with tabs, reset controls, and dirty-tracked save/close — so a new mod bootstrapped from this template has one concrete, working reference to pattern-match its own first real feature against.

The feature itself: a single example progress value, `[0, 1)`, that advances every server tick by a per-tick rate. The rate is server-authoritative and OP-configurable (always less than 1, so at least one tick is visible before it wraps). When the value reaches 1 it resets to 0. Clients render the current value as either a filling/resetting bar or a filling/resetting radial "pie" wedge, per each player's own client-side preference.

---

## Data Contract

### Inputs

| Component | Fields Used | Access |
|-----------|-------------|--------|
| `ServerProgressConfig` | `progressRate` | Read (tick advancement), read/write (admin GUI + `/template-mod-admin config`) |
| `ClientVisualizationConfig` | `visualizationMode` | Read (HUD renderer), read/write (client GUI tab + `/template-mod config`) |
| `ClientNetworkHandler` (synced state) | `lastSyncedProgress`, `lastSyncedRatePerTick`, `ticksSinceLastSync` | Read (HUD renderer, via `ProgressMath.extrapolate`) |

### Outputs

| Component | Fields Modified | Notes |
|-----------|----------------|-------|
| `TemplateMod` (static tick state) | `currentProgress` | Advanced once per server tick via `TickProgressState.advance`; wraps at 1.0 |
| `ServerProgressConfig` | `progressRate`, `allowNonOpReadOnlyView` | Written by admin GUI save, `/template-mod-admin config` commands, and JSON load-fallback — all three routed through the same `ServerProgressConfig.validate(float)` |
| `ClientVisualizationConfig` | `visualizationMode` | Written by client GUI tab and `/template-mod config visualization` — client-authoritative, no server validation involved |

### New Types Required

- `com.onthehill.templatemod.progress.TickProgressState` — pure static tick-advance function
- `com.onthehill.templatemod.progress.ProgressMath` — pure static client-side extrapolation function
- `com.onthehill.templatemod.config.ServerProgressConfig` — server-authoritative config object + JSON load/save + shared validation
- `com.onthehill.templatemod.client.config.ProgressVisualizationMode` — enum `{BAR, RADIAL}`
- `com.onthehill.templatemod.client.config.ClientVisualizationConfig` — client-authoritative config object + JSON load/save
- `com.onthehill.templatemod.network.ProgressSyncPayload` (S2C), `AdminConfigSyncPayload` (S2C), `AdminConfigUpdatePayload` (C2S), `OpenAdminScreenPayload` (S2C) — `CustomPayload` records
- `com.onthehill.templatemod.network.ModNetworking` — payload type + server receiver registration
- `com.onthehill.templatemod.ModCommands` — `/template-mod-admin` command tree
- `com.onthehill.templatemod.client.network.ClientNetworkHandler` — client receiver registration + last-synced state
- `com.onthehill.templatemod.client.ClientModCommands` — `/template-mod` command tree
- `com.onthehill.templatemod.client.render.ProgressHudRenderer` — HUD draw logic
- `com.onthehill.templatemod.client.screen.ProgressConfigScreen`, `SaveConfirmScreen` — consolidated config GUI

---

## Algorithm

### Step 1 — Server-side tick advancement

Every server tick (`ServerTickEvents.END_SERVER_TICK`), the server's single authoritative progress value advances by the configured rate and wraps at 1:

$$\text{next} = \text{current} + \text{rate}; \quad \text{if next} \geq 1 \text{ then next} \mathrel{-}= 1$$

`rate` is always constrained to `(0, 1)` (see Constants below), so `next` can never reach or exceed `2`, meaning a single conditional subtraction is sufficient — no general modulo is needed here. Implemented in `TickProgressState.advance`.

### Step 2 — Resync broadcast, not a packet every tick

Rather than sending a network payload every single server tick (rejected — see the Fabric mod standard's general rule against unnecessary per-tick work, applied here to networking rather than compute), the server broadcasts `ProgressSyncPayload(progress, rate)` to every connected client only:

- immediately on that player's join, and
- every `PROGRESS_BROADCAST_INTERVAL_TICKS` (20 ticks / 1 second) as a heartbeat correction.

### Step 3 — Client-side extrapolation between syncs

Between resyncs, the client renders a smoothly-advancing value by extrapolating from the last confirmed sync using the same rate, folded back into `[0, 1)`:

$$\text{raw} = \text{syncedProgress} + (\text{rate} \times \text{ticksElapsedSinceSync}); \quad \text{result} = \text{raw} \bmod 1$$

Implemented in `ProgressMath.extrapolate`, called every HUD render frame with `ticksElapsedSinceSync` sourced from a counter incremented on `ClientTickEvents.END_CLIENT_TICK` and reset to 0 whenever a new `ProgressSyncPayload` arrives.

### Step 4 — Admin config sync respects the mandatory read-only-view setting

On join and after any successful admin config change, the server sends each connected player their own `AdminConfigSyncPayload`, built by `ModNetworking.buildAdminSyncPayloadFor`: real values if the recipient is an operator, or if `allowNonOpReadOnlyView` is `true`; otherwise a `permitted = false` payload carrying no real values at all — per the config standard's rule that this must gate the payload's contents, not just client-side rendering.

### Constants

| Constant | Value | Unit | Rationale |
|----------|-------|------|-----------|
| `ServerProgressConfig.DEFAULT_PROGRESS_RATE` | `0.01f` | progress/tick | Completes one full cycle in 100 ticks (5 seconds) — visible and legible by default |
| `ServerProgressConfig.MIN_PROGRESS_RATE` | `0.0001f` | progress/tick | Strictly positive floor so the value never appears to stall |
| `ServerProgressConfig.MAX_PROGRESS_RATE` | `0.999f` | progress/tick | Stays below 1 per the feature's own "rate is less than 1" requirement, guaranteeing at least one visible tick before reset |
| `ServerProgressConfig.DEFAULT_ALLOW_NON_OP_READ_ONLY_VIEW` | `true` | boolean | Mandatory default per `minecraft-config-standards.md` |
| `TemplateMod.PROGRESS_BROADCAST_INTERVAL_TICKS` | `20` | ticks | 1 real-time second at 20 TPS — frequent enough to correct client drift quickly, far less frequent than a per-tick packet |

---

## Implementation Requirements

Files the implementing AI must create or modify. All paths relative to the repo root.

### Create

- `src/main/java/com/onthehill/templatemod/progress/TickProgressState.java`
- `src/main/java/com/onthehill/templatemod/progress/ProgressMath.java`
- `src/main/java/com/onthehill/templatemod/config/ServerProgressConfig.java`
- `src/main/java/com/onthehill/templatemod/network/ProgressSyncPayload.java`
- `src/main/java/com/onthehill/templatemod/network/AdminConfigSyncPayload.java`
- `src/main/java/com/onthehill/templatemod/network/AdminConfigUpdatePayload.java`
- `src/main/java/com/onthehill/templatemod/network/OpenAdminScreenPayload.java`
- `src/main/java/com/onthehill/templatemod/network/ModNetworking.java`
- `src/main/java/com/onthehill/templatemod/ModCommands.java`
- `src/client/java/com/onthehill/templatemod/client/config/ProgressVisualizationMode.java`
- `src/client/java/com/onthehill/templatemod/client/config/ClientVisualizationConfig.java`
- `src/client/java/com/onthehill/templatemod/client/network/ClientNetworkHandler.java`
- `src/client/java/com/onthehill/templatemod/client/render/ProgressHudRenderer.java`
- `src/client/java/com/onthehill/templatemod/client/screen/ProgressConfigScreen.java`
- `src/client/java/com/onthehill/templatemod/client/screen/SaveConfirmScreen.java`
- `src/client/java/com/onthehill/templatemod/client/TemplateModMenuIntegration.java` — Mod Menu soft-dependency integration per `minecraft-config-standards.md`
- `src/client/java/com/onthehill/templatemod/client/ClientModCommands.java`
- `src/main/resources/assets/template-mod/lang/en_us.json`
- `src/test/java/com/onthehill/templatemod/progress/TickProgressStateTest.java`
- `src/test/java/com/onthehill/templatemod/progress/ProgressMathTest.java`

### Modify

- `src/main/java/com/onthehill/templatemod/TemplateMod.java` — add type registration, command registration, `SERVER_STARTED`/`END_SERVER_TICK`/`JOIN` wiring for the example feature, alongside the existing placeholder log line
- `src/client/java/com/onthehill/templatemod/client/TemplateModClient.java` — add client config load, receiver registration, client command registration, tick counter, and `HudRenderCallback` registration
- `build.gradle` — add JUnit 5 `testImplementation`s and `test { useJUnitPlatform() }`
- `gradle.properties` — add `junit_version`
- `LICENSE`, `src/main/resources/fabric.mod.json` (`license` field) — CC0 → MIT, per `fabric-mod-standards.md`'s "License: MIT, always" rule (a pre-existing drift from the standard, corrected incidentally while this spec's author was in these files — noted here rather than silently bundled, per the spec workflow's own transparency expectation)
- `src/main/resources/fabric.mod.json` — added the `modmenu` entrypoint and a `suggests` entry, per `minecraft-config-standards.md`'s Mod Menu soft-dependency section
- `build.gradle`, `gradle.properties` — added `clientModCompileOnly` Mod Menu dependency (originally, incorrectly, plain `modCompileOnly` — see Issues Encountered), the `TerraformersMC` maven repository (also originally missing), and `modmenu_version`

---

## Test Requirements

Per `java-coding-standards.md`: for every unit of pure, Minecraft-independent logic, write exactly **1 happy path**, **2 boundary/limit**, and **1 negative/toxicity** test, named `methodName_stateUnderTest_expectedBehavior`. `TickProgressState` and `ProgressMath` are the only classes in this spec with no Minecraft object dependency — every other new class touches `net.minecraft.*`/Fabric API types directly and is not unit-testable without a running game instance, per that same file's guidance to test the extracted plain logic rather than attempting to stand up a fake `World`/`MinecraftServer`.

### `TickProgressState`

#### Happy Path
- **`advance_midRangeProgress_addsRateWithoutWrapping`** — normal mid-cycle tick advancement

#### Boundary / Limit Tests
- **`advance_zeroProgress_addsRateFromZero`** — advancing from the zero bound
- **`advance_reachesExactlyOne_wrapsToZero`** — advancing to exactly the wrap boundary

#### Negative / Toxicity Test
- **`advance_overshootsPastOne_wrapsWithRemainder`** — a rate near the maximum bound pushes progress past 1 with a nonzero remainder, proving the wrap preserves the remainder rather than clamping to 0

### `ProgressMath`

#### Happy Path
- **`extrapolate_severalTicksElapsed_addsAccumulatedRate`** — normal multi-tick extrapolation

#### Boundary / Limit Tests
- **`extrapolate_zeroTicksElapsed_returnsSyncedProgressUnchanged`** — zero elapsed ticks
- **`extrapolate_manyTicksElapsed_wrapsAroundMultipleTimes`** — enough elapsed ticks to wrap more than once

#### Negative / Toxicity Test
- **`extrapolate_negativeTicksElapsed_throwsIllegalArgument`** — invalid negative tick count must throw rather than silently produce a nonsensical value

---

## Acceptance Criteria

The spec is complete when all of the following are true:

- [x] The example progress value advances every server tick by the configured rate and wraps at 1
- [x] The rate is classified server-authoritative, is OP-gated, and is reachable from all three required surfaces (admin GUI tab, `/template-mod-admin config progress-rate`, JSON config file)
- [x] The visualization choice is classified client-authoritative and is reachable from all three required surfaces (client GUI tab, `/template-mod config visualization`, JSON config file)
- [x] Both a bar and a radial visualization are implemented, selectable via client config
- [x] The consolidated config screen has separate Client/Admin tabs, each with per-field and section-wide reset controls
- [x] The screen tracks a dirty flag and intercepts Close with a three-action confirm prompt when dirty
- [x] The mandatory non-op read-only-view setting exists, defaults to `true`, and gates the sync payload's contents (not just rendering)
- [x] Client and server commands live under disjoint roots (`/template-mod` vs `/template-mod-admin`), and every `-admin` command is op-gated
- [x] Payload type registration is unconditional; server receiver registration is deferred to `SERVER_STARTED`
- [x] Mod Menu is a soft/compile-time-only dependency, the mod loads and works with it absent, and `getModConfigScreenFactory()` returns the same construction path the client `gui` command uses — confirmed via a real `./gradlew build` (see the 2026-08-15 rows in `ai-specs/HANDOFF.md`); loads with it as a plain `clientCompileOnly` dependency under this project's no-remap Loom mode
- [x] All 4 required tests pass for both `TickProgressState` and `ProgressMath` (8 total)
- [x] No XML/Javadoc violations on public members in the new classes
- [x] No `snake_case` identifiers (outside the deliberate asset-path/JSON-key exception)

---

## Post-Implementation Notes

> **This section is filled in by the implementing AI after the work is done.**

**Date Implemented:** 2026-08-13
**Implementing AI:** Claude (Cowork, claude-sonnet-5)

### What Was Built

The full config/network/command/GUI stack described above: pure server-tick and client-extrapolation math (unit tested), a server-authoritative config object with centralized validation and JSON persistence, a client-authoritative config object, four networking payloads with unconditional type registration and correctly-deferred server receiver registration, disjoint client/admin command roots, a client-side network handler holding last-synced state, an HUD renderer supporting both required visualizations, a consolidated two-tab config screen with reset controls and a three-action confirm-on-close prompt, and a Mod Menu soft-dependency integration reusing that same screen's construction path. Also corrected the repo's license (CC0 → MIT) and `fabric.mod.json`'s `license` field to match the studio's Fabric mod standard, which the template had drifted from before this spec.

### Deviations from Spec

- The admin GUI's rate control uses fixed-increment `+`/`-` buttons rather than a text field with free-form numeric entry, to avoid needing a numeric `TextFieldWidget` validation path this spec didn't otherwise require. A real mod's admin tab should likely use a proper numeric text field with inline validation feedback instead — flagged below as follow-up work, not silently done to spec-standard.
- `ProgressConfigScreen.save()` treats an admin update as applied to its dirty-tracking baseline optimistically, at send time, rather than waiting for the server's `AdminConfigSyncPayload` confirmation to round-trip back. This was a deliberate simplification to keep the dirty-flag logic in this example legible; a production mod with a slow/unreliable connection should instead keep the field dirty until the confirming payload actually arrives, and surface a distinct "saving..." state in between.

### Issues Encountered

- **This sandbox has no Minecraft/Fabric Loom toolchain** — nothing in this repo was compiled, and no `javap` verification against real mapped jars (as `fabric-mod-standards.md` and `minecraft-gui-standards.md` both require before trusting an API signature) was possible. Every class was written for API consistency with the pre-existing `TemplateMod.java` (which uses `net.minecraft.resources.Identifier` / `Identifier.fromNamespaceAndPath`, not the more commonly-documented `net.minecraft.util.Identifier` / `Identifier.of` — this repo's pinned Minecraft version's actual mappings were followed by matching that existing file, not assumed) and with the exact API usages shown in the standards docs' own code samples, but **none of it has been built or run**. **RESOLVED 2026-08-15** — a later session's sandbox did have a real JDK/Gradle toolchain; see `ai-specs/HANDOFF.md`'s 2026-08-15 rows for the full story. Status of each item below:
  - A full `./gradlew build` pass — **done, passes** (`BUILD SUCCESSFUL`, jar produced)
  - Visual, on-screen confirmation of `ProgressConfigScreen`'s `Layout`-tree composition — **still outstanding**, no launchable game client available in any sandbox session so far
  - Visual confirmation of `ProgressHudRenderer`'s radial wedge rendering — **still outstanding**, same reason
  - Confirmation of the real Minecraft/Fabric API class names — **done**, and it turned out the guesses were substantially wrong (Yarn-style names instead of this version's real Mojang-named API, e.g. `net.minecraft.text.Text`→`net.minecraft.network.chat.Component`, `CustomPayload`→`CustomPacketPayload`, int op-levels→a named permission-tier system) — every file was rewritten against `javap` output from the real cached jars, not guessed

- **A real `./gradlew build` attempt by the project owner failed** with "cannot find method modCompileOnly() for arguments [...]" on the Mod Menu dependency line, confirming the concern above was not hypothetical. Diagnosed (without being able to reproduce/verify in this sandbox — no Gradle/Loom toolchain here either) as two compounding mistakes in the same change: (1) `repositories {}` was left empty, so `com.terraformersmc:modmenu` — published only to `https://maven.terraformersmc.com/`, not Maven Central or Fabric's own maven — had nowhere to resolve from; (2) the dependency was declared as the bare `modCompileOnly`, which targets the `main` source set, when `TemplateModMenuIntegration` lives entirely in the `client` source set that `loom.splitEnvironmentSourceSets()` creates — it needed the source-set-prefixed `clientModCompileOnly` instead, matching Fabric's own split-source-set example-mod convention. **RESOLVED 2026-08-15, but the actual root cause was different than either guess above**: the real problem was `build.gradle`'s plugin id being Loom's no-remap marker plugin, which meant *no* `mod*`-prefixed configuration existed at all (not a repository or scoping problem specifically) — see `ai-specs/HANDOFF.md`. The final, working dependency line is plain `clientCompileOnly` (no `mod` prefix), since this project correctly runs in no-remap mode.

### Suggested Follow-Up Specs

- `modmenu_version` (`gradle.properties`) was pinned following this file's own existing convention (matching this project's fictional/future Minecraft+Fabric API pin) rather than a verified real release, exactly like `minecraft_version`/`fabric_api_version`/`loader_version` already were before this spec touched them — an initial oversight in this spec's first pass was holding `modmenu_version` to a stricter, self-inconsistent verification bar than those. Re-verify it against an actual Mod Menu release the same way every other pinned dependency in this file should be re-verified once this template targets a real Minecraft version.
- Replace the admin tab's fixed-increment rate control with a numeric `TextFieldWidget` plus inline min/max validation feedback, matching `minecraft-config-standards.md`'s rejection-message guidance ("name the field, state its bounds") directly in the GUI rather than only in command/network error text.
- Make `ProgressConfigScreen`'s admin-tab dirty tracking wait for `AdminConfigSyncPayload` confirmation instead of clearing optimistically at send time (see Deviations above).
- A build-and-visual-verification pass, by whoever next opens this repo with the real Fabric/Loom toolchain, covering every item listed under Issues Encountered above — this is the single most important follow-up, since nothing in this spec's GUI/HUD code has been confirmed to actually run.
