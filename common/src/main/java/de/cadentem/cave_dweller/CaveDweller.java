package de.cadentem.cave_dweller;

import com.mojang.logging.LogUtils;
import de.cadentem.cave_dweller.config.ServerConfig;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.registry.ModEntityTypes;
import de.cadentem.cave_dweller.registry.ModSounds;
import de.cadentem.cave_dweller.util.Timer;
import de.cadentem.cave_dweller.util.Utils;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import org.slf4j.Logger;

public class CaveDweller {
    public static final String MODID = "cave_dweller";
    public static final Logger LOG = LogUtils.getLogger();
    public static final Random RANDOM = new Random();
    private static final HashMap<String, Timer> TIMERS = new HashMap();
    public static boolean RELOAD_ALL = false;
    public static boolean RELOAD_MISSING = false;

    public static final CaveDweller INSTANCE = new CaveDweller();
    private CaveDweller() {}
    public static void resetTimers() { TIMERS.clear(); RELOAD_ALL = true; }
    public void serverTick(net.minecraft.server.MinecraftServer server) {
        String key;
        if (RELOAD_ALL) {
            TIMERS.clear();
            RELOAD_ALL = false;
        }
        Iterable<ServerLevel> levels = server.getAllLevels();
        if (TIMERS.isEmpty()) {
            for (ServerLevel level : levels) {
                key = level.dimension().location().toString();
                boolean isRelevant = ((List)ServerConfig.DIMENSION_WHITELIST.get()).contains(key);
                if (!isRelevant) continue;
                TIMERS.put(key, new Timer());
            }
            if (TIMERS.isEmpty() && server.getTickCount() % 6000 == 0) {
                LOG.debug("There are currently no timers present - are the dimensions properly configured?");
            }
            RELOAD_ALL = false;
            RELOAD_MISSING = false;
        } else if (RELOAD_MISSING) {
            for (ServerLevel level : levels) {
                key = level.dimension().location().toString();
                boolean isRelevant = TIMERS.get(key) == null && ((List)ServerConfig.DIMENSION_WHITELIST.get()).contains(key);
                if (!isRelevant) continue;
                TIMERS.put(key, new Timer());
            }
            RELOAD_MISSING = false;
        }
        for (ServerLevel level : levels) {
            key = level.dimension().location().toString();
            if (TIMERS.get(key) == null) continue;
            this.handleLogic(level);
        }
    }

    private void handleLogic(ServerLevel level) {
        if (level == null) {
            return;
        }
        List<ServerPlayer> players = level.getPlayers(this::isRelevantPlayer);
        if (players.isEmpty()) {
            return;
        }
        String key = level.dimension().location().toString();
        Timer timer = TIMERS.get(key);
        if (timer.currentVictim == null || players.stream().filter(element -> element.getStringUUID().equals(timer.currentVictim.getStringUUID())).toList().isEmpty()) {
            timer.currentVictim = (Entity)players.get(RANDOM.nextInt(players.size()));
        }
        Iterable<Entity> entities = level.getAllEntities();
        AtomicInteger caveDwellerCount = new AtomicInteger();
        entities.forEach(entity -> {
            if (entity instanceof CaveDwellerEntity) {
                caveDwellerCount.getAndAdd(1);
            }
        });
        ++timer.currentSpawn;
        ++timer.currentNoise;
        if (timer.isNoiseTimerReached() && (caveDwellerCount.get() > 0 || timer.currentSpawn >= Utils.secondsToTicks((Integer)ServerConfig.CAN_SPAWN_MAX.get()) / 2)) {
            players.forEach(this::playCaveSoundToSpelunkers);
            timer.resetNoiseTimer();
        }
        if (timer.isSpawnTimerReached() && caveDwellerCount.get() < (Integer)ServerConfig.MAXIMUM_AMOUNT.get() && RANDOM.nextDouble() <= (Double)ServerConfig.SPAWN_CHANCE_PER_TICK.get() && timer.currentVictim != null) {
            level.getPlayers(this::playCaveSoundToSpelunkers);
            timer.resetNoiseTimer();
            Optional<CaveDwellerEntity> optionalEntity = Utils.trySpawnMob(timer.currentVictim, ModEntityTypes.CAVE_DWELLER.get(), MobSpawnType.TRIGGERED, level, timer.currentVictim.blockPosition(), 40, 35, 6, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER);
            if (optionalEntity.isPresent()) {
                CaveDwellerEntity caveDweller = (CaveDwellerEntity)((Object)optionalEntity.get());
                caveDweller.setInvisible(true);
                caveDweller.hasSpawned = true;
                timer.resetSpawnTimer();
            } else {
                timer.currentVictim = null;
            }
        }
    }

