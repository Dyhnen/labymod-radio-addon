# LabyMod Addon Store submission

This directory is the prepared root of the public GitHub repository used for review. The official
workflow, Gradle wrapper, `api/`, `core/`, and all review documents are located at repository root.

## Flint values

- Name: `Dyhnunity Radio Player`
- Namespace: `dyhnunity-radio`
- Modification type: `LabyMod`
- Restart required: `No` (the addon uses no mixins or other start-time transformations)
- Background modification: `No`
- Hidden: `No`
- Workflow: `build.yml`
- Supported version string: `*`
- Short description: `Provider-independent radio player with DyhnunityFM, laut.fm, Radio Browser, I LOVE MUSIC, RadioReg, favorites, custom stations, HUD and optional LabyConnect Now Playing.`
- Icon: `store-assets/icon-flint-final.png` (optimized PNG, 320 x 320, 21,643 bytes)
- Thumbnail: `store-assets/thumbnail-neutral-minecraft.png` (hand-built neutral pixel/vector design, optimized PNG, exact 16:9 at 1600 x 900)
- License: `None (No license)`. No `LICENSE` file is included; all rights remain reserved by default.

The `*` compatibility value is intentional: production code is confined to the version-independent
official LabyMod Addon API and contains no Minecraft/NMS imports, mixins, reflection, or version
modules. Every Minecraft target listed in `gradle.properties` is included by the template build.

## Reviewer notes

- LabyConnect sharing is opt-in and disabled by default; see `PRIVACY.md`.
- There is no server plugin, own backend, analytics, session-token access, command interception,
  reflection, mixin, or LabyMod core-package access.
- JLayer `1.0.1` is declared with `addonMavenDependency`; licensing details are in
  `THIRD_PARTY_NOTICES.md` and in the release JAR.
- The project keeps the official addon-template GitHub Actions workflow and verified Gradle wrapper.
- Run `./gradlew reviewAudit`, `./gradlew test`, `./gradlew build`, and
  `./gradlew createReleaseJar` before submitting a channel.

## Manual production smoke test

Install only the `*-release.jar` in `~/.minecraft/labymod-neo/addons`, restart the production
client, and verify both German and English once:

1. Enable and completely disable the addon.
2. Open the player from main menu, pause menu, settings, and the configured hotkey.
3. Play, pause, reconnect, change volume, and switch providers.
4. Search every catalog and test one MP3 station from every provider.
5. Add/edit/delete a custom station and add/remove favorites.
6. Verify notifications, HUD lines, Now Playing card, sneaking behavior, and cover fallback/retry.
7. Opt in to LabyConnect sharing with a second addon user, then disable it and confirm removal.
