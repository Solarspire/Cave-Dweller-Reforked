# Changelog

## 1.6.4-reforked.3 - Minecraft 1.21.1

- Restore the original classic spawn-egg appearance and exact reference colors.
- Retain Spawn Eggs creative-tab registration on Fabric and NeoForge.

## 1.6.4-reforked.2 — Minecraft 1.21.1

- Bundle GeckoLib via native loader JAR-in-JAR; compatible standalone mods take priority.
- Verify built production JARs both with and without standalone GeckoLib.
- Verify the Cave Dweller spawn-egg item and document its give command.


## 1.6.4-reforked.1 — Minecraft 1.21.1

- Fabric and NeoForge ports of Cadentem's 1.20.1/1.6.4 JAR gameplay and assets.
- Shared common gameplay with separate loader registration and pinned dependencies.
- JSON configuration, scoped navigation, vanilla attributes and private sound packets.
- Dedicated-server and client smoke-test tooling; see docs/VALIDATION.md for exact evidence and gaps.
- Version branches preserve the existing main backport and provide a base for future ports.
