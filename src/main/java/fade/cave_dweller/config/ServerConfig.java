package fade.cave_dweller.config;

import fade.cave_dweller.CaveDweller;
import java.util.Arrays;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.BiomeDictionary;

@Config(modid = CaveDweller.MODID, name = CaveDweller.MODID + "/server")
public class ServerConfig {
    @Config.Comment("Minimum time between spawns in seconds")
    @Config.RangeInt(min = 0, max = 86400)
    public static int canSpawnMin = 300;

    @Config.Comment("Maximum time between spawns in seconds")
    @Config.RangeInt(min = 0, max = 86400)
    public static int canSpawnMax = 600;

    @Config.Comment("Chance for a spawn cooldown to occur")
    @Config.RangeDouble(min = 0.0D, max = 1.0D)
    public static double canSpawnCooldownChance = 0.4D;

    @Config.Comment("Spawn cooldown length in seconds")
    @Config.RangeInt(min = 0, max = 86400)
    public static int canSpawnCooldown = 1200;

    @Config.Comment("Minimum time between noise occurrences in seconds")
    @Config.RangeInt(min = 0, max = 86400)
    public static int resetNoiseMin = 240;

    @Config.Comment("Maximum time between noise occurrences in seconds")
    @Config.RangeInt(min = 0, max = 86400)
    public static int resetNoiseMax = 360;

    @Config.Comment("Time in seconds it takes for the Cave Dweller to leave")
    @Config.RangeInt(min = 1, max = 6000)
    public static int timeUntilLeave = 300;

    @Config.Comment("Time in seconds it takes for the Cave Dweller to leave once chase starts")
    @Config.RangeInt(min = 1, max = 600)
    public static int timeUntilLeaveChase = 30;

    @Config.Comment("Multiplier for timers on the surface")
    @Config.RangeDouble(min = 0.0D, max = 5.0D)
    public static double surfaceTimerMultiplier = 1.0D;

    @Config.Comment("Spawn chance per tick once spawn timer is reached")
    @Config.RangeDouble(min = 0.0D, max = 1.0D)
    public static double spawnChancePerTick = 0.005D;

    @Config.Comment("Maximum Y level where the Cave Dweller can start spawning")
    public static int spawnHeight = 40;

    @Config.Comment("Allow Cave Dweller spawning on surface")
    public static boolean allowSurfaceSpawn = false;

    @Config.Comment("Maximum sky light level for spawn checks")
    @Config.RangeInt(min = 0, max = 15)
    public static int skyLightLevel = 8;

    @Config.Comment("Maximum block light level for spawn checks")
    @Config.RangeInt(min = 0, max = 15)
    public static int blockLightLevel = 15;

    @Config.Comment("Maximum number of Cave Dwellers alive at once")
    @Config.RangeInt(min = 0, max = 100)
    public static int maximumAmount = 5;

    @Config.Comment("Minimum distance to players for valid spawn position")
    @Config.RangeInt(min = 0, max = 64)
    public static int spawnDistance = 16;

    @Config.Comment("If true, validate path to spawn victim before spawning")
    public static boolean checkPathToSpawn = true;

    @Config.Comment("Dimension whitelist using resource ids (e.g. minecraft:overworld)")
    public static String[] dimensionWhitelist = new String[] {"minecraft:overworld", "0", "dim:0"};

    @Config.Comment("Use biome list below instead of datapack biome tags")
    public static boolean overrideBiomeDatapackConfig = true;

    @Config.Comment("Treat surface biome list as whitelist (true) or blacklist (false)")
    public static boolean surfaceBiomesIsWhitelist = true;

    @Config.Comment("Surface biome white/black list entries (modid:biome)")
    public static String[] surfaceBiomes = new String[0];

    @Config.Comment("Spotting range for line-of-sight behavior")
    @Config.RangeInt(min = 0, max = 128)
    public static int spottingRange = 60;

