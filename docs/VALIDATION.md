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

Not verified: visual appearance/head motion/eyes/audio quality, all
chase/stare/flee transitions, tight-tunnel crawling, wall climbing, door breaking and repeated
drowning/void escape. Client smoke is log-based and does not assert these gameplay outcomes.

Reproduce checks in disposable localhost-only run/smoke and run/client-smoke worlds.
Saves are separated by Minecraft version. Server script writes test config/settings and accepts Minecraft EULA for that development server;
use it only when you accept that EULA. Client smoke copies that save, mutes sound and terminates
only its own test process tree after observation. Personal game saves are never used.
Logs are local under .cache. Development offline profiles produce expected auth401/chat-key
warnings; Windows performance-counter warnings are unrelated to the mod.

The 1.21.1 branch records its own Java21/GeckoLib4 validation.

## Reforked.2 — bundled dependency checks

- Both 26.2 final artifacts build and pass config and nested-JAR checks.
- python tools/smoke_bundled.py fabric and neoforge launch actual production loaders,
  using only the built mod (plus Fabric API on Fabric). Both pass without standalone GeckoLib
  and with standalone GeckoLib5.5.5: startup, one selected GeckoLib, registered spawn egg
  in a barrel inventory, summon60HP and clean shutdown. Standalone mod takes priority.
- Original JAR inspection confirms GeckoLib was a required separate dependency, not embedded.
- These tests verify packaging and item registration. They do not claim manual egg-click,
  creative-menu or visual testing; the previous gameplay/visual limits remain.

Production harness downloads pinned official installers and uses disposable localhost-only
.cache/production saves. It accepts Minecraft EULA for those test servers only.

## Natural-spawn audit

- Compared server-tick registration, player eligibility, timers and candidate
  checks with the supplied 1.20.1/1.6.4 reference JAR. Natural spawning is
  present; no missing gameplay logic required porting.
- `python tools/smoke_natural.py fabric` and `neoforge`: packaged production
  servers with bundled GeckoLib and real AFK clients reject Creative,
  Spectator and surface Survival at Y=60. A stationary Survival player in a
  covered cave at Y=-60 then triggers a natural spawn with 60 HP.
- The test accelerates only spawn timing/chance and suppresses ambient noise;
  the default player, lighting, dimension, 16-block separation and path checks
  remain enabled. Each negative phase lasts eight seconds, exceeding the
  accelerated five-second timer; the timer remains paused until eligibility.
  No summon or spawn egg is used. Both loaders on this branch pass.
- Default 5-10 minute/20-minute timing and the 0.5% per-tick roll were audited
  against the original; the live checks use accelerated timing rather than a
  full default-duration soak. Arbitrary cave geometries and every AI transition
  remain outside this test.

Reproduce after building both artifacts and preparing the production loaders
with `tools/smoke_bundled.py LOADER --prepare-only`. Set JAVA_HOME to a Gradle
JDK and pass `--java PATH` to smoke_natural.py for the production server
(Java21 on 1.21.1, Java25 on 26.2). The default artifact is the versioned JAR
in LOADER/build/libs; `--artifact PATH` can select a distributed JAR. Logs are
under .cache/production/VERSION/LOADER/natural.log and natural-client.log.
The harness uses only its disposable production world and development client
directory, accepts the test server EULA through the preparation step, and
terminates only the client process tree it started. Personal saves are not used.
