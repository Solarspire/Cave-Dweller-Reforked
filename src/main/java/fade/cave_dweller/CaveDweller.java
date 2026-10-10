package fade.cave_dweller;

import fade.cave_dweller.config.ServerConfig;
import fade.cave_dweller.entities.CaveDwellerEntity;
import fade.cave_dweller.network.CaveSoundPacket;
import fade.cave_dweller.network.NetworkHandler;
import fade.cave_dweller.proxy.CommonProxy;
import fade.cave_dweller.registry.ModSounds;
import fade.cave_dweller.util.Timer;
import fade.cave_dweller.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib3.GeckoLib;

@Mod(
    modid = CaveDweller.MODID,
    name = CaveDweller.NAME,
    version = CaveDweller.VERSION,
    dependencies = "required-after:forge@[14.23.5.2860,);required-after:geckolib3"
)
public class CaveDweller {
    public static final String MODID = "cave_dweller";
    public static final String NAME = "Cave Dweller Reimagined";
    public static final String VERSION = "1.6.4-1.12.2";
    public static final Logger LOG = LogManager.getLogger(MODID);
    public static final Random RANDOM = new Random();
    public static final int CAVE_DWELLER_ENTITY_ID = 1;

    private static final Map<String, Timer> TIMERS = new HashMap<String, Timer>();
    public static boolean RELOAD_ALL = false;

    @Mod.Instance(MODID)
    public static CaveDweller INSTANCE;

