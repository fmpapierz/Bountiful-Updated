# Bountiful — Minecraft 26.2 multiloader

Bountiful adds bounty boards to the world: take a bounty, meet its objectives, hand it in for the
reward. This repository is an unofficial update of [ejektaflex/Bountiful](https://github.com/ejektaflex/Bountiful)
to **Minecraft 26.2**, built for **Fabric, Quilt, Forge and NeoForge** from one shared source tree.

## Downloads

`./gradlew build` writes all four installable jars to `build/libs/`:

| Loader   | Jar                             | Needs                                            |
| -------- | ------------------------------- | ------------------------------------------------ |
| Fabric   | `Bountiful-fabric-<ver>.jar`    | Fabric API, Fabric Language Kotlin               |
| Quilt    | `Bountiful-quilt-<ver>.jar`     | Fabric API, Fabric Language Kotlin               |
| Forge    | `Bountiful-forge-<ver>.jar`     | nothing extra                                    |
| NeoForge | `Bountiful-neoforge-<ver>.jar`  | nothing extra                                    |

Mod Menu is optional on Fabric and Quilt; it only supplies the button that opens the settings
screen. Forge and NeoForge get the same screen from their own mod list.

## What changed from upstream

Upstream targets Minecraft 26.1.2 on Fabric and NeoForge, and depends on three of the author's own
libraries. This port had to move off all of them.

**Kambrik and Percale are gone as dependencies.** Bountiful is written in Kotlin on top of
[Kambrik](https://github.com/ejektaflex/Kambrik) (GUI DSL, networking, registration, config files)
and [Percale](https://github.com/ejektaflex/Percale) (a `DynamicOps` ↔ `kotlinx.serialization`
bridge). Neither ships for 26.2. Rather than rewrite the mod around them, both libraries are
**vendored into this repository** under `common/src/main/java/io/ejekta/kambrik`,
`.../kambrikx` and `.../percale`, taken from the 26.1-targeting branches and carried forward to
26.2. Bountiful therefore needs no library mod at all — the classes ship inside its own jar. Both
libraries are MPL-2.0; that notice travels with the source.

**Kotlin runtime.** Kotlin for Forge has no 26.2 build, so the Forge and NeoForge jars load through
plain `javafml` and bundle `kotlin-stdlib` and `kotlinx-serialization` themselves (this is why
those jars are larger). Fabric and Quilt use Fabric Language Kotlin as usual.

**Cloth Config is gone.** The settings screen was a Cloth Config screen, and Cloth publishes no
Forge build for 26.2. It is now `common/.../config/BountifulConfigScreen.kt`, drawn with vanilla
widgets, so every loader shows the same screen and the mod carries no config-library dependency.
One setting did not survive the move: `general.dataPathsToExclude`, a list of data paths for pack
authors, is now edited in `config/bountiful/config.json` rather than in the GUI.

**Wandering trader decrees are data-driven.** Minecraft 26.2 moved villager and wandering-trader
trades into the `villager_trade` datapack registry, so a mod can no longer build a `MerchantOffer`
in code — upstream had left this as an open TODO after 26.1. The trade is now four JSON entries in
`data/bountiful/villager_trade/wandering_trader/`, added to the `wandering_trader/uncommon` tag,
and the part that has to be decided when the trade is handed out — which decrees the item holds —
is a small item modifier, `bountiful:wandering_decree` (`DecreeTradeFunction.kt`). The emerald
prices follow the mod's original 2, 3, 5, 9 ladder. The one behavioural difference: upstream rolled
the decree count per trade, weighted heavily toward one decree, whereas the four fixed trades are
now picked between evenly.

**26.2 API updates**, the notable ones: the advancement `criterion` package split into
`advancements.triggers` and `advancements.predicates`; the colour value behind a formatting code
moved from `ChatFormatting` to `TextColor`; the current screen and the toast manager moved onto
`Gui`; `BlockPos.center` became `Vec3.atCenterOf`; entity NBT is written through a `ValueOutput`;
and `MultiBufferSource`, `ColorArgument` and `ItemProperties` are gone.

## Building

Minecraft 26.2 needs **Java 25** — both to run Gradle and to run the game. Point `JAVA_HOME` at a
JDK 25 and run:

```
./gradlew build
```

Per-loader tasks are `:fabric:build`, `:quilt:build`, `:forge:build`, `:neoforge:build`, and each
loader has `runClient` / `runServer` for a dev instance.

## Layout

```
common/    shared code — the whole mod, plus the vendored Kambrik and Percale sources
fabric/    Fabric entrypoints and manifest
quilt/     Quilt manifest; compiles the Fabric sources
forge/     Forge entrypoint and manifest
neoforge/  NeoForge entrypoint and manifest
```

`common` is a source folder rather than a Gradle project: each loader module folds it in with
`srcDir`, so Loom, ForgeGradle and ModDevGradle never have to agree on a shared Minecraft artifact
and every jar is self-contained.

## Credits and licence

Bountiful is by **Ejektaflex**, with board block art by AkiShirai and translations by Zano1999 and
GodGun968. Vendored Kambrik and Percale are also by Ejektaflex, under MPL-2.0.

Bountiful's own licence is as upstream ships it: the `LICENSE` file in this repository is the
GPL-3.0 text, while upstream's build metadata declares LGPL-3.0. Both are reproduced here as
found rather than reconciled.
