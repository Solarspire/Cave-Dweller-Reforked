# GeckoLib for Minecraft 1.21.9

Fabric and NeoForge artifacts use GeckoLib 5.3-alpha-3 from
[upstream commit fac3fc79](https://github.com/bernie-g/geckolib/commit/fac3fc79c3787b4468988d76e3eb53a301266b33)
under the [MIT license](GeckoLib-LICENSE). This backport keeps the
`geckolib` mod ID and uses version `5.3-alpha-3+cdw.mc1.21.9`.

Apply `geckolib-1.21.9.patch` to that upstream commit. Use Gradle 9.5.1
and Java 21 to build `:fabric:remapJar` and `:neoforge:jar`.
The local Maven repository supplies those release JARs to Loom and JarJar.