    @SidedProxy(
        clientSide = "fade.cave_dweller.proxy.ClientProxy",
        serverSide = "fade.cave_dweller.proxy.CommonProxy"
    )
    public static CommonProxy PROXY;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GeckoLib.initialize();
        NetworkHandler.register();
        PROXY.preInit(event);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        PROXY.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        PROXY.postInit(event);
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        RELOAD_ALL = true;
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.world.isRemote) {
            return;
        }

        if (!(event.world instanceof WorldServer)) {
            return;
        }

        WorldServer world = (WorldServer) event.world;
        if (RELOAD_ALL) {
            TIMERS.clear();
            RELOAD_ALL = false;
        }

        String key = Utils.getDimensionKey(world);
        if (!TIMERS.containsKey(key) && ServerConfig.isValidDimension(key)) {
            TIMERS.put(key, new Timer());
            LOG.debug("Created Cave Dweller timer for dimension key [{}]", key);
        }

        Timer timer = TIMERS.get(key);
        if (timer != null) {
            this.handleLogic(world, timer);
        }
    }

    private void handleLogic(WorldServer world, Timer timer) {
        List<EntityPlayerMP> players = new ArrayList<EntityPlayerMP>();
        for (EntityPlayer player : world.playerEntities) {
            if (player instanceof EntityPlayerMP && this.isRelevantPlayer((EntityPlayerMP) player)) {
                players.add((EntityPlayerMP) player);
            }
        }

        if (players.isEmpty()) {
            return;
        }

        if (timer.currentVictim == null || !timer.currentVictim.isEntityAlive() || !players.contains(timer.currentVictim)) {
            timer.currentVictim = players.get(RANDOM.nextInt(players.size()));
        }

        int caveDwellerCount = 0;
        for (Entity entity : world.loadedEntityList) {
            if (entity instanceof CaveDwellerEntity) {
                caveDwellerCount++;
            }
        }

        timer.currentSpawn++;
        timer.currentNoise++;

        if (timer.isNoiseTimerReached() && (caveDwellerCount > 0 || timer.currentSpawn >= Utils.secondsToTicks(ServerConfig.canSpawnMax) / 2)) {
            for (EntityPlayerMP player : players) {
                playCaveSoundToSpelunker(player);
            }
            timer.resetNoiseTimer();
        }

        if (!timer.isSpawnTimerReached()) {
            return;
        }

        if (caveDwellerCount >= ServerConfig.maximumAmount) {
            return;
        }

        if (RANDOM.nextDouble() > ServerConfig.spawnChancePerTick) {
            return;
        }

        if (!(timer.currentVictim instanceof EntityPlayerMP)) {
            return;
        }

        for (EntityPlayerMP player : players) {
            playCaveSoundToSpelunker(player);
        }
        timer.resetNoiseTimer();
        String key = Utils.getDimensionKey(world);

        EntityPlayerMP victim = (EntityPlayerMP) timer.currentVictim;
        Optional<CaveDwellerEntity> spawned = Utils.trySpawnMob(victim, world, victim.getPosition(), 40, 35, 6);
        if (spawned.isPresent()) {
            CaveDwellerEntity caveDweller = spawned.get();
            caveDweller.setInvisible(true);
            caveDweller.hasSpawned = true;
            timer.resetSpawnTimer();
            LOG.debug("Spawned Cave Dweller in [{}] near victim [{}] at [{}]", key, victim.getName(), caveDweller.getPosition());
        } else {
            timer.currentVictim = null;
            LOG.debug("Failed Cave Dweller spawn attempt in [{}] for victim [{}]", key, victim.getName());
        }
    }

    private boolean playCaveSoundToSpelunker(EntityPlayerMP player) {
        SoundEvent sound;
        int choice = RANDOM.nextInt(4);
        if (choice == 1) {
            sound = ModSounds.CAVENOISE_2;
        } else if (choice == 2) {
            sound = ModSounds.CAVENOISE_3;
        } else if (choice == 3) {
            sound = ModSounds.CAVENOISE_4;
        } else {
            sound = ModSounds.CAVENOISE_1;
        }

        if (sound == null) {
            sound = SoundEvents.AMBIENT_CAVE;
        }

        ResourceLocation soundLocation = sound.getRegistryName();
        if (soundLocation == null) {
            return false;
        }

        SimpleNetworkWrapper channel = NetworkHandler.CHANNEL;
        channel.sendTo(new CaveSoundPacket(soundLocation, player.getPosition(), 2.0F, 1.0F), player);
        return true;
    }

    private boolean isRelevantPlayer(@Nullable EntityPlayerMP player) {
        if (!Utils.isValidPlayer(player)) {
            return false;
        }

        if (player.posY > ServerConfig.spawnHeight) {
            return false;
        }

        World world = player.world;
        BlockPos pos = player.getPosition();
        int actualSkyLight = world.getLightFor(EnumSkyBlock.SKY, pos) - world.getSkylightSubtracted();
        float sunAngle = world.getCelestialAngle(1.0F);
        if (actualSkyLight > 0) {
            float f1 = sunAngle < (float) Math.PI ? 0.0F : ((float) Math.PI * 2F);
            sunAngle += (f1 - sunAngle) * 0.2F;
            actualSkyLight = Math.round((float) actualSkyLight * MathHelper.cos(sunAngle));
        }

        actualSkyLight = MathHelper.clamp(actualSkyLight, 0, 15);
        if (actualSkyLight > ServerConfig.skyLightLevel) {
            return false;
        }

        int blockLight = world.getLightFor(EnumSkyBlock.BLOCK, pos);
        if (blockLight > ServerConfig.blockLightLevel) {
            return false;
        }

        boolean isOnSurface = Utils.isOnSurface(player);
        if (isOnSurface) {
            if (!ServerConfig.allowSurfaceSpawn) {
                return false;
            }
            return ServerConfig.isInValidBiome(player);
        }

        return true;
    }

    public static void speedUpTimers(String key, int spawnDelta, int noiseDelta) {
        Timer timer = TIMERS.get(key);
        LOG.debug("Speeding up timers for dimension [{}], timer [{}]", key, timer);
        if (timer != null) {
            timer.currentSpawn += spawnDelta;
            timer.currentNoise += noiseDelta;
        }
    }
}
