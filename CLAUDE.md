# CLAUDE.md

This file is Claude Code's onboarding point for this repository. It does not duplicate rules — it points to where they live and describes this project specifically.

## Project Identity

- **Repo:** `com.onthehill.xaero-world-map-book`
- **Mod ID:** `xaero-world-map-book` (`src/main/resources/fabric.mod.json`)
- **Base package:** `com.onthehill.xaeroworldmapbook`
- **Toolchain:** Fabric Loader + Fabric API via Gradle/Fabric Loom
- **Language:** Java only — see `ai-specs/standards/rules/fabric-mod-standards.md` ("Language Choice: Java, Not Kotlin")
- **Purpose:** A wrapper mod around Xaero's World Map (with optional support for Xaero's Minimap): it gates the ability to use/see those maps behind in-world items, and adjusts how their maps present in-game. Bootstrapped from OnTheHill Studios' Fabric mod template — the identifier/README de-templating pass (mod ID, base package, entrypoints) is done and logged in `ai-specs/HANDOFF.md`. The mod's real feature work is split across two specs: item-gating (`001`, queued) and map-appearance changes (`002`, queued) — see `ai-specs/index.md`.

## Architecture Notes

- `main` entrypoint: `com.onthehill.xaeroworldmapbook.XaeroWorldMapBook` (`ModInitializer`)
- `client` entrypoint: `com.onthehill.xaeroworldmapbook.client.XaeroWorldMapBookClient` (`ClientModInitializer`)
- `fabric-datagen` entrypoint: `com.onthehill.xaeroworldmapbook.client.XaeroWorldMapBookDataGenerator`
- Mixins: `xaero-world-map-book.mixins.json` (common), `xaero-world-map-book.client.mixins.json` (client) — example mixins live under `mixin/` in each source set
- No `src/server/` — this template has no server-only logic yet; only add it per the rule in `fabric-mod-standards.md` if a real server-only need arises
- `registry/`, `block/`, `item/`, `entity/`, `datagen/` (beyond the placeholder data generator) — still don't exist; add per `fabric-mod-standards.md`'s canonical layout only once a real feature needs them
- `config/`, `network/`, `progress/` (main) and `client/config/`, `client/network/`, `client/render/`, `client/screen/` — populated by the worked example feature described below. `progress/` is not one of `fabric-mod-standards.md`'s named sub-packages; it was added because the example's tick-state logic didn't fit any existing category (see that file's "every class belongs in the sub-package matching what it is" rule) — treat it as a precedent for adding a new, narrowly-named package when a real feature needs one, not as a catch-all to keep reusing.

## Worked Example: Spec 000

Unlike specs `001+`, **Spec 000 is this template's own** — it is not queued work for a new mod, it's a fully-implemented reference feature (`ai-specs/specs/000-tick-progress-example.md`) demonstrating this repo's config/networking/command/GUI standards end-to-end: a server-authoritative, OP-configurable tick counter with two client-selectable HUD visualizations. Read it as the concrete example of how a spec for *this* pipeline should be structured — `ai-specs/standards/template.md` is shared with Unity packages and is written in Unity/C# terms, so Spec 000 is this repo's own Fabric/Java translation of that structure, not a duplicate of it. New mods bootstrapped from this template should either keep and extend the example feature, or delete it and author `001-<title>.md` from Spec 000's structure — see `TEMPLATE-SETUP.md` step 5. **This project's own owner has chosen, for now, to keep the example feature in place as scaffold** rather than delete it — it is not part of the mod's real feature set (item gating / map appearance), and may be repurposed or stripped once specs `001`/`002` land.

**Spec 000's GUI code (`client/screen/ProgressConfigScreen.java`, `SaveConfirmScreen.java`) and HUD renderer (`client/render/ProgressHudRenderer.java`) have not been compiled or visually confirmed against a running client** — this sandbox has no local Minecraft/Fabric Loom toolchain to verify against. Treat that code as unverified per `minecraft-gui-standards.md`'s own rule until someone with the real toolchain builds and visually confirms it; see the spec's `## Post-Implementation Notes` for the full list of what still needs that pass.

## Where the Rules Live

This project does not maintain its own copy of coding/workflow standards. All of them live in the `ai-specs/standards` submodule (see `ai-specs/standards/ABOUT.md`) and are shared across every OnTheHill Studios project. Read `ai-specs/standards/README.md` first — it explains the full pipeline and links every rules file below.

**Read before writing any code, in this order:**

1. `ai-specs/standards/rules/excluded-paths.md` — directories no agent may touch
2. `ai-specs/standards/rules/brand-standards.md` — OTH color palette, required for any UI/visual output
3. `ai-specs/standards/rules/spec-workflow.md` — how to find, implement, and close out a spec
4. `ai-specs/standards/rules/java-coding-standards.md` — Java naming, Javadoc, formatting, performance, JUnit patterns
5. `ai-specs/standards/rules/fabric-mod-standards.md` — Gradle/Loom layout, package naming, entrypoints, registries, Mixins, datagen, networking, commands, versioning
6. `ai-specs/standards/rules/minecraft-config-standards.md` — config GUI/command/JSON parity rules for any mod with configurable settings (this template's Spec 000 is a full worked example)
7. `ai-specs/standards/rules/minecraft-gui-standards.md` — custom `Screen`/`ClickableWidget` conventions
8. `ai-specs/standards/rules/faq-standards.md` — when/how to capture a recurring point of confusion

## Spec Pipeline

This repo follows the spec-driven pipeline described in `ai-specs/standards/README.md`:

- `ai-specs/index.md` — manifest of every spec, its status, and its output files
- `ai-specs/HANDOFF.md` — this repo's agent log (read on every agent switch — protocol in `ai-specs/standards/HANDOFF.md`)
- `ai-specs/REPO-STRUCTURE.md` — submodule and repo map
- `ai-specs/specs/NNN-*.md` — individual specs; `000-tick-progress-example.md` is this template's own worked example (see above), `001+` are queued work for whatever mod this repo becomes

Before writing any code: read `ai-specs/index.md`, find the earliest spec with status `Ready`, and read it in full along with everything in its `## Context` section. If none exists yet, there is no spec-driven work queued — confirm with the project owner before adding new features outside the template scaffolding.

## Submodule Note

`ai-specs/standards` is a git submodule (`git@github.com:OnTheHill-Studios/ai-standards.git`). Do not edit files inside it directly from this repo — changes belong in the `ai-standards` repo itself.
