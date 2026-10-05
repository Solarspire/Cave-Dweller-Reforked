package de.cadentem.cave_dweller.util;

import de.cadentem.cave_dweller.CaveDweller;
import de.cadentem.cave_dweller.config.ServerConfig;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Utils {
    public static int ticksToSeconds(int ticks) {
        return ticks / 20;
    }

    public static int secondsToTicks(int seconds) {
        return seconds * 20;
    }

    public static int minutesToTicks(int minutes) {
        return Utils.secondsToTicks(minutes * 60);
    }

    public static String getTextureAppend() {
        return "";
    }

    public static boolean isValidPlayer(Entity entity) {
        if (!(entity instanceof Player)) {
            return false;
        }
        Player player = (Player)entity;
        if (!player.isAlive()) {
            return false;
        }
        if (!((Boolean)ServerConfig.TARGET_INVISIBLE.get()).booleanValue() && player.isInvisible()) {
            return false;
        }
        return !player.isCreative() && !player.isSpectator();
    }

    public static LivingEntity getValidTarget(@NotNull CaveDwellerEntity caveDweller) {
        return caveDweller.level().getNearestPlayer(caveDweller.position().x, caveDweller.position().y, caveDweller.position().z, 128.0, Utils::isValidPlayer);
    }

    public static boolean isOnSurface(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        Level level = entity.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            BlockPos blockPosition = entity.blockPosition();
            if (serverLevel.canSeeSky(blockPosition)) {
                return true;
            }
            Holder biome = serverLevel.getBiome(blockPosition);
            if (biome.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "is_cave"))) || biome.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "is_underground")))) {
                return false;
            }
            int baseSkyLightLevel = serverLevel.getBrightness(LightLayer.SKY, blockPosition) - serverLevel.getSkyDarken();
            if (baseSkyLightLevel > 0) {
                return true;
            }
        }
        return false;
    }

    public static <T extends Mob> Optional<T> trySpawnMob(@NotNull Entity currentVictim, EntityType<T> entityType, MobSpawnType spawnType, ServerLevel level, BlockPos blockPosition, int attempts, int xzOffset, int yOffset, SpawnUtil.Strategy strategy) {
        BlockPos.MutableBlockPos mutableBlockPosition = blockPosition.mutable();
        for (int i = 0; i < attempts; ++i) {
            T entity;
            int xOffset = Mth.randomBetweenInclusive((RandomSource)level.random, (int)(-xzOffset), (int)xzOffset);
            int zOffset = Mth.randomBetweenInclusive((RandomSource)level.random, (int)(-xzOffset), (int)xzOffset);
            mutableBlockPosition.setWithOffset((Vec3i)blockPosition, xOffset, yOffset, zOffset);
            if (!level.getWorldBorder().isWithinBounds((BlockPos)mutableBlockPosition) || !findSpawnFloor(level, yOffset, mutableBlockPosition, strategy) || !((entity = entityType.create(level, null, (BlockPos)mutableBlockPosition, spawnType, false, false)) instanceof CaveDwellerEntity)) continue;
            if (entity.checkSpawnRules((LevelAccessor)level, spawnType) && entity.checkSpawnObstruction((LevelReader)level)) {
                boolean isValidSpawn;
                boolean bl = isValidSpawn = entity.level().getNearestPlayer((Entity)entity, (double)((Integer)ServerConfig.SPAWN_DISTANCE.get()).intValue()) == null;
                if (isValidSpawn && ((Boolean)ServerConfig.CHECK_PATH_TO_SPAWN.get()).booleanValue()) {
                    Path path = entity.getNavigation().createPath(currentVictim, 0);
                    boolean bl2 = isValidSpawn = path != null && path.canReach();
                }
                if (isValidSpawn) {
                    entity.getNavigation().createPath(entity.blockPosition(), 0);
                    entity.getNavigation().stop();
                    level.addFreshEntityWithPassengers((Entity)entity);
                    return Optional.of(entity);
                }
            }
            entity.discard();
        }
        CaveDweller.LOG.debug("Cave Dweller could not pass the spawn checks, target: [{}]", (Object)currentVictim);
        return Optional.empty();
    }

    private static boolean findSpawnFloor(ServerLevel level, int range, BlockPos.MutableBlockPos position, SpawnUtil.Strategy strategy) {
        BlockPos.MutableBlockPos below = position.mutable();
        net.minecraft.world.level.block.state.BlockState aboveState = level.getBlockState(position);
        for (int offset = -range; offset <= range; offset++) {
            below.setWithOffset(position, net.minecraft.core.Direction.DOWN);
            var belowState = level.getBlockState(below);
            if (strategy.canSpawnOn(level, below, belowState, position, aboveState)) return true;
            position.move(net.minecraft.core.Direction.DOWN);
            aboveState = belowState;
        }
        return false;
    }
}
