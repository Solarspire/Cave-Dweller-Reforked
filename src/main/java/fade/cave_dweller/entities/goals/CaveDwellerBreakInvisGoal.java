package fade.cave_dweller.entities.goals;

import fade.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.entity.ai.EntityAIBase;

public class CaveDwellerBreakInvisGoal extends EntityAIBase {
    private final CaveDwellerEntity caveDweller;

    public CaveDwellerBreakInvisGoal(CaveDwellerEntity caveDweller) {
        this.caveDweller = caveDweller;
    }

    @Override
    public boolean shouldExecute() {
        return this.caveDweller.isInvisible() && !this.caveDweller.targetIsFacingMe;
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        this.caveDweller.setInvisible(false);
    }
}
