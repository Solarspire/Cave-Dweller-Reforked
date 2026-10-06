# Cave Dweller Reforked — Minecraft 1.21.1

Fabric and NeoForge ports based on Cadentem's **1.20.1 / 1.6.4 JAR**.
The original 1.12.2 backport remains on main. Modern branches: 1.21.1 and 26.2.

Install the matching loader; GeckoLib 4.9.3 is bundled. Fabric still requires Fabric API.
A standalone compatible GeckoLib JAR for the same Minecraft/loader takes priority; only one copy loads.
Use only the JAR for your loader. Requires Java 21.

Build both: `./gradlew build` (Windows: `.\gradlew.bat build`).
Outputs: `fabric/build/libs` and `neoforge/build/libs`; install the JAR without `-sources` or `-dev`.
Dev client: `./gradlew :fabric:runClient` or `./gradlew :neoforge:runClient`.
Dev server: corresponding `runServer` task.

Settings: config/cave_dweller.json; restart to apply. Defaults match the reference.
Summon: `/summon cave_dweller:cave_dweller`. Spawn egg: Spawn Eggs creative tab, or `/give @s cave_dweller:cave_dweller_spawn_egg`.

See docs/PORTING.md for provenance, API adaptations and future-port instructions.
See docs/VALIDATION.md for tested behavior and limitations.
