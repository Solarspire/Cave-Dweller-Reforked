package de.cadentem.cave_dweller.entities.goals;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.entities.goals.Roll;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class CaveDwellerFleeGoal
extends Goal {
    private final CaveDwellerEntity caveDweller;
    private final double speedModifier;
    private float ticksUntilLeave;
    private float ticksUntilFlee;
    private boolean shouldLeave;
    private Path fleePath;
    private int ticksUntilNextPathRecalculation;

    public CaveDwellerFleeGoal(CaveDwellerEntity caveDweller, float ticksUntilLeave, double speedModifier) {
        this.caveDweller = caveDweller;
        this.ticksUntilLeave = ticksUntilLeave;
        this.ticksUntilFlee = 10.0f;
        this.speedModifier = speedModifier;
    }

    public boolean canUse() {
        if (this.caveDweller.isInvisible()) {
            return false;
        }
        if (this.caveDweller.currentRoll != Roll.FLEE) {
            return false;
        }
        return this.caveDweller.getTarget() != null;
    }

    public boolean canContinueToUse() {
        if (this.caveDweller.currentRoll != Roll.FLEE) {
            return false;
        }
        return this.caveDweller.getTarget() != null;
    }

    public void start() {
        this.setFleePath();
        this.shouldLeave = false;
    }

    public void tick() {
        LivingEntity target = this.caveDweller.getTarget();
        if (this.shouldLeave && !this.caveDweller.targetIsFacingMe) {
            this.caveDweller.disappear();
        }
        this.ticksUntilFlee -= 1.0f;
        this.tickStareClock();
        if (this.ticksUntilFlee <= 0.0f) {
            this.fleeTick();
            this.caveDweller.isFleeing = true;
            this.caveDweller.getEntityData().set(CaveDwellerEntity.FLEEING_ACCESSOR, true);
        } else if (target != null) {
            this.caveDweller.getLookControl().setLookAt((Entity)target, 180.0f, 1.0f);
        }
    }

    private void setFleePath() {
        LivingEntity target = this.caveDweller.getTarget();
        if (target == null) {
            return;
        }
        Vec3 fleePosition = DefaultRandomPos.getPosAway((PathfinderMob)this.caveDweller, (int)32, (int)7, (Vec3)target.position());
        if (fleePosition != null) {
            this.fleePath = this.caveDweller.getNavigation().createPath(fleePosition.x, fleePosition.y, fleePosition.z, 0);
        }
    }

    public void tickStareClock() {
        this.ticksUntilLeave -= 1.0f;
        if (this.ticksUntilLeave < 0.0f) {
            this.shouldLeave = true;
        }
    }

    public void fleeTick() {
        if (this.fleePath == null || this.fleePath.isDone()) {
            this.setFleePath();
        }
        this.caveDweller.playFleeSound();
        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
        if (this.ticksUntilNextPathRecalculation == 0) {
            this.ticksUntilNextPathRecalculation = 2;
            if (!this.caveDweller.getNavigation().moveTo(this.fleePath, this.speedModifier)) {
                this.ticksUntilNextPathRecalculation += 2;
            }
            this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
        }
    }
}
