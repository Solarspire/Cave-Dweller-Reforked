package fade.cave_dweller.entities.goals;

import fade.cave_dweller.config.ServerConfig;
import fade.cave_dweller.entities.CaveDwellerEntity.ChasePathPlan;
import fade.cave_dweller.entities.CaveDwellerEntity;
import fade.cave_dweller.entities.CaveDwellerNavigation;
import fade.cave_dweller.util.Utils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

public class CaveDwellerChaseGoal extends EntityAIBase {
    private static final double CLIMB_TARGET_BREAK_DISTANCE_SQ = 9.0D;
    private static final int UNREACHABLE_HIGH_TARGET_TICKS = Utils.secondsToTicks(2);

    private final CaveDwellerEntity caveDweller;
    private final int maxSpeedReached;
    private final boolean followTargetEvenIfNotSeen;
    private long lastGameTimeCheck;
    private int ticksUntilLeave;
    private int ticksUntilNextAttack;
    private int speedUp;
    private int unreachableHighTargetTicks;
    private BlockPos climbTarget;
    private BlockPos pathDestination;

    public CaveDwellerChaseGoal(CaveDwellerEntity caveDweller, boolean followTargetEvenIfNotSeen) {
        this.caveDweller = caveDweller;
        this.followTargetEvenIfNotSeen = followTargetEvenIfNotSeen;
        this.ticksUntilLeave = Utils.secondsToTicks(ServerConfig.timeUntilLeaveChase);
        this.maxSpeedReached = Utils.secondsToTicks(3);
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (this.caveDweller.isInvisible()) {
            return false;
        }
        if (this.caveDweller.currentRoll != Roll.CHASE) {
            return false;
        }

        long ticks = this.caveDweller.world.getTotalWorldTime();
        if (ticks - this.lastGameTimeCheck < 20L) {
            return false;
        }
        this.lastGameTimeCheck = ticks;

        EntityLivingBase target = this.caveDweller.getAttackTarget();
        if (!Utils.isValidPlayer(target)) {
            return false;
        }

        ChasePathPlan plan = this.caveDweller.buildPathPlanToTarget(target);
        if (plan != null && plan.getPath() != null) {
            return true;
        }
        return this.getAttackReachSqr(target) >= this.caveDweller.getDistanceSq(target);
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (this.caveDweller.currentRoll != Roll.CHASE) {
            return false;
        }

        EntityLivingBase target = this.caveDweller.getAttackTarget();
        if (!Utils.isValidPlayer(target)) {
            this.caveDweller.disappear();
            return false;
        }

        if (!this.followTargetEvenIfNotSeen) {
            return !this.caveDweller.getNavigator().noPath();
        }
        return target != null && target.isEntityAlive() && this.caveDweller.isWithinHomeDistanceFromPosition(target.getPosition());
    }

    @Override
    public void startExecuting() {
        this.caveDweller.setAggressive(true);
        this.ticksUntilNextAttack = 0;
        this.ticksUntilLeave = Utils.secondsToTicks(ServerConfig.timeUntilLeaveChase);
        this.speedUp = 0;
        this.unreachableHighTargetTicks = 0;
        this.climbTarget = null;
        this.pathDestination = null;
    }

    @Override
    public void resetTask() {
        EntityLivingBase target = this.caveDweller.getAttackTarget();
        if (!Utils.isValidPlayer(target)) {
            this.caveDweller.setAttackTarget(null);
        }
        this.speedUp = 0;
        this.unreachableHighTargetTicks = 0;
        this.caveDweller.setAggressive(false);
        this.caveDweller.setCrawling(false);
        this.caveDweller.getNavigator().clearPath();
        this.clearClimbTarget();
        this.pathDestination = null;
    }

