package fade.cave_dweller.entities.goals;

import fade.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class CaveDwellerFleeGoal extends EntityAIBase {
    private static final int HIDE_SEARCH_ATTEMPTS = 12;
    private static final int HIDE_SEARCH_RADIUS = 32;
    private static final int HIDE_VERTICAL_RANGE = 10;
    private static final double HIDE_REACHED_DISTANCE_SQ = 4.0D;

    private final CaveDwellerEntity caveDweller;
    private final double speedModifier;
    private final float initialTicksUntilLeave;
    private float ticksUntilLeave;
    private float ticksUntilFlee;
    private boolean shouldLeave;
    private Path fleePath;
    private BlockPos fleeDestination;
    private int ticksUntilNextPathRecalculation;

    public CaveDwellerFleeGoal(CaveDwellerEntity caveDweller, float ticksUntilLeave, double speedModifier) {
        this.caveDweller = caveDweller;
        this.initialTicksUntilLeave = ticksUntilLeave;
        this.ticksUntilLeave = ticksUntilLeave;
        this.ticksUntilFlee = 10.0F;
        this.speedModifier = speedModifier;
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (this.caveDweller.isInvisible()) {
            return false;
        }
        if (this.caveDweller.currentRoll != Roll.FLEE) {
            return false;
        }
        return this.caveDweller.getAttackTarget() != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.caveDweller.currentRoll == Roll.FLEE && this.caveDweller.getAttackTarget() != null;
    }

    @Override
    public void startExecuting() {
        this.ticksUntilLeave = this.initialTicksUntilLeave;
        this.ticksUntilFlee = 10.0F;
        this.shouldLeave = false;
        this.caveDweller.setAggressive(false);
        this.caveDweller.pleaseStopMoving = false;
        this.fleeDestination = null;
        this.setFleePath();
    }

    @Override
    public void resetTask() {
        this.caveDweller.isFleeing = false;
        this.caveDweller.setFleeing(false);
        this.caveDweller.setAggressive(false);
        this.fleePath = null;
        this.fleeDestination = null;
        this.ticksUntilNextPathRecalculation = 0;
    }

    @Override
    public void updateTask() {
        EntityLivingBase target = this.caveDweller.getAttackTarget();
        if (target == null) {
            return;
        }

        if (this.shouldLeave && this.shouldDisappear(target)) {
            this.caveDweller.disappear();
            return;
        }

        this.ticksUntilFlee--;
        this.tickStareClock();
        if (this.ticksUntilFlee <= 0.0F) {
            this.fleeTick();
            this.caveDweller.isFleeing = true;
            this.caveDweller.setFleeing(true);
        } else {
            this.caveDweller.getLookHelper().setLookPositionWithEntity(target, 180.0F, 1.0F);
        }
    }

    private void setFleePath() {
        EntityLivingBase target = this.caveDweller.getAttackTarget();
        if (target == null) {
            return;
        }

        Path bestPath = null;
        BlockPos bestDestination = null;
        int bestScore = Integer.MAX_VALUE;

        for (int attempt = 0; attempt < HIDE_SEARCH_ATTEMPTS; attempt++) {
            Vec3d fleePosition = RandomPositionGenerator.findRandomTargetBlockAwayFrom(
                this.caveDweller,
                HIDE_SEARCH_RADIUS,
                HIDE_VERTICAL_RANGE,
                target.getPositionVector()
            );
            if (fleePosition == null) {
                continue;
            }

            BlockPos destination = new BlockPos(fleePosition);
            Path path = this.caveDweller.getNavigator().getPathToPos(destination);
            if (path == null || path.getCurrentPathLength() <= 0) {
                continue;
            }

            int score = this.scoreHideDestination(target, destination, path);
            if (bestPath == null || score < bestScore) {
                bestPath = path;
                bestDestination = destination;
                bestScore = score;
            }
        }

        this.fleePath = bestPath;
        this.fleeDestination = bestDestination;
    }

    private void tickStareClock() {
        this.ticksUntilLeave--;
        if (this.ticksUntilLeave < 0.0F) {
            this.shouldLeave = true;
        }
    }

    private void fleeTick() {
        if (this.fleePath == null || this.fleePath.isFinished()) {
            this.setFleePath();
        }

        this.caveDweller.playFleeSound();
        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
        if (this.ticksUntilNextPathRecalculation == 0) {
            this.ticksUntilNextPathRecalculation = 2;
            if (this.fleePath == null || !this.caveDweller.getNavigator().setPath(this.fleePath, this.speedModifier)) {
                this.ticksUntilNextPathRecalculation += 2;
            }
        }
    }

    private int scoreHideDestination(EntityLivingBase target, BlockPos destination, Path path) {
        int score = path.getCurrentPathLength() * 6;
        score += this.caveDweller.world.getLight(destination) * 8;
        score += Math.abs(destination.getY() - MathHelper.floor(this.caveDweller.posY)) * 3;
        score -= Math.min(96, MathHelper.floor(this.getDistanceSqToCenter(target, destination)));
        if (!this.canTargetSeePosition(target, destination)) {
            score -= 80;
        }
        return score;
    }

    private boolean shouldDisappear(EntityLivingBase target) {
        return !this.caveDweller.targetIsFacingMe || !target.canEntityBeSeen(this.caveDweller) || this.hasReachedFleeDestination();
    }

    private boolean hasReachedFleeDestination() {
        return this.fleeDestination != null && this.getDistanceSqToCenter(this.caveDweller, this.fleeDestination) <= HIDE_REACHED_DISTANCE_SQ;
    }

    private boolean canTargetSeePosition(EntityLivingBase target, BlockPos destination) {
        Vec3d start = target.getPositionEyes(1.0F);
        Vec3d end = new Vec3d(
            destination.getX() + 0.5D,
            destination.getY() + this.caveDweller.height * 0.5D,
            destination.getZ() + 0.5D
        );
        RayTraceResult hit = this.caveDweller.world.rayTraceBlocks(start, end, false, true, false);
        return hit == null || hit.typeOfHit == RayTraceResult.Type.MISS;
    }

    private double getDistanceSqToCenter(EntityLivingBase entity, BlockPos pos) {
        double dx = entity.posX - (pos.getX() + 0.5D);
        double dy = entity.posY - pos.getY();
        double dz = entity.posZ - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }
}
