package fade.cave_dweller.entities.goals;

import fade.cave_dweller.config.ServerConfig;
import fade.cave_dweller.util.Utils;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIDoorInteract;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;

public class CaveDwellerBreakDoorGoal extends EntityAIDoorInteract {
    private final Predicate<EnumDifficulty> validDifficulties;
    private int breakingTime;
    private int previousBreakProgress = -1;

    public CaveDwellerBreakDoorGoal(EntityLiving entity, Predicate<EnumDifficulty> validDifficulties) {
        super(entity);
        this.validDifficulties = validDifficulties;
    }

    @Override
    public boolean shouldExecute() {
        if (!super.shouldExecute()) {
            return false;
        }
        if (!this.entity.world.getGameRules().getBoolean("mobGriefing")) {
            return false;
        }
        return this.validDifficulties.test(this.entity.world.getDifficulty()) && !this.doorBlock.isOpen(this.entity.world, this.doorPosition);
    }

    @Override
    public boolean shouldContinueExecuting() {
        double distanceSq = this.entity.getDistanceSq((double) this.doorPosition.getX(), this.entity.posY, (double) this.doorPosition.getZ());
        return this.breakingTime <= Utils.secondsToTicks(ServerConfig.breakDoorTime)
            && !this.doorBlock.isOpen(this.entity.world, this.doorPosition)
            && distanceSq < 4.0D
            && this.validDifficulties.test(this.entity.world.getDifficulty());
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        this.breakingTime = 0;
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.entity.world.sendBlockBreakProgress(this.entity.getEntityId(), this.doorPosition, -1);
    }

    @Override
    public void updateTask() {
        super.updateTask();
        if (this.entity.getRNG().nextInt(20) == 0) {
            this.entity.world.playEvent(1019, this.doorPosition, 0);
        }

        this.breakingTime++;
        int maxBreak = Math.max(1, Utils.secondsToTicks(ServerConfig.breakDoorTime));
        int progress = (int) ((float) this.breakingTime / (float) maxBreak * 10.0F);
        if (progress != this.previousBreakProgress) {
            this.entity.world.sendBlockBreakProgress(this.entity.getEntityId(), this.doorPosition, progress);
            this.previousBreakProgress = progress;
        }

        if (this.breakingTime == maxBreak && this.validDifficulties.test(this.entity.world.getDifficulty())) {
            this.entity.world.setBlockToAir(this.doorPosition);
            this.entity.world.playEvent(1021, this.doorPosition, 0);
            this.entity.world.playEvent(2001, this.doorPosition, Block.getIdFromBlock(this.doorBlock));
        }
    }
}
