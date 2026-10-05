package de.cadentem.cave_dweller.entities.goals;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.entities.goals.Roll;
import de.cadentem.cave_dweller.util.Utils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class CaveDwellerTargetTooCloseGoal
extends NearestAttackableTargetGoal<Player> {
    private final CaveDwellerEntity caveDweller;
    private final float distanceThreshold;

    public CaveDwellerTargetTooCloseGoal(CaveDwellerEntity mob, float distanceThreshold) {
        super((Mob)mob, Player.class, false);
        this.caveDweller = mob;
        this.distanceThreshold = distanceThreshold;
    }

    public boolean canUse() {
        Player target;
        if (!this.caveDweller.isInvisible() && Utils.isValidPlayer((Entity)(target = this.caveDweller.level().getNearestPlayer((Entity)this.caveDweller, (double)this.distanceThreshold)))) {
            this.target = target;
            return true;
        }
        return false;
    }

    public void start() {
        this.caveDweller.setAggressive(true);
        this.caveDweller.currentRoll = Roll.CHASE;
        this.caveDweller.setTarget(this.target);
        super.start();
    }

    public void stop() {
        super.stop();
    }

    public boolean canContinueToUse() {
        return Utils.isValidPlayer((Entity)this.target);
    }

    public void tick() {
        super.tick();
    }
}
