# Validation — Minecraft 1.21.1

Tested on Windows, Java21 toolchain (Gradle hosted on Java25), Fabric Loader0.19.5, FabricAPI0.116.17+1.21.1, NeoForge21.1.255 and GeckoLib4.9.3.

- gradlew build: both loaders compile/package; 3 config tests per loader, zero failures.
- python tools/verify_artifacts.py: both final artifacts contain entity classes, metadata and all referenced sounds/models/textures; no fade/backport classes.
- python tools/smoke_server.py fabric and neoforge: real dedicated servers start, summon Cave Dweller with60HP, reject suffocation/fall damage, take10generic damage to50HP, despawn after configured10seconds and stop cleanly.
- Independent review compared remapped reference JAR, all8goals, navigation, damage behavior, water attribute conversion and asset bytes. Two surface-biome defects fixed and build rerun.

Not verified: interactive client rendering, head animation, emissive eyes/audio, player-driven chase/stare/flee, natural timer spawning, tight-tunnel crawling, wall climbing, door breaking and repeated drowning/void escape. These are testing gaps, not claims of working gameplay.

Reproduce server checks in disposable localhost-only run/smoke worlds. The script writes test config/server settings and accepts Minecraft EULA for that development server; use it only when you accept that EULA. It never modifies personal game saves.
