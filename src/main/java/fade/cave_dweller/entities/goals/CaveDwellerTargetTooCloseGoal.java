package fade.cave_dweller.entities.goals;

import fade.cave_dweller.entities.CaveDwellerEntity;
import fade.cave_dweller.util.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;

public class CaveDwellerTargetTooCloseGoal extends EntityAINearestAttackableTarget<EntityPlayer> {
    private final CaveDwellerEntity caveDweller;
    private final float distanceThreshold;

    public CaveDwellerTargetTooCloseGoal(CaveDwellerEntity mob, float distanceThreshold) {
        super(mob, EntityPlayer.class, false);
        this.caveDweller = mob;
        this.distanceThreshold = distanceThreshold;
    }

    @Override
    public boolean shouldExecute() {
        if (this.caveDweller.isInvisible()) {
            return false;
        }

        EntityPlayer target = this.caveDweller.world.getClosestPlayerToEntity(this.caveDweller, this.distanceThreshold);
        if (Utils.isValidPlayer(target)) {
            this.targetEntity = target;
            return true;
        }
        return false;
    }

    @Override
    public void startExecuting() {
        this.caveDweller.setAggressive(true);
        this.caveDweller.currentRoll = Roll.CHASE;
        this.caveDweller.setAttackTarget(this.targetEntity);
        super.startExecuting();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return Utils.isValidPlayer(this.targetEntity);
    }
}
