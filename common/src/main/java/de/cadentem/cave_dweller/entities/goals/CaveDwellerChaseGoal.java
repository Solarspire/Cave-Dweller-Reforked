package de.cadentem.cave_dweller.entities.goals;

import de.cadentem.cave_dweller.config.ServerConfig;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.entities.goals.Roll;
import de.cadentem.cave_dweller.util.Utils;
import java.util.EnumSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

public class CaveDwellerChaseGoal
extends Goal {
    private final CaveDwellerEntity caveDweller;
    private final int maxSpeedReached;
    private final boolean followTargetEvenIfNotSeen;
    private long lastGameTimeCheck;
    private int ticksUntilLeave;
    private int ticksUntilNextAttack;
    private int speedUp;

    public CaveDwellerChaseGoal(CaveDwellerEntity caveDweller, boolean followTargetEvenIfNotSeen) {
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.caveDweller = caveDweller;
        this.followTargetEvenIfNotSeen = followTargetEvenIfNotSeen;
        this.ticksUntilLeave = Utils.secondsToTicks((Integer)ServerConfig.TIME_UNTIL_LEAVE_CHASE.get());
        this.maxSpeedReached = Utils.secondsToTicks(3);
    }

    public boolean canUse() {
        boolean canAttack;
        if (this.caveDweller.isInvisible()) {
            return false;
        }
        if (this.caveDweller.currentRoll != Roll.CHASE) {
            return false;
        }
        if (!this.caveDweller.targetIsFacingMe) {
            return false;
        }
        long ticks = this.caveDweller.level().getGameTime();
        if (ticks - this.lastGameTimeCheck < 20L) {
            return false;
        }
        this.lastGameTimeCheck = ticks;
        LivingEntity target = this.caveDweller.getTarget();
        if (!Utils.isValidPlayer((Entity)target)) {
            return false;
        }
        Path path = this.caveDweller.getNavigation().createPath((Entity)target, 0);
        if (path != null) {
            return true;
        }
        boolean bl = canAttack = this.getAttackReachSqr(target) >= this.caveDweller.distanceToSqr((Entity)target);
        if (canAttack) {
            return true;
        }
        path = this.caveDweller.getNavigation().createPath((Entity)target, 0);
        return path != null;
    }

    public boolean canContinueToUse() {
        LivingEntity target = this.caveDweller.getTarget();
        if (!Utils.isValidPlayer((Entity)target)) {
            this.caveDweller.disappear();
            return false;
        }
        if (!this.followTargetEvenIfNotSeen) {
            return !this.caveDweller.getNavigation().isDone();
        }
        return this.caveDweller.isWithinRestriction(target.blockPosition());
    }

    public void start() {
        this.caveDweller.setAggressive(true);
        this.ticksUntilNextAttack = 0;
    }

    public void stop() {
        LivingEntity target = this.caveDweller.getTarget();
        if (!Utils.isValidPlayer((Entity)target)) {
            this.caveDweller.setTarget(null);
        }
        this.speedUp = 0;
        this.caveDweller.setAggressive(false);
        this.caveDweller.getEntityData().set(CaveDwellerEntity.CRAWLING_ACCESSOR, false);
        this.caveDweller.getNavigation().stop();
        this.caveDweller.refreshDimensions();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        boolean targetMoved;
        LivingEntity target;
        if (this.ticksUntilLeave <= 0 && !this.caveDweller.targetIsFacingMe) {
            this.caveDweller.disappear();
        }
        if (!Utils.isValidPlayer((Entity)(target = this.caveDweller.getTarget()))) {
            return;
        }
        Path path = this.caveDweller.getNavigation().getPath();
        this.fixPath(path);
        boolean bl = targetMoved = path != null && path.getEndNode() != null && path.getEndNode().distanceTo(target.blockPosition()) > 2.0f;
        if (path == null || this.caveDweller.getNavigation().isStuck() || targetMoved || path.isDone() && !this.shouldClimb(path) || this.caveDweller.getNavigation().shouldRecomputePath(target.blockPosition()) && this.caveDweller.tickCount % 20 == 0) {
            path = this.caveDweller.getNavigation().createPath((Entity)target, 0);
            this.fixPath(path);
        }
        if (path != null && !path.isDone() && this.caveDweller.hasLineOfSight((Entity)target)) {
            this.caveDweller.playChaseSound();
        }
        this.caveDweller.getNavigation().moveTo(path, this.caveDweller.getSpeedModifier());
        if (!this.caveDweller.isCrawling()) {
            if (this.caveDweller.isAggressive()) {
                this.caveDweller.getLookControl().setLookAt((Entity)target, 90.0f, 90.0f);
            } else {
                this.caveDweller.getLookControl().setLookAt((Entity)target, 180.0f, 1.0f);
            }
        }
        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        double distance = this.caveDweller.distanceToSqr((Entity)target);
        this.checkAndPerformAttack(target, distance);
        --this.ticksUntilLeave;
        if (this.speedUp < this.maxSpeedReached) {
            ++this.speedUp;
        }
    }

    private void fixPath(Path path) {
        LivingEntity target = this.caveDweller.getTarget();
        if (target == null) {
            return;
        }
        if (this.shouldClimb(path) && (double)path.getNode(0).distanceTo(this.caveDweller.getTarget().blockPosition()) > 0.1) {
            path.replaceNode(0, path.getNode(0).cloneAndMove(target.blockPosition().getX(), target.blockPosition().getY(), target.blockPosition().getZ()));
        }
    }

    private boolean shouldClimb(Path path) {
        if (this.caveDweller.getTarget() == null) {
            return false;
        }
        return path != null && path.getNodeCount() == 1 && (float)this.caveDweller.getTarget().blockPosition().getY() > (float)this.caveDweller.blockPosition().getY() + this.caveDweller.maxUpStep();
    }

    private void checkAndPerformAttack(LivingEntity target, double distanceToTarget) {
        double attackReach = this.getAttackReachSqr(target);
        if (distanceToTarget <= attackReach && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();
            this.caveDweller.swing(InteractionHand.MAIN_HAND);
            this.caveDweller.doHurtTarget((Entity)target);
        }
    }

    private void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.adjustedTickDelay(20);
    }

    private double getAttackReachSqr(LivingEntity target) {
        return this.caveDweller.getBbWidth() * 4.0f * this.caveDweller.getBbWidth() * 4.0f + target.getBbWidth();
    }
}
