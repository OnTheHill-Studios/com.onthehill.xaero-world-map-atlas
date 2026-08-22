# Repo Structure — com.onthehill.xaero-world-map-book

Submodule and repo map for this project. Read alongside `ai-specs/standards/README.md`.

## Submodules

| Path | Repo | Purpose |
|------|------|---------|
| `ai-specs/standards` | `git@github.com:OnTheHill-Studios/ai-standards.git` | Canonical, agent-agnostic coding/workflow standards shared across all OTH projects. Read-only from this repo — edits belong upstream. |

## Top-Level Layout

```
com.onthehill.xaero-world-map-book/
├── ai-specs/
│   ├── standards/        ← submodule (ai-standards) — rules, pipeline docs, spec template
│   ├── index.md          ← spec manifest (this repo)
│   ├── HANDOFF.md         ← agent log (this repo)
│   ├── REPO-STRUCTURE.md ← this file
│   └── specs/
│       └── 000-tick-progress-example.md  ← this template's own worked example spec
├── CLAUDE.md              ← Claude Code onboarding — points into ai-specs/standards/rules
├── TEMPLATE-SETUP.md      ← checklist for bootstrapping a new mod repo from this template
├── docs/
│   └── images/
│       └── logo.png       ← duplicate of assets/xaero-world-map-book/icon.png for use outside the jar (README, wiki) — see fabric-mod-standards.md "Documentation Assets & Icons"
├── build.gradle
├── gradle.properties
├── settings.gradle
├── LICENSE                ← MIT, per fabric-mod-standards.md
├── src/
│   ├── main/
│   │   ├── java/com/onthehill/xaeroworldmapbook/
│   │   │   ├── XaeroWorldMapBook.java          ← common entrypoint (placeholder + Spec 000 wiring)
│   │   │   ├── ModCommands.java          ← /xaero-world-map-book-admin command tree (Spec 000)
│   │   │   ├── mixin/ExampleMixin.java   ← placeholder mixin
│   │   │   ├── progress/                 ← pure, unit-tested tick/extrapolation math (Spec 000)
│   │   │   ├── config/                   ← ServerProgressConfig (Spec 000)
│   │   │   └── network/                  ← CustomPayload records + ModNetworking (Spec 000)
│   │   └── resources/
│   │       ├── fabric.mod.json
│   │       ├── xaero-world-map-book.mixins.json
│   │       └── assets/xaero-world-map-book/
│   │           ├── icon.png
│   │           └── lang/en_us.json       ← GUI translation keys (Spec 000)
│   ├── client/
│   │   ├── java/com/onthehill/xaeroworldmapbook/client/
│   │   │   ├── XaeroWorldMapBookClient.java          ← client entrypoint (placeholder + Spec 000 wiring)
│   │   │   ├── XaeroWorldMapBookDataGenerator.java   ← datagen entrypoint (placeholder)
│   │   │   ├── ClientModCommands.java          ← /xaero-world-map-book command tree (Spec 000)
│   │   │   ├── mixin/ExampleClientMixin.java   ← placeholder client mixin
│   │   │   ├── config/                         ← ClientVisualizationConfig (Spec 000)
│   │   │   ├── network/                        ← ClientNetworkHandler (Spec 000)
│   │   │   ├── render/                         ← ProgressHudRenderer (Spec 000)
│   │   │   └── screen/                         ← ProgressConfigScreen, SaveConfirmScreen (Spec 000)
│   │   └── resources/xaero-world-map-book.client.mixins.json
│   └── test/java/com/onthehill/xaeroworldmapbook/progress/
│       ├── TickProgressStateTest.java
│       └── ProgressMathTest.java
└── .github/workflows/build.yml
```

No `src/server/` yet — add per `ai-specs/standards/rules/fabric-mod-standards.md` and `ai-specs/standards/rules/java-coding-standards.md` when real (non-template) server-only logic needs it. `registry/`, `block/`, `item/`, `entity/`, `datagen/` (beyond the placeholder data generator) also don't exist yet — same rule.

## Status

This repo has been de-templated (mod ID, base package, and entrypoints renamed off `template-mod`/`templatemod`) and is now `xaero-world-map-book`, a wrapper mod for Xaero's World Map and (optionally) Xaero's Minimap. It still carries the `ai-specs` pipeline scaffolding and one fully-authored example spec (`000-tick-progress-example.md`) demonstrating the full config/networking/command/GUI standard stack, kept as scaffold rather than deleted — see that spec's Post-Implementation Notes for what has and hasn't been build/visually verified. The mod's real feature work is specs `001` (item-gated map access) and `002` (map appearance), both queued — see `ai-specs/index.md`.
