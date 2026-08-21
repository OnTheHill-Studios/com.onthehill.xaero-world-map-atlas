# Spec Index

Manifest of all specs for `com.onthehill.template-mod`. See `ai-specs/standards/README.md` for the pipeline this table supports and `ai-specs/standards/rules/spec-workflow.md` for how to update it.

| # | Title | Status | Depends On | Output Files |
|---|-------|--------|------------|---------------|
| 000 | Example Tick Progress Value (Server-Authoritative Rate, Client-Selectable Visualization) | Implemented — **`./gradlew build` passes** (compiles, packages, tests all green), first time confirmed; runtime/visual behavior (config screen, both HUD modes, Mod Menu entry) still needs a manual pass in an actual launchable game client, which this sandbox cannot do — see spec's Post-Implementation Notes | None | `progress/`, `config/`, `network/`, `ModCommands.java`, `client/config/`, `client/network/`, `client/render/`, `client/screen/`, `client/ClientModCommands.java`, `client/TemplateModMenuIntegration.java`, `assets/template-mod/lang/en_us.json` |

Spec 000 is this template repository's own worked example (see `ai-specs/specs/000-tick-progress-example.md`'s header note and `CLAUDE.md`'s "Worked Example: Spec 000" section) — it is not queued work for a new mod. A mod bootstrapped from this template starts its own numbering at `001`; copy `ai-specs/standards/template.md`, using Spec 000 as the concrete structural example rather than that Unity-flavored template alone, to author it.