    @Override
    public void updateTask() {
        if (this.ticksUntilLeave <= 0 && !this.caveDweller.targetIsFacingMe) {
            this.caveDweller.disappear();
            return;
        }

        EntityLivingBase target = this.caveDweller.getAttackTarget();
        if (!Utils.isValidPlayer(target)) {
            this.caveDweller.setAttackTarget(null);
            return;
        }

        Path path = this.caveDweller.getNavigator().getPath();
        this.fixPath(path);
        this.updateClimbTarget(path, target);
        boolean holdingClimbTarget = this.shouldHoldClimbTarget(target);
        boolean targetMoved = !holdingClimbTarget && this.hasTargetMoved(path, target);
        boolean shouldRepath =
            (!holdingClimbTarget && path == null)
                || (!holdingClimbTarget && this.caveDweller.getNavigator().noPath())
                || targetMoved
                || (!holdingClimbTarget && path != null && path.isFinished() && !this.shouldClimb(path))
                || (!holdingClimbTarget && this.caveDweller.ticksExisted % 20 == 0);
        if (shouldRepath || this.pathDestination == null) {
            ChasePathPlan plan = this.caveDweller.buildPathPlanToTarget(target);
            path = plan == null ? null : plan.getPath();
            this.pathDestination = plan == null ? target.getPosition() : plan.getDestination();
            this.fixPath(path);
            this.updateClimbTarget(path, target);
            holdingClimbTarget = this.shouldHoldClimbTarget(target);
        }

        if (this.shouldRetreatFromHighTarget(target, path, holdingClimbTarget)) {
            this.unreachableHighTargetTicks++;
            if (this.unreachableHighTargetTicks >= UNREACHABLE_HIGH_TARGET_TICKS) {
                this.beginHideRetreat();
                return;
            }
        } else {
            this.unreachableHighTargetTicks = 0;
        }

        if (path != null && !path.isFinished() && this.caveDweller.getEntitySenses().canSee(target)) {
            this.caveDweller.playChaseSound();
        }

        if (holdingClimbTarget) {
            this.moveToClimbTarget();
        }

        if (path != null) {
            this.caveDweller.getNavigator().setPath(path, this.caveDweller.getSpeedModifier());
        } else if (!holdingClimbTarget && this.caveDweller.getDistanceSq(target) <= this.getAttackReachSqr(target) + 1.0D) {
            this.caveDweller.getMoveHelper().setMoveTo(target.posX, target.posY, target.posZ, this.caveDweller.getSpeedModifier());
        } else if (!holdingClimbTarget && this.caveDweller.getNavigator() instanceof CaveDwellerNavigation) {
            ((CaveDwellerNavigation) this.caveDweller.getNavigator()).clearTrackedTargetPosition();
        }

        if (!this.caveDweller.isCrawling()) {
            if (this.caveDweller.isAggressive()) {
                this.caveDweller.getLookHelper().setLookPositionWithEntity(target, 90.0F, 90.0F);
            } else {
                this.caveDweller.getLookHelper().setLookPositionWithEntity(target, 180.0F, 1.0F);
            }
        }

        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        this.checkAndPerformAttack(target, this.caveDweller.getDistanceSq(target));
        this.ticksUntilLeave--;

        if (this.speedUp < this.maxSpeedReached) {
            this.speedUp++;
        }
    }

    private void fixPath(Path path) {
        EntityLivingBase target = this.caveDweller.getAttackTarget();
        if (target == null || path == null) {
            return;
        }

        BlockPos destination = this.pathDestination == null ? target.getPosition() : this.pathDestination;
        if (!this.shouldClimb(path, destination) || path.getCurrentPathLength() <= 0) {
            return;
        }

        PathPoint point = path.getPathPointFromIndex(0);
        PathPoint destinationPoint = new PathPoint(destination.getX(), destination.getY(), destination.getZ());
        if (point.distanceToSquared(destinationPoint) <= 0.1F) {
            return;
        }
        PathPoint adjustedPoint = point.cloneMove(destination.getX(), destination.getY(), destination.getZ());
        path.setPoint(0, adjustedPoint);
    }

    private boolean hasTargetMoved(Path path, EntityLivingBase target) {
        if (path == null || target == null) {
            return false;
        }

        PathPoint finalPoint = path.getFinalPathPoint();
        if (finalPoint == null) {
            return false;
        }

        BlockPos destination = this.pathDestination == null ? target.getPosition() : this.pathDestination;
        PathPoint targetPoint = new PathPoint(destination.getX(), destination.getY(), destination.getZ());
        return finalPoint.distanceToSquared(targetPoint) > 2.0F;
    }

    private boolean shouldClimb(Path path) {
        return this.shouldClimb(path, this.getCurrentDestination(this.caveDweller.getAttackTarget()));
    }

    private boolean shouldClimb(Path path, BlockPos destination) {
        EntityLivingBase target = this.caveDweller.getAttackTarget();
        if (target == null || path == null || destination == null) {
            return false;
        }

        return
            path.getCurrentPathLength() == 1
                && destination.getY() > this.caveDweller.getPosition().getY() + this.caveDweller.stepHeight;
    }

    private boolean shouldRetreatFromHighTarget(EntityLivingBase target, Path path, boolean holdingClimbTarget) {
        if (!this.isTargetMeaningfullyAbove(target)) {
            return false;
        }

        if (holdingClimbTarget || this.caveDweller.isClimbing()) {
            return false;
        }

        BlockPos destination = this.getCurrentDestination(target);
        if (destination == null) {
            return true;
        }

        if (this.shouldClimb(path, destination)) {
            return false;
        }

        if (path == null) {
            return true;
        }

        PathPoint finalPoint = path.getFinalPathPoint();
        int destinationY = finalPoint == null ? destination.getY() : finalPoint.y;
        return destinationY + 1 < target.getPosition().getY();
    }

