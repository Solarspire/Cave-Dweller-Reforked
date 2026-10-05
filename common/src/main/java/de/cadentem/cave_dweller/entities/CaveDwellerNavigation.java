package de.cadentem.cave_dweller.entities;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

/** Reference navigation hooks, scoped to this entity instead of global mixins. */
public final class CaveDwellerNavigation extends WallClimberNavigation {
    private final CaveDwellerEntity dweller;
    public CaveDwellerNavigation(CaveDwellerEntity dweller, Level level) {
        super(dweller, level);
        this.dweller = dweller;
    }
    @Override
    protected boolean canUpdatePath() {
        return super.canUpdatePath() || !(dweller.hasSpawned() && !dweller.isCrawling());
    }
    @Override
    protected Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange) {
        boolean crawling = dweller.isCrawling();
        dweller.setCrawling(true);
        try { return super.createPath(targets, regionOffset, offsetUpward, accuracy, followRange); }
        finally { dweller.setCrawling(crawling); }
    }
}
