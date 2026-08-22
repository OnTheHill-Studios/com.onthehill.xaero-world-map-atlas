# Xaero World Map Book

![Xaero World Map Book logo](docs/images/logo.png)

An OnTheHill Studios Fabric mod that wraps [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map), with optional support for [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap). This mod does not reimplement or replace either of Xaero's mods — it gates the ability to use/see them behind in-world items, and adjusts how their maps present in-game.

This repo was bootstrapped from OnTheHill Studios' Fabric mod template; the de-templating pass (mod ID, base package, entrypoints) is done. Real feature work is tracked as two specs: `001` gates map access/visibility behind in-world items, and `002` changes the maps' in-game appearance — see [`ai-specs/index.md`](ai-specs/index.md) for the full manifest.

The repo still carries a fully worked, non-shipping example feature inherited from the template (an OP-configurable, server-authoritative tick counter with two client-selectable HUD visualizations) — its spec, [`ai-specs/specs/000-tick-progress-example.md`](ai-specs/specs/000-tick-progress-example.md), is kept as a worked reference for how a spec for this repo's pipeline should be written, and is not part of this mod's real feature set.

## Setup

For IDE/toolchain setup, see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) for the IDE you're using.

This repo also depends on the `ai-specs/standards` git submodule — after cloning (or after using this repo as a template), run:

```
git submodule update --init --recursive
```

## Documentation Images

The logo above is a duplicate of `src/main/resources/assets/xaero-world-map-book/icon.png`, stored separately under [`docs/images/`](docs/images/) rather than referenced from its in-jar path — per `ai-specs/standards/rules/fabric-mod-standards.md`'s "Documentation Assets & Icons" section, `src/main/resources/` is a build input Loom packs into the jar, not a stable path a rendered README can rely on. Any future screenshots, banners, or diagrams for this repo's docs/wiki belong in `docs/images/` alongside it — never under `src/`.

## Third-Party Assets

- `assets/xaero-world-map-book/textures/gui/widgets/reset.png` — the config screen's reset-to-default icon is Material Design Icons' `mdi-restore` glyph, by [Pictogrammers](https://pictogrammers.com/) ([github.com/Templarian/MaterialDesign](https://github.com/Templarian/MaterialDesign)), licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0). Rasterized from the upstream SVG path data to a 64x64 RGBA PNG as part of this mod's own asset build; not a redistributed upstream binary. See `ProgressConfigResetButton`'s class javadoc for the same attribution alongside the code that uses it — this file demonstrates the pattern for pulling an external icon into a Fabric config screen, for mods bootstrapped from this template to follow.

## License

This mod is available under the MIT license.
