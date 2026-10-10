package fade.cave_dweller.entities.goals;

import fade.cave_dweller.entities.CaveDwellerEntity;
import fade.cave_dweller.util.Utils;
import java.util.Arrays;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;

public class CaveDwellerTargetSeesMeGoal extends EntityAINearestAttackableTarget<EntityPlayer> {
    private final CaveDwellerEntity caveDweller;

    public CaveDwellerTargetSeesMeGoal(CaveDwellerEntity mob) {
        super(mob, EntityPlayer.class, false);
        this.caveDweller = mob;
    }

    @Override
    public boolean shouldExecute() {
        if (this.caveDweller.isInvisible()) {
            return false;
        }

        EntityPlayerMP target = Utils.getValidTarget(this.caveDweller);
        this.targetEntity = target;
        if (!Utils.isValidPlayer(this.targetEntity)) {
            return false;
        }
        return this.caveDweller.isLookingAtMe(this.targetEntity, true);
    }

    @Override
    public void startExecuting() {
        this.caveDweller.setAttackTarget(this.targetEntity);
        this.caveDweller.setSpotted(true);
        if (this.targetEntity != null) {
            this.caveDweller.pickRoll(Arrays.asList(Roll.CHASE, Roll.STARE, Roll.STARE, Roll.FLEE));
        }
        super.startExecuting();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return Utils.isValidPlayer(this.targetEntity);
    }
}