    private boolean isTargetMeaningfullyAbove(EntityLivingBase target) {
        return target.posY > this.caveDweller.posY + this.caveDweller.stepHeight + 1.0D;
    }

    private void checkAndPerformAttack(EntityLivingBase target, double distanceToTarget) {
        double attackReach = this.getAttackReachSqr(target);
        if (distanceToTarget <= attackReach && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();
            this.caveDweller.swingArm(EnumHand.MAIN_HAND);
            this.caveDweller.attackEntityAsMob(target);
        }
    }

    private void resetAttackCooldown() {
        this.ticksUntilNextAttack = 20;
    }

    private double getAttackReachSqr(EntityLivingBase target) {
        return this.caveDweller.width * 4.0F * this.caveDweller.width * 4.0F + target.width;
    }

    private void updateClimbTarget(Path path, EntityLivingBase target) {
        if (target == null) {
            this.clearClimbTarget();
            return;
        }

        if (!this.shouldClimb(path)) {
            if (!this.caveDweller.isClimbing() && this.caveDweller.onGround) {
                this.clearClimbTarget();
            }
            return;
        }

        BlockPos destination = this.getCurrentDestination(target);
        if (this.climbTarget == null || this.isClimbTargetStale(destination)) {
            this.climbTarget = destination;
        }
    }

    private boolean shouldHoldClimbTarget(EntityLivingBase target) {
        if (this.climbTarget == null || target == null) {
            return false;
        }

        if (this.caveDweller.shouldAbortBlockedClimb()) {
            this.clearClimbTarget();
            return false;
        }

        if (!this.caveDweller.isClimbing() && this.caveDweller.onGround && this.caveDweller.posY + this.caveDweller.stepHeight >= this.climbTarget.getY()) {
            this.clearClimbTarget();
            return false;
        }

        if (this.getHorizontalDistanceSq(target.getPosition(), this.climbTarget) > CLIMB_TARGET_BREAK_DISTANCE_SQ) {
            this.clearClimbTarget();
            return false;
        }

        return true;
    }

    private void moveToClimbTarget() {
        if (this.climbTarget == null) {
            return;
        }

        double speed = this.caveDweller.getSpeedModifier();
        if (this.caveDweller.getNavigator() instanceof CaveDwellerNavigation) {
            CaveDwellerNavigation navigation = (CaveDwellerNavigation) this.caveDweller.getNavigator();
            navigation.setTrackedTargetPosition(this.climbTarget, speed);
        } else if (!this.caveDweller.getNavigator().tryMoveToXYZ(
            this.climbTarget.getX() + 0.5D,
            this.climbTarget.getY(),
            this.climbTarget.getZ() + 0.5D,
            speed
        )) {
            this.caveDweller.getMoveHelper().setMoveTo(
                this.climbTarget.getX() + 0.5D,
                this.climbTarget.getY(),
                this.climbTarget.getZ() + 0.5D,
                speed
            );
        }
    }

    private boolean isClimbTargetStale(BlockPos targetPos) {
        if (this.climbTarget == null) {
            return true;
        }

        if (Math.abs(targetPos.getY() - this.climbTarget.getY()) > 1) {
            return true;
        }

        return this.getHorizontalDistanceSq(targetPos, this.climbTarget) > CLIMB_TARGET_BREAK_DISTANCE_SQ;
    }

    private double getHorizontalDistanceSq(BlockPos first, BlockPos second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private void clearClimbTarget() {
        this.climbTarget = null;
        if (this.caveDweller.getNavigator() instanceof CaveDwellerNavigation) {
            CaveDwellerNavigation navigation = (CaveDwellerNavigation) this.caveDweller.getNavigator();
            navigation.clearTrackedTargetPosition();
        }
    }

    private void beginHideRetreat() {
        this.unreachableHighTargetTicks = 0;
        this.pathDestination = null;
        this.clearClimbTarget();
        this.caveDweller.getNavigator().clearPath();
        this.caveDweller.setAggressive(false);
        this.caveDweller.setClimbing(false);
        this.caveDweller.setCrouching(false);
        this.caveDweller.setCrawling(false);
        this.caveDweller.pleaseStopMoving = false;
        this.caveDweller.currentRoll = Roll.FLEE;
    }

    private BlockPos getCurrentDestination(EntityLivingBase target) {
        return this.pathDestination == null && target != null ? target.getPosition() : this.pathDestination;
    }
}
