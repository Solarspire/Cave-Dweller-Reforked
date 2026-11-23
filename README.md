# Cave Dweller Reforked - Minecraft 1.21.2

Fabric and NeoForge ports of Cadentem's Minecraft 1.20.1 / Cave Dweller 1.6.4.
The original 1.12.2 backport remains on `main`.

## Installation

- Requires Minecraft 1.21.2 and Java 21.
- Install the JAR for Fabric or NeoForge 21.2.1-beta.
- Fabric also requires Fabric API 0.106.1+1.21.2.
- GeckoLib 4.7.3+cdw.mc1.21.2 is bundled. A compatible standalone GeckoLib JAR
  for the same Minecraft version and loader takes priority; only one copy loads.

## Gameplay and settings

Settings are in `config/cave_dweller.json`. Restart the world/server after changes.

By default, natural spawning targets Survival or Adventure players underground
at Y <= 40. Creative and Spectator players are excluded. The timer normally takes
5-10 minutes, with a 40% chance of a 20-minute cooldown. It pauses when no eligible
players are present. The spawn location must have space and a reachable path.

The Cave Dweller spawn egg is in the Spawn Eggs creative tab. Commands:

```mcfunction
/give @s cave_dweller:cave_dweller_spawn_egg
/summon cave_dweller:cave_dweller
```

## Building

Use installed Gradle 9.5.1:

```shell
gradle build
```

JARs are written to `fabric/build/libs` and `neoforge/build/libs`.
Install the runtime JAR without a `-sources` or `-dev` suffix.

Run a development client with `gradle :fabric:runClient` or
`gradle :neoforge:runClient`. Use the corresponding `runServer` task for a server.
