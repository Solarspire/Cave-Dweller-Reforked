# Validation â€” Minecraft 26.2

Tested on Windows with Java25, Fabric Loader0.19.5, FabricAPI0.161.0+26.2,
NeoForge26.2.0.88 and GeckoLib5.5.5.

- gradlew clean build, then final gradlew build: both loaders compile/package; 3 config tests per loader, zero failures.
- python tools/verify_artifacts.py: both artifacts contain entity classes, exact Minecraft metadata,
  sounds/textures, original model/animation files and identical GeckoLib5 relocated copies.
  Every sound subtitle has an English translation. Asset bytes are protected from Git EOL conversion.
- python tools/smoke_server.py fabric and neoforge: real dedicated servers start, summon60HP,
  reject suffocation/fall damage, take10generic damage to50HP, despawn after configured10seconds
  and stop cleanly. Explicit chunk loading and a tagged test entity isolate repeat runs.
- python tools/smoke_client.py fabric and neoforge: real clients load one model and one animation,
  join disposable singleplayer worlds and run40seconds without a render/server-tick crash.
  The NeoForge test dweller targeted and killed the test player; this is a basic attack check.
- Independent review found/fixed duplicate renderer scaling, invisible-phase eye suppression
  and Fabric dependency OR bounds. Configuration sources/tests are tracked in Git. The committed port also passed a clean Git-archive build
  for both loaders. Missing reference subtitles were fixed and package checks rerun.

Not verified: visual appearance/head motion/eyes/audio quality, natural timer spawning, all
chase/stare/flee transitions, tight-tunnel crawling, wall climbing, door breaking and repeated
drowning/void escape. Client smoke is log-based and does not assert these gameplay outcomes.

Reproduce checks in disposable localhost-only run/smoke and run/client-smoke worlds.
Saves are separated by Minecraft version. Server script writes test config/settings and accepts Minecraft EULA for that development server;
use it only when you accept that EULA. Client smoke copies that save, mutes sound and terminates
only its own test process tree after observation. Personal game saves are never used.
Logs are local under .cache. Development offline profiles produce expected auth401/chat-key
warnings; Windows performance-counter warnings are unrelated to the mod.

The 1.21.1 branch records its own Java21/GeckoLib4 validation.