    @Config.Comment("Whether Cave Dweller can climb")
    public static boolean canClimb = true;

    @Config.Comment("Whether Cave Dweller can break doors")
    public static boolean canBreakDoor = true;

    @Config.Comment("Time in seconds to break a door")
    @Config.RangeInt(min = 1, max = 60)
    public static int breakDoorTime = 3;

    @Config.Comment("Allow vanilla riding behavior (boats etc.)")
    public static boolean allowRiding = false;

    @Config.Comment("Allow targeting invisible players")
    public static boolean targetInvisible = true;

    @Config.Comment("Maximum health")
    @Config.RangeDouble(min = 1.0D, max = 100000.0D)
    public static double maximumHealth = 60.0D;

    @Config.Comment("Attack damage")
    @Config.RangeDouble(min = 0.0D, max = 1000.0D)
    public static double attackDamage = 6.0D;

    @Config.Comment("Attack speed multiplier used by AI cooldowns")
    @Config.RangeDouble(min = 0.0D, max = 10.0D)
    public static double attackSpeed = 0.35D;

    @Config.Comment("Movement speed")
    @Config.RangeDouble(min = 0.0D, max = 5.0D)
    public static double movementSpeed = 0.5D;

    @Config.Comment("Depth Strider-like movement bonus in water")
    @Config.RangeDouble(min = 0.0D, max = 3.0D)
    public static double depthStriderBonus = 1.5D;

    public static boolean isValidDimension(String key) {
        if (Arrays.asList(dimensionWhitelist).contains(key)) {
            return true;
        }

        Integer dimensionId = tryExtractDimensionId(key);
        for (String configured : dimensionWhitelist) {
            if (configured == null) {
                continue;
            }

            if (configured.equalsIgnoreCase(key)) {
                return true;
            }

            Integer configuredId = tryExtractDimensionId(configured);
            if (configuredId != null && dimensionId != null && configuredId.intValue() == dimensionId.intValue()) {
                return true;
            }

            if (dimensionId != null) {
                if (dimensionId == 0 && ("minecraft:overworld".equals(configured) || "overworld".equalsIgnoreCase(configured))) {
                    return true;
                }
                if (dimensionId == -1 && ("minecraft:the_nether".equals(configured) || "the_nether".equalsIgnoreCase(configured) || "nether".equalsIgnoreCase(configured))) {
                    return true;
                }
                if (dimensionId == 1 && ("minecraft:the_end".equals(configured) || "the_end".equalsIgnoreCase(configured) || "end".equalsIgnoreCase(configured))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static Integer tryExtractDimensionId(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        if ("minecraft:overworld".equals(key) || "overworld".equalsIgnoreCase(key)) {
            return 0;
        }
        if ("minecraft:the_nether".equals(key) || "the_nether".equalsIgnoreCase(key) || "nether".equalsIgnoreCase(key)) {
            return -1;
        }
        if ("minecraft:the_end".equals(key) || "the_end".equalsIgnoreCase(key) || "end".equalsIgnoreCase(key)) {
            return 1;
        }

        if (key.startsWith("dim:")) {
            try {
                return Integer.parseInt(key.substring(4));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static boolean isInValidBiome(Entity entity) {
        if (entity == null || entity.world == null) {
            return false;
        }

        Biome biome = entity.world.getBiome(entity.getPosition());
        boolean isBiomeInList;
        if (overrideBiomeDatapackConfig) {
            ResourceLocation resource = biome.getRegistryName();
            isBiomeInList = resource != null && Arrays.asList(surfaceBiomes).contains(resource.toString());
        } else {
            // 1.18 used a biome tag populated from forge "spooky" biomes.
            isBiomeInList = BiomeDictionary.hasType(biome, BiomeDictionary.Type.SPOOKY);
        }

        if (surfaceBiomesIsWhitelist) {
            return isBiomeInList;
        }
        return !isBiomeInList;
    }
}
