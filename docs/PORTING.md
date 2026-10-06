# Modern ports

The 1.21.1 and 26.2 branches target Fabric and NeoForge independently. Main retains the 1.12.2 backport.
Gameplay and assets originate from Cadentem's cave_dweller-1.20.1-1.6.4.jar, not main's 1.12.2 Java sources.
Reference SHA-256: 2c243cad05887b40841401abdbb6256473ef4a05be65fc076d68dba34a90d8c1.
The supplied filename with `(1)` was absent; the same base filename was available without that suffix.
The JAR declares MIT licensing and Cadentem authorship. Decompiled Java was mapped using official
1.20.1 Mojang mappings and MCP SRG descriptors before adapting APIs.

## Layout and upward ports

- common/src/main/java: entity, all eight goals, timers, server spawning, configuration and navigation.
- common/src/main/resources: original JAR assets and data; included in both artifacts.
- fabric and neoforge: loader entry points and registration only.
- gradle.properties: exact Minecraft, Java, loader, API and GeckoLib versions.

Both loader projects compile the same common source tree. There is no runtime platform abstraction,
Architectury dependency, copied loader gameplay, or dependency on the backport.
To add a version, branch from the closest modern branch, update pinned versions and wrapper,
adapt Minecraft/GeckoLib changes in common, then build and smoke-test both loaders.
26.2 requires unobfuscated Loom, Java 25 and GeckoLib 5 render-state adaptation; changing
only a version string is insufficient. Do not broaden metadata compatibility without testing.

## Deliberate API adaptations

Use vanilla sound packets for private ambient sounds (position, volume and pitch retained).
Use a Cave Dweller navigation subclass instead of fragile global pathfinding mixins.
Travel freezing, water movement and damage immunity live on the entity rather than Forge event handlers.
Use vanilla step-height/water-movement attributes instead of removed Forge additions/local-variable injection.
Missing English sound subtitle translations in the reference assets are supplied.
Biome tag namespace changes from forge:is_spooky to c:is_spooky.
Original goals, behavior rolls, dimensions, sound cadence, configurable defaults and spawn checks remain.
On 26.2, model/animation resources also use GeckoLib 5 geckolib/models and geckolib/animations paths.
Animation choices are captured into render state before rendering. Full-bright eyes remain visible
during invisible encounters, and adult/baby model scales remain 1.3/0.1.
26.2 spawn eggs use the native iron-golem egg model because vanilla removed the old tinted egg template;
the entity, original textures and gameplay are unchanged. Resource/data pack formats are 88.0/107.1.

Per-dimension timers clear when a server stops; despawning runs on the server only.

Server settings use config/cave_dweller.json on both loaders; restart to apply changes.
Names/defaults/ranges match the reference; old Forge TOML files are not automatically imported.
Invalid values fail startup with the setting name. Biome tags remain datapack-configurable.

## Required verification

Run gradlew build (both loaders), python tools/verify_artifacts.py, and both dedicated-server
startup tests. In a client, verify spawn egg/summon, animations, emissive eyes, sounds,
chase/stare/flee, one-block crawling, wall climbing, door breaking and despawning.
An artifact build does not prove interactive gameplay. Record exact results in docs/VALIDATION.md.
Only push tested implementation branches. Never replace main or force-push.

## Bundled GeckoLib

The original 1.20.1/1.6.4 JAR does not embed GeckoLib: no nested JARs or GeckoLib classes;
its mods.toml requires GeckoLib4.2+. Reforked.2 deliberately improves installation.
Fabric uses Loom include; NeoForge uses ModDevGradle JarJar with a compatible version range.
GeckoLib keeps its mod ID, original package names and metadata. It is not shaded or relocated.
A compatible standalone GeckoLib for the same Minecraft/loader takes priority over the nested
fallback. Loader resolution selects one mod, preventing duplicate classes/mixins.
Incompatible Minecraft, loader or GeckoLib-major versions remain unsupported.
When porting, update both compile and embedded dependency coordinates/version ranges.

The Cave Dweller spawn egg is registered and appears in Spawn Eggs on both loaders.
Its item ID is cave_dweller:cave_dweller_spawn_egg; /give @s accepts that ID.
