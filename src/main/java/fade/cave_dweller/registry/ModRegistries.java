package fade.cave_dweller.registry;

import fade.cave_dweller.CaveDweller;
import fade.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = CaveDweller.MODID)
public final class ModRegistries {
    private ModRegistries() {
    }

    @SubscribeEvent
    public static void onRegisterSounds(RegistryEvent.Register<SoundEvent> event) {
        ModSounds.register(event);
    }

    @SubscribeEvent
    public static void onRegisterEntities(RegistryEvent.Register<EntityEntry> event) {
        ResourceLocation id = new ResourceLocation(CaveDweller.MODID, "cave_dweller");
        EntityEntryBuilder<?> builder = EntityEntryBuilder.create()
            .entity(CaveDwellerEntity.class)
            .id(id, CaveDweller.CAVE_DWELLER_ENTITY_ID)
            .name("cave_dweller")
            .tracker(64, 3, true);
        if (!EntityList.ENTITY_EGGS.containsKey(id)) {
            builder = builder.egg(12895428, 790333);
        }
        EntityEntry entry = builder.build();
        event.getRegistry().register(entry);
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!CaveDweller.MODID.equals(event.getModID())) {
            return;
        }
        ConfigManager.sync(CaveDweller.MODID, Config.Type.INSTANCE);
        CaveDweller.RELOAD_ALL = true;
        CaveDweller.LOG.info("Cave Dweller configs reloaded");
    }
}
