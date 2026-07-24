package fade.cave_dweller.events;

import fade.cave_dweller.CaveDweller;
import fade.cave_dweller.config.ServerConfig;
import fade.cave_dweller.entities.CaveDwellerEntity;
import fade.cave_dweller.util.Utils;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = CaveDweller.MODID)
public final class ForgeEvents {
    public static final ConcurrentHashMap<Integer, Integer> HIT_COUNTER = new ConcurrentHashMap<Integer, Integer>();

    private ForgeEvents() {
    }

    @SubscribeEvent
    public static void handleHurt(LivingAttackEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof CaveDwellerEntity)) {
            return;
        }

        CaveDwellerEntity caveDweller = (CaveDwellerEntity) entity;
        DamageSource source = event.getSource();
        boolean skipDamage = source == DamageSource.IN_WALL || source == DamageSource.CACTUS || source == DamageSource.DROWN;
        boolean increaseCounter = source == DamageSource.IN_WALL || source == DamageSource.CACTUS;

        if (skipDamage) {
            if (increaseCounter && !caveDweller.world.isRemote) {
                HIT_COUNTER.merge(caveDweller.getEntityId(), 1, Integer::sum);
                if (HIT_COUNTER.get(caveDweller.getEntityId()) > 5) {
                    HIT_COUNTER.remove(caveDweller.getEntityId());
                    boolean couldTeleport = caveDweller.teleportToTarget();
                    caveDweller.velocityChanged = true;
                    if (!couldTeleport) {
                        String key = Utils.getDimensionKey(caveDweller.world);
                        if (ServerConfig.isValidDimension(key)) {
                            int spawnDelta = (int) (ServerConfig.canSpawnMin * 0.3D);
                            int noiseDelta = (int) (ServerConfig.resetNoiseMin * 0.3D);
                            CaveDweller.speedUpTimers(key, spawnDelta, noiseDelta);
                        }
                        caveDweller.disappear();
                    }
                }
            }
            event.setCanceled(true);
            return;
        }

        if (source == DamageSource.FALL) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void handleKnockback(LivingKnockBackEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof CaveDwellerEntity) {
            CaveDwellerEntity caveDweller = (CaveDwellerEntity) entity;
            if (caveDweller.isClimbing()) {
                event.setCanceled(true);
            }
        }
    }
}
