package fade.cave_dweller.entities;

import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigateClimber;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class CaveDwellerNavigation extends PathNavigateClimber {
    private final CaveDwellerEntity caveDweller;
    private BlockPos trackedTargetPosition;

    public CaveDwellerNavigation(CaveDwellerEntity entityLivingIn, World worldIn) {
        super(entityLivingIn, worldIn);
        this.caveDweller = entityLivingIn;
    }

    @Override
    protected boolean canNavigate() {
        boolean canNavigate = super.canNavigate();
        if (!canNavigate && (!this.caveDweller.hasSpawned || this.caveDweller.isCrawling())) {
            return true;
        }
        return canNavigate;
    }

    @Override
    public Path getPathToPos(BlockPos pos) {
        this.trackedTargetPosition = pos;
        return super.getPathToPos(pos);
    }

    @Override
    public Path getPathToEntityLiving(Entity entityIn) {
        this.trackedTargetPosition = new BlockPos(entityIn);
        return super.getPathToEntityLiving(entityIn);
    }

    @Override
    public boolean tryMoveToEntityLiving(Entity entityIn, double speedIn) {
        Path path = this.getPathToEntityLiving(entityIn);
        if (path != null) {
            return this.setPath(path, speedIn);
        }

        this.trackedTargetPosition = new BlockPos(entityIn);
        this.speed = speedIn;
        return true;
    }

    public void setTrackedTargetPosition(BlockPos pos, double speedIn) {
        this.trackedTargetPosition = pos;
        this.speed = speedIn;
    }

    public void clearTrackedTargetPosition() {
        this.trackedTargetPosition = null;
    }

    public Path findPathToPos(BlockPos pos, CaveDwellerEntity.PathSearchMode mode) {
        return this.withPathSearchMode(mode, () -> super.getPathToPos(pos));
    }

    public Path findPathToEntityLiving(Entity entityIn, CaveDwellerEntity.PathSearchMode mode) {
        return this.withPathSearchMode(mode, () -> super.getPathToEntityLiving(entityIn));
    }

    @Override
    public void onUpdateNavigation() {
        if (this.caveDweller.isClimbing() && this.trackedTargetPosition != null) {
            this.updateTrackedTargetNavigation();
            return;
        }

        if (!this.noPath()) {
            super.onUpdateNavigation();
            return;
        }

        this.updateTrackedTargetNavigation();
    }

    private void updateTrackedTargetNavigation() {
        if (this.trackedTargetPosition == null) {
            return;
        }

        double minDistanceSq = (double) (this.entity.width * this.entity.width);
        BlockPos sameHeightTarget = new BlockPos(
            this.trackedTargetPosition.getX(),
            MathHelper.floor(this.entity.posY),
            this.trackedTargetPosition.getZ()
        );
        boolean farFromTargetCenter = this.entity.getDistanceSqToCenter(this.trackedTargetPosition) >= minDistanceSq;
        boolean needsVerticalMove =
            this.entity.posY <= (double) this.trackedTargetPosition.getY() || this.entity.getDistanceSqToCenter(sameHeightTarget) >= minDistanceSq;
        if ((!farFromTargetCenter || !needsVerticalMove) && !this.caveDweller.shouldKeepClimbingUpward()) {
            this.trackedTargetPosition = null;
            return;
        }

        double moveX = this.trackedTargetPosition.getX() + 0.5D;
        double moveZ = this.trackedTargetPosition.getZ() + 0.5D;
        double moveY = this.trackedTargetPosition.getY();

        // While climbing, keep the movement centered to prevent left/right drift.
        if (this.caveDweller.isClimbing()) {
            BlockPos ledgePos = this.caveDweller.getClimbLedgePos();
            if (ledgePos != null) {
                moveX = ledgePos.getX() + 0.5D;
                moveZ = ledgePos.getZ() + 0.5D;
                moveY = ledgePos.getY();
            } else {
                moveX = MathHelper.floor(this.entity.posX) + 0.5D;
                moveZ = MathHelper.floor(this.entity.posZ) + 0.5D;
            }

            if (this.caveDweller.shouldKeepClimbingUpward()) {
                moveY = ledgePos != null ? Math.max(this.entity.posY, ledgePos.getY()) : this.entity.posY + 1.0D;
            } else if (ledgePos == null) {
                moveY = this.entity.posY;
            }
        }

        this.entity.getMoveHelper().setMoveTo(moveX, moveY, moveZ, this.speed);
    }

    private Path withPathSearchMode(CaveDwellerEntity.PathSearchMode mode, Supplier<Path> supplier) {
        boolean wasCrouching = this.caveDweller.isCrouching();
        boolean wasCrawling = this.caveDweller.isCrawling();
        this.caveDweller.applyPathSearchMode(mode);
        try {
            return supplier.get();
        } finally {
            this.caveDweller.restorePathSearchPose(wasCrouching, wasCrawling);
        }
    }
}