    private boolean playCaveSoundToSpelunkers(ServerPlayer player) {
        ResourceLocation soundLocation = switch (RANDOM.nextInt(4)) {
            case 1 -> ((SoundEvent)ModSounds.CAVENOISE_2.get()).getLocation();
            case 2 -> ((SoundEvent)ModSounds.CAVENOISE_3.get()).getLocation();
            case 3 -> ((SoundEvent)ModSounds.CAVENOISE_4.get()).getLocation();
            default -> ((SoundEvent)ModSounds.CAVENOISE_1.get()).getLocation();
        };
        playPrivateSound(player, soundLocation, 2.0f, 1.0f);
        return true;
    }

    private boolean isRelevantPlayer(ServerPlayer player) {
        if (!Utils.isValidPlayer((Entity)player)) {
            return false;
        }
        if (player.position().y > (double)((Integer)ServerConfig.SPAWN_HEIGHT.get()).intValue()) {
            return false;
        }
        Level serverLevel = player.level();
        int actualSkyLightLevel = serverLevel.getBrightness(LightLayer.SKY, player.blockPosition()) - serverLevel.getSkyDarken();
        float sunAngle = serverLevel.getSunAngle(1.0f);
        if (actualSkyLightLevel > 0) {
            float f1 = sunAngle < (float)Math.PI ? 0.0f : (float)Math.PI * 2;
            sunAngle += (f1 - sunAngle) * 0.2f;
            actualSkyLightLevel = Math.round((float)actualSkyLightLevel * Mth.cos((float)sunAngle));
        }
        if ((actualSkyLightLevel = Mth.clamp((int)actualSkyLightLevel, (int)0, (int)15)) > (Integer)ServerConfig.SKY_LIGHT_LEVEL.get()) {
            return false;
        }
        LayerLightEventListener blockLighting = serverLevel.getLightEngine().getLayerListener(LightLayer.BLOCK);
        if (blockLighting.getLightValue(player.blockPosition()) > (Integer)ServerConfig.BLOCK_LIGHT_LEVEL.get()) {
            return false;
        }
        boolean isOnSurface = Utils.isOnSurface((Entity)player);
        if (isOnSurface) {
            if (!((Boolean)ServerConfig.ALLOW_SURFACE_SPAWN.get()).booleanValue()) {
                return false;
            }
            return ServerConfig.isInValidBiome((Entity)player);
        }
        return true;
    }

    public static void speedUpTimers(String key, int spawnDelta, int noiseDelta) {
        Timer timer = TIMERS.get(key);
        LOG.debug("Speeding up timers for the dimension [{}], timer: [{}]", (Object)key, (Object)timer);
        if (timer != null) {
            timer.currentSpawn += spawnDelta;
            timer.currentNoise += noiseDelta;
        }
    }

    public static void playPrivateSound(ServerPlayer player, ResourceLocation sound, float volume, float pitch) {
        var event = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(sound);
        if (event == null) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.Holder.direct(event), net.minecraft.sounds.SoundSource.AMBIENT,
                player.getX(), player.getY(), player.getZ(), volume, pitch, RANDOM.nextLong()));
    }
}
