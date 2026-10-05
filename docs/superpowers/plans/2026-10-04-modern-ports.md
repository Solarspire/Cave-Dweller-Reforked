# Modern ports implementation plan

> Execute inline with superpowers:executing-plans; verify before completion.

**Goal:** Four loader/version artifacts based on the 1.20.1 reference JAR.
**Architecture:** Shared common Java/resources, two small loader projects, version branches.
**Tech stack:** Gradle, Mojang mappings, Fabric Loom, NeoForge ModDevGradle, GeckoLib.
**Spec:** ../specs/2026-10-04-modern-ports-design.md

## Global constraints
Minecraft 1.21.1 and 26.2. Fabric and NeoForge. JAR provenance. Preserve main.
Branch names 1.21.1 and 26.2. No force-push. No claim of gameplay verification from builds.

## Review focus
Dedicated servers must not load client render classes. Bad config must fail clearly.
Timers must reset between servers. Spawn candidates must pass clearance and path checks.
Both artifacts must contain original assets and exact loader/version metadata.

## Tasks
- [x] Audit reference JAR, map decompiled classes by descriptor and record provenance.
- [x] Port 1.21.1 common gameplay plus loader registration; test config and artifact contents.
- [x] Build/smoke-test 1.21.1 Fabric and NeoForge; record evidence and commit.
- [x] Branch 26.2; adapt Minecraft APIs and GeckoLib 5; test both loaders.
- [x] Review branches, record limitations and prepare verified branches for the requested push to origin.

Ruling: execute authorized work continuously; no added design/plan approval round.
Ruling: use requested version branches in the clean target checkout; no extra worktree.
