package fade.cave_dweller.util;

import fade.cave_dweller.CaveDweller;
import fade.cave_dweller.config.ClientConfig;
import fade.cave_dweller.config.ServerConfig;
import fade.cave_dweller.entities.CaveDwellerEntity;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public final class Utils {
    private Utils() {
    }

    public static int ticksToSeconds(int ticks) {
        return ticks / 20;
    }

    public static int secondsToTicks(int seconds) {
        return seconds * 20;
    }

    public static int minutesToTicks(int minutes) {
        return secondsToTicks(minutes * 60);
    }

    public static String getTextureAppend() {
        return ClientConfig.useUpdatedTextures ? "" : "";
    }

    public static boolean isValidPlayer(@Nullable Entity entity) {
        if (!(entity instanceof EntityPlayer)) {
            return false;
        }

        EntityPlayer player = (EntityPlayer) entity;
        if (!player.isEntityAlive()) {
            return false;
        }

        if (!ServerConfig.targetInvisible && player.isInvisible()) {
            return false;
        }

        return !player.capabilities.isCreativeMode && !player.isSpectator();
    }

    @Nullable
    public static EntityPlayerMP getValidTarget(CaveDwellerEntity caveDweller) {
        World world = caveDweller.world;
        EntityPlayerMP best = null;
        double closest = Double.MAX_VALUE;
        for (EntityPlayer player : world.playerEntities) {
            if (!(player instanceof EntityPlayerMP) || !isValidPlayer(player)) {
                continue;
            }
            double distance = caveDweller.getDistanceSq(player);
            if (distance <= 128.0D * 128.0D && distance < closest) {
                closest = distance;
                best = (EntityPlayerMP) player;
            }
        }
        return best;
    }

    public static boolean isOnSurface(@Nullable Entity entity) {
        if (entity == null || entity.world == null) {
            return false;
        }

        BlockPos pos = entity.getPosition();
        World world = entity.world;
        if (world.canSeeSky(pos)) {
            return true;
        }

        int baseSkyLight = world.getLightFor(EnumSkyBlock.SKY, pos) - world.getSkylightSubtracted();
        return baseSkyLight > 0;
    }

    public static Optional<CaveDwellerEntity> trySpawnMob(
        EntityLivingBase currentVictim,
        WorldServer world,
        BlockPos blockPosition,
        int attempts,
        int xzOffset,
        int yOffset
    ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(blockPosition);
        for (int i = 0; i < attempts; i++) {
            int xOffset = MathHelper.getInt(world.rand, -xzOffset, xzOffset);
            int zOffset = MathHelper.getInt(world.rand, -xzOffset, xzOffset);
            mutable.setPos(blockPosition.getX() + xOffset, blockPosition.getY() + yOffset, blockPosition.getZ() + zOffset);

            if (!world.getWorldBorder().contains(mutable)) {
                continue;
            }

            if (!moveToPossibleSpawnPosition(world, yOffset, mutable)) {
                continue;
            }

            CaveDwellerEntity entity = new CaveDwellerEntity(world);
            entity.setLocationAndAngles(mutable.getX() + 0.5D, mutable.getY(), mutable.getZ() + 0.5D, world.rand.nextFloat() * 360.0F, 0.0F);
            if (!entity.getCanSpawnHere() || !entity.isNotColliding()) {
                continue;
            }

            boolean isValidSpawn = world.getClosestPlayerToEntity(entity, ServerConfig.spawnDistance) == null;
            if (isValidSpawn && ServerConfig.checkPathToSpawn) {
                Path path = entity.buildPathToTarget(currentVictim);
                isValidSpawn = path != null;
            }

            if (isValidSpawn) {
                entity.getNavigator().clearPath();
                world.spawnEntity(entity);
                return Optional.of(entity);
            }

            entity.setDead();
        }

        CaveDweller.LOG.debug("Cave Dweller failed spawn checks, target [{}]", currentVictim.getName());
        return Optional.empty();
    }

    private static boolean moveToPossibleSpawnPosition(WorldServer world, int attempts, BlockPos.MutableBlockPos mutable) {
        IBlockState stateAbove = world.getBlockState(mutable.up());
        for (int i = attempts; i >= -attempts; i--) {
            mutable.move(EnumFacing.DOWN);
            BlockPos up = mutable.up();
            IBlockState belowState = world.getBlockState(mutable);
            if (isOpenSpace(world, up, stateAbove) && hasSolidSupport(world, mutable, belowState)) {
                mutable.move(EnumFacing.UP);
                return true;
            }
            stateAbove = world.getBlockState(up);
        }
        return false;
    }

    private static boolean isOpenSpace(World world, BlockPos pos, IBlockState state) {
        AxisAlignedBB collision = state.getCollisionBoundingBox(world, pos);
        return state.getMaterial().isReplaceable() || collision == Block.NULL_AABB;
    }

    private static boolean hasSolidSupport(World world, BlockPos pos, IBlockState state) {
        AxisAlignedBB collision = state.getCollisionBoundingBox(world, pos);
        return state.isSideSolid(world, pos, EnumFacing.UP) || (state.getMaterial().blocksMovement() && collision != Block.NULL_AABB);
    }

    public static String getDimensionKey(World world) {
        int dim = world.provider.getDimension();
        if (dim == 0) {
            return "minecraft:overworld";
        }
        if (dim == -1) {
            return "minecraft:the_nether";
        }
        if (dim == 1) {
            return "minecraft:the_end";
        }
        return "dim:" + dim;
    }
}
