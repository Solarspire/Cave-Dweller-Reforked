package fade.cave_dweller.entities.goals;

import fade.cave_dweller.entities.CaveDwellerEntity;
import fade.cave_dweller.util.Utils;
import java.util.Arrays;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.Path;

public class CaveDwellerStareGoal extends EntityAIBase {
    private static final int MAX_STARE_LOCK_TICKS = Utils.secondsToTicks(10);
    private static final double TOO_CLOSE_CHASE_DISTANCE_SQ = 12.0D * 12.0D;

    private final CaveDwellerEntity caveDweller;
    private boolean wasNotLookingPreviously;
    private int lookedAtCount;
    private int stareTicks;
    private final int lookedAtMax;

    public CaveDwellerStareGoal(CaveDwellerEntity caveDweller) {
        this.caveDweller = caveDweller;
        this.lookedAtMax = caveDweller.getRNG().nextInt(8) + 8;
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (this.caveDweller.isInvisible()) {
            return false;
        }
        if (!Utils.isValidPlayer(this.caveDweller.getAttackTarget())) {
            return false;
        }
        return this.caveDweller.currentRoll == Roll.STARE;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (!Utils.isValidPlayer(this.caveDweller.getAttackTarget())) {
            return false;
        }
        return this.caveDweller.currentRoll == Roll.STARE;
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        this.stareTicks = 0;
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.lookedAtCount = 0;
        this.stareTicks = 0;
        this.wasNotLookingPreviously = false;
        this.caveDweller.pleaseStopMoving = false;
        this.caveDweller.setSpotted(false);
    }

    @Override
    public void updateTask() {
        EntityLivingBase target = this.caveDweller.getAttackTarget();
        if (target == null) {
            this.caveDweller.disappear();
            return;
        }

        if (this.caveDweller.getDistanceSq(target) <= TOO_CLOSE_CHASE_DISTANCE_SQ) {
            this.forceChase();
            return;
        }

        boolean actuallyLooking = this.caveDweller.targetIsFacingMe && target.canEntityBeSeen(this.caveDweller);
        if (this.wasNotLookingPreviously && actuallyLooking) {
            this.lookedAtCount++;
        }

        if (this.lookedAtCount > this.lookedAtMax && !actuallyLooking) {
            if (this.caveDweller.getRNG().nextDouble() < 0.1D) {
                this.caveDweller.disappear();
                return;
            }
            if (this.caveDweller.getRNG().nextDouble() < 0.3D) {
                this.caveDweller.pickRoll(Arrays.asList(Roll.CHASE, Roll.FLEE));
                return;
            }
        }

        if (!actuallyLooking) {
            this.caveDweller.pleaseStopMoving = false;
            boolean moved = this.caveDweller.getNavigator().tryMoveToEntityLiving(target, 1.0D);
            if (!moved) {
                Path path = this.caveDweller.buildPathToTarget(target);
                if (path != null) {
                    this.caveDweller.getNavigator().setPath(path, 1.0D);
                } else {
                    this.caveDweller.getMoveHelper().setMoveTo(target.posX, target.posY, target.posZ, 1.0D);
                }
            }
        } else {
            this.caveDweller.pleaseStopMoving = true;
            this.caveDweller.getNavigator().clearPath();
            this.caveDweller.motionX = 0.0D;
            this.caveDweller.motionY = 0.0D;
            this.caveDweller.motionZ = 0.0D;
        }

        this.caveDweller.getLookHelper().setLookPositionWithEntity(target, 180.0F, 1.0F);
        this.wasNotLookingPreviously = !actuallyLooking;

        this.stareTicks++;
        if (this.stareTicks >= MAX_STARE_LOCK_TICKS && this.caveDweller.currentRoll == Roll.STARE) {
            this.forceChase();
        }
    }

    private void forceChase() {
        this.caveDweller.pleaseStopMoving = false;
        this.caveDweller.currentRoll = Roll.CHASE;
    }
}
