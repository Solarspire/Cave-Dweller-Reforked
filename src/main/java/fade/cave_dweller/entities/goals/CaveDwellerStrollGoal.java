package fade.cave_dweller.entities.goals;

import fade.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;

public class CaveDwellerStrollGoal extends EntityAIWanderAvoidWater {
    public CaveDwellerStrollGoal(CaveDwellerEntity mob, double speedModifier) {
        super(mob, speedModifier);
    }

    @Override
    public boolean shouldExecute() {
        return ((CaveDwellerEntity) this.entity).currentRoll == Roll.STROLL && super.shouldExecute();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return ((CaveDwellerEntity) this.entity).currentRoll == Roll.STROLL && super.shouldContinueExecuting();
    }
}
