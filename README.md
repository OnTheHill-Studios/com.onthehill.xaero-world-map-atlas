# Template Mod

![Template Mod logo](docs/images/logo.png)

OnTheHill Studios' starter/skeleton repo for new Fabric mods. This repo is meant to be used via GitHub's **"Use this template"** button, not cloned and worked in directly — see [`TEMPLATE-SETUP.md`](TEMPLATE-SETUP.md) for the exact steps to turn a fresh copy into a new mod.

It ships with a small, fully worked example feature (an OP-configurable, server-authoritative tick counter with two client-selectable HUD visualizations) whose spec — [`ai-specs/specs/000-tick-progress-example.md`](ai-specs/specs/000-tick-progress-example.md) — is meant to be read as a worked example of how a spec for this repo's pipeline should be written, before authoring `001-*`. See `ai-specs/index.md` for the full spec manifest.

## Setup

For IDE/toolchain setup, see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) for the IDE you're using.

This repo also depends on the `ai-specs/standards` git submodule — after cloning (or after using this repo as a template), run:

```
git submodule update --init --recursive
```

## Documentation Images

The logo above is a duplicate of `src/main/resources/assets/template-mod/icon.png`, stored separately under [`docs/images/`](docs/images/) rather than referenced from its in-jar path — per `ai-specs/standards/rules/fabric-mod-standards.md`'s "Documentation Assets & Icons" section, `src/main/resources/` is a build input Loom packs into the jar, not a stable path a rendered README can rely on. Any future screenshots, banners, or diagrams for this repo's docs/wiki belong in `docs/images/` alongside it — never under `src/`.

## Third-Party Assets

- `assets/template-mod/textures/gui/widgets/reset.png` — the config screen's reset-to-default icon is Material Design Icons' `mdi-restore` glyph, by [Pictogrammers](https://pictogrammers.com/) ([github.com/Templarian/MaterialDesign](https://github.com/Templarian/MaterialDesign)), licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0). Rasterized from the upstream SVG path data to a 64x64 RGBA PNG as part of this mod's own asset build; not a redistributed upstream binary. See `ProgressConfigResetButton`'s class javadoc for the same attribution alongside the code that uses it — this file demonstrates the pattern for pulling an external icon into a Fabric config screen, for mods bootstrapped from this template to follow.

## License

This template is available under the MIT license. Feel free to learn from it and incorporate it in your own projects.
