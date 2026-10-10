# Cave Dweller Reimagined (Forge 1.12.2 Backport)

This workspace contains a source backport scaffold of `cave_dweller-1.18.2-1.6.4.jar` for Forge `1.12.2`, aligned to a Cleanroom-style development setup.

## Environment Target

- Cleanroom Forge dev env reference: `https://github.com/CleanroomMC/ForgeDevEnv`
- Forge version: `1.12.2`
- Java toolchain: `8` for compile/run
- GeckoLib dependency: `software.bernie.geckolib:geckolib-forge-1.12.2:3.0.31`

## Run Dev Client

```powershell
.\gradlew.bat runClient
```

## Build

```powershell
.\gradlew.bat build
```

## Project Layout

- Backported code: `src/main/java/fade/cave_dweller`
- Mod assets: `src/main/resources/assets/cave_dweller`
- Decompiled reference: `decomp-java`, `decomp-assets`
