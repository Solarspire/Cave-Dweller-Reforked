# Cave Dweller modern ports

Port the supplied Cadentem 1.20.1 1.6.4 JAR to Minecraft 1.21.1 and 26.2, on Fabric
and NeoForge. Preserve its behavior and assets; do not transplant the 1.12.2 backport.
Use version branches named 1.21.1 and 26.2; keep main untouched. Push those branches
to the existing origin after successful verification. Do not merge main or publish releases.

Compile shared common sources directly into each loader artifact. Loader adapters register
the entity, attributes, spawn egg, sounds and lifecycle hooks. Shared code owns AI, spawning,
navigation and validated JSON settings. Retain original defaults, biome data and animations.
Use modern vanilla APIs to replace obsolete Forge hooks, networking and global mixins.

Pin dependencies per branch. Validate both builds, artifact contents, settings boundaries,
dedicated-server startup and client gameplay where feasible. Report every verification gap
explicitly. Future ports update one common implementation plus loader-specific adapters.
