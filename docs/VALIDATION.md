# Validation — Minecraft 1.21.1

Tested on Windows, Java21 toolchain (Gradle hosted on Java25), Fabric Loader0.19.5,
FabricAPI0.116.17+1.21.1, NeoForge21.1.255 and GeckoLib4.9.3.

- gradlew clean build, then final gradlew build: both loaders compile/package;
  3 config tests per loader, zero failures.
- python tools/verify_artifacts.py: both final artifacts contain entity/config classes,
  exact Minecraft metadata and all sounds/models/textures; every subtitle key has an English
  translation; no fade/backport classes.
- python tools/smoke_server.py fabric and neoforge: real dedicated servers start, summon60HP,
  reject suffocation/fall damage, take10generic damage to50HP, despawn after configured10seconds
  and stop cleanly. Explicit chunk loading and a tagged test entity isolate repeat runs.
- python tools/smoke_client.py fabric and neoforge: real clients join disposable singleplayer
  worlds and run40seconds without a render/server-tick crash. Fabric's test dweller targeted
  and killed the test player; this is a basic attack check.
- Independent review compared remapped reference JAR, all8goals, navigation, damage behavior,
  water conversion and asset bytes. Surface-biome defects and Fabric dependency bounds fixed.
  Git tracks configuration sources/tests. Missing reference subtitles found by the client
  were fixed and verified in both packaged artifacts.

Not verified: visual appearance/head motion/eyes/audio quality, natural timer spawning, all
chase/stare/flee transitions, tight-tunnel crawling, wall climbing, door breaking and repeated
drowning/void escape. Client smoke is log-based and does not assert these gameplay outcomes.

Reproduce checks in disposable localhost-only run/smoke and run/client-smoke worlds, separated
by Minecraft version. Server script writes test config/settings and accepts Minecraft EULA for
that development server; use it only when you accept that EULA. Client smoke copies that save,
mutes sound and terminates only its own test process tree after observation. Personal saves are
never used. Logs are local under .cache; development auth/performance-counter warnings are
unrelated to the mod. The 26.2 branch records its own Java25/GeckoLib5 validation.
