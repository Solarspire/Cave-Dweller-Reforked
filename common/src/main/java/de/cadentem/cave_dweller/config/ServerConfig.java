package de.cadentem.cave_dweller.config;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
public final class ServerConfig {
    public static final ConfigValue<Integer> CAN_SPAWN_MIN = new ConfigValue<>("can_spawn_min", 300, 0, 86400);
    public static final ConfigValue<Integer> CAN_SPAWN_MAX = new ConfigValue<>("can_spawn_max", 600, 0, 86400);
    public static final ConfigValue<Double> CAN_SPAWN_COOLDOWN_CHANCE = new ConfigValue<>("can_spawn_cooldown_chance", 0.4, 0.0, 1.0);
    public static final ConfigValue<Integer> CAN_SPAWN_COOLDOWN = new ConfigValue<>("can_spawn_cooldown", 1200, 0, 86400);
    public static final ConfigValue<Integer> RESET_NOISE_MIN = new ConfigValue<>("reset_noise_min", 240, 0, 86400);
    public static final ConfigValue<Integer> RESET_NOISE_MAX = new ConfigValue<>("reset_noise_max", 360, 0, 86400);
    public static final ConfigValue<Integer> TIME_UNTIL_LEAVE = new ConfigValue<>("time_until_leave", 300, 1, 6000);
    public static final ConfigValue<Integer> TIME_UNTIL_LEAVE_CHASE = new ConfigValue<>("time_until_leave_chase", 30, 1, 600);
    public static final ConfigValue<Double> SURFACE_TIMER_MULTIPLIER = new ConfigValue<>("surface_timer_multiplier", 1.0, 0.0, 5.0);
    public static final ConfigValue<Double> SPAWN_CHANCE_PER_TICK = new ConfigValue<>("spawn_chance_per_tick", 0.005, 0.0, 1.0);
    public static final ConfigValue<Integer> SPAWN_HEIGHT = new ConfigValue<>("spawn_height", 40);
    public static final ConfigValue<Boolean> ALLOW_SURFACE_SPAWN = new ConfigValue<>("allow_surface_spawn", false);
    public static final ConfigValue<Integer> SKY_LIGHT_LEVEL = new ConfigValue<>("sky_light_level", 8, 0, 15);
    public static final ConfigValue<Integer> BLOCK_LIGHT_LEVEL = new ConfigValue<>("block_light_level", 15, 0, 15);
    public static final ConfigValue<Integer> MAXIMUM_AMOUNT = new ConfigValue<>("maximum_amount", 5, 0, 100);
    public static final ConfigValue<Integer> SPAWN_DISTANCE = new ConfigValue<>("spawn_distance", 16, 0, 64);
    public static final ConfigValue<Boolean> CHECK_PATH_TO_SPAWN = new ConfigValue<>("check_path_to_spawn", true);
    public static final ConfigValue<List<String>> DIMENSION_WHITELIST = new ConfigValue<>("dimension_whitelist", List.of("minecraft:overworld"));
    public static final ConfigValue<Boolean> OVERRIDE_BIOME_DATAPACK_CONFIG = new ConfigValue<>("override_biome_datapack_config", false);
    public static final ConfigValue<Boolean> SURFACE_BIOMES_IS_WHITELIST = new ConfigValue<>("surface_biomes_is_whitelist", true);
    public static final ConfigValue<List<String>> SURFACE_BIOMES = new ConfigValue<>("surface_biomes", List.of());
    public static final ConfigValue<Integer> SPOTTING_RANGE = new ConfigValue<>("spotting_range", 60, 0, 128);
    public static final ConfigValue<Boolean> CAN_CLIMB = new ConfigValue<>("can_climb", true);
    public static final ConfigValue<Boolean> CAN_BREAK_DOOR = new ConfigValue<>("can_break_door", true);
    public static final ConfigValue<Integer> BREAK_DOOR_TIME = new ConfigValue<>("break_door_time", 3, 1, 60);
    public static final ConfigValue<Boolean> ALLOW_RIDING = new ConfigValue<>("allow_riding", false);
    public static final ConfigValue<Boolean> TARGET_INVISIBLE = new ConfigValue<>("target_invisible", true);
    public static final ConfigValue<Double> MAX_HEALTH = new ConfigValue<>("maximum_health", 60.0, 1.0, 100000.0);
    public static final ConfigValue<Double> ATTACK_DAMAGE = new ConfigValue<>("attack_damage", 6.0, 0.0, 1000.0);
    public static final ConfigValue<Double> ATTACK_SPEED = new ConfigValue<>("attack_speed", 0.35, 0.0, 10.0);
    public static final ConfigValue<Double> MOVEMENT_SPEED = new ConfigValue<>("movement_speed", 0.5, 0.0, 5.0);
    public static final ConfigValue<Double> DEPTH_STRIDER_BONUS = new ConfigValue<>("depth_strider_bonus", 1.5, 0.0, 3.0);
    public static boolean isValidDimension(String key) { return DIMENSION_WHITELIST.get().contains(key); }
    public static boolean isInValidBiome(Entity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) return false;
        var biome = level.getBiome(entity.blockPosition());
        boolean listed = OVERRIDE_BIOME_DATAPACK_CONFIG.get()
                ? biome.unwrapKey().map(key -> SURFACE_BIOMES.get().contains(key.identifier().toString())).orElse(false)
                : biome.is(TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("cave_dweller", "cave_dweller_surface_biomes")));
        return SURFACE_BIOMES_IS_WHITELIST.get() == listed;
    }
    public static void load(java.nio.file.Path configDirectory) {
        ConfigValue.load(configDirectory.resolve("cave_dweller.json"));
    }
}
