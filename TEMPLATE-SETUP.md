# Using This Repo as a GitHub Template

This repo is configured (or should be, once someone with repo-admin access flips the switch in GitHub's own Settings → "Template repository" checkbox — that flag lives in GitHub's repo settings, not in any file, so no amount of file changes here can turn it on by itself) to back a **"Use this template"** button. This document is the checklist for turning a fresh copy created that way into a real, working mod repo.

None of these steps are optional — skipping the rename steps leaves a mod that still identifies itself as `template-mod` to Fabric Loader, which will collide with an actual install of this template mod if a player ever has both installed, and will make every future spec's file paths wrong.

---

## 1. Initialize the submodule

`ai-specs/standards` is a git submodule. GitHub's "Use this template" flow copies the submodule's *commit pointer*, not its contents, so the new repo's `ai-specs/standards` directory is empty until you run:

```
git submodule update --init --recursive
```

## 2. Pick your mod's identity

Decide, up front, on:

- **Mod ID** (`snake_case`-if-multiple-words, all lowercase) — e.g. `magic_forge`
- **Display name** — e.g. `Magic Forge`
- **Base package** — always `com.onthehill.<modid-with-no-underscores>`, e.g. `com.onthehill.magicforge`

Per `fabric-mod-standards.md`'s Resource & Data Safety section, `fabric.mod.json`'s `id` must never change again once the mod has shipped — get this right now, not after the first release.

## 3. Rename the mod ID

Replace every occurrence of `template-mod` with your new mod ID in:

- `src/main/resources/fabric.mod.json` (`id` field)
- `src/main/resources/template-mod.mixins.json` → rename the file itself and update its `package` field
- `src/client/resources/template-mod.client.mixins.json` → rename the file itself and update its `package` field, and `fabric.mod.json`'s `mixins` entry pointing at it
- `gradle.properties` (`maven_group`)
- `loom { mods { "template-mod" { ... } } }` block in `build.gradle`
- `TemplateMod.MOD_ID` constant
- `src/main/resources/assets/template-mod/` → rename the directory itself (lang file, icon, any future textures/models)
- Every `gui.template-mod.*` lang key in `en_us.json`, and every `Text.translatable(...)` call referencing one

## 4. Rename the package and classes

Move every class out of `com.onthehill.templatemod` (and `com.onthehill.templatemod.client`) into your new base package, and rename the placeholder classes to match your mod's name:

- `TemplateMod` → `<ModId>`
- `TemplateModClient` → `<ModId>Client`
- `TemplateModDataGenerator` → `<ModId>DataGenerator`
- `ExampleMixin` / `ExampleClientMixin` → replace with your own first mixin, or delete if you don't need one yet

Update every `entrypoints` value in `fabric.mod.json` to match the new fully-qualified class names.

## 5. Decide what to keep from the example feature

The tick-progress example (`ai-specs/specs/000-tick-progress-example.md` and everything under `progress/`, `config/`, `network/`, `client/screen/`, `client/render/`, `ModCommands`, `ClientModCommands`) exists to demonstrate the studio's Fabric config/networking/GUI standards end-to-end, not because your mod needs a tick counter. Once you've read it as a reference:

- Delete the example's own classes if your mod's first real feature doesn't need them.
- Keep the *pattern* — the config/network/GUI/command layering it demonstrates — for your own first feature.
- Author `ai-specs/specs/001-<your-feature>.md` from the same structure Spec 000 uses (not `ai-specs/standards/template.md` alone — that template is shared with Unity packages and is Unity-flavored; Spec 000 is this repo's own worked Fabric/Java translation of it).

## 6. Reset the spec pipeline for your mod

- Clear `ai-specs/index.md` back to just Spec 000 (or remove Spec 000 too, if you're not keeping the example feature) and add a row once you author `001-*`.
- Reset `ai-specs/HANDOFF.md` to a single fresh row noting the repo was bootstrapped from the template, dated today.
- Update `ai-specs/REPO-STRUCTURE.md`'s layout diagram once your package/class renames are in.

## 7. Update the metadata files

- `README.md` — replace this template's own description with your mod's.
- `CLAUDE.md` — update the "Project Identity" section (repo name, mod ID, base package, purpose) — the rest of the file (pointers into `ai-specs/standards/rules/`) stays as-is.
- `LICENSE` — the studio default is MIT for every Fabric mod; update the copyright line's year/name if it should differ from the template's.
- `build.gradle` / `gradle.properties` — reset `mod_version` to `0.1.0` (or your own starting point) for a new mod's first release.

## 8. Verify before shipping

- [ ] `git submodule update --init --recursive` has been run
- [ ] No file still contains the string `template-mod`, `templatemod`, or `TemplateMod` (search the whole repo)
- [ ] The project builds (`./gradlew build`)
- [ ] The client launches and the mod's own screens/commands (if kept) render correctly
- [ ] `ai-specs/index.md` and `ai-specs/HANDOFF.md` reflect the new repo, not the template's own history
