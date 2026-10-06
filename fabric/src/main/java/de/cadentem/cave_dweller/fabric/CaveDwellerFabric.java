package de.cadentem.cave_dweller.fabric;
import de.cadentem.cave_dweller.CaveDweller;
import de.cadentem.cave_dweller.config.ServerConfig;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.registry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
public final class CaveDwellerFabric implements ModInitializer {
    public void onInitialize() {
        ServerConfig.load(FabricLoader.getInstance().getConfigDir());
        var type = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("cave_dweller"),
                EntityType.Builder.of(CaveDwellerEntity::new, MobCategory.MONSTER).sized(0.5f, 2.7f).build("cave_dweller:cave_dweller"));
        ModEntityTypes.CAVE_DWELLER = () -> type;
        FabricDefaultAttributeRegistry.register(type, CaveDwellerEntity.getAttributeBuilder());
        Item egg = Registry.register(BuiltInRegistries.ITEM, id("cave_dweller_spawn_egg"),
                new SpawnEggItem(type, 0xC4C4C4, 0x0C0F3D, new Item.Properties()));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(egg));
        ModSounds.CAVENOISE_1 = registerSound("cavenoise_1");
        ModSounds.CAVENOISE_2 = registerSound("cavenoise_2");
        ModSounds.CAVENOISE_3 = registerSound("cavenoise_3");
        ModSounds.CAVENOISE_4 = registerSound("cavenoise_4");
        ModSounds.CHASE_STEP_1 = registerSound("chase_step_1");
        ModSounds.CHASE_STEP_2 = registerSound("chase_step_2");
        ModSounds.CHASE_STEP_3 = registerSound("chase_step_3");
        ModSounds.CHASE_STEP_4 = registerSound("chase_step_4");
        ModSounds.CHASE_1 = registerSound("chase_1");
        ModSounds.CHASE_2 = registerSound("chase_2");
        ModSounds.CHASE_3 = registerSound("chase_3");
        ModSounds.CHASE_4 = registerSound("chase_4");
        ModSounds.FLEE_1 = registerSound("flee_1");
        ModSounds.FLEE_2 = registerSound("flee_2");
        ModSounds.SPOTTED = registerSound("spotted");
        ModSounds.DISAPPEAR = registerSound("disappear");
        ModSounds.DWELLER_HURT_1 = registerSound("dweller_hurt_1");
        ModSounds.DWELLER_HURT_2 = registerSound("dweller_hurt_2");
        ModSounds.DWELLER_HURT_3 = registerSound("dweller_hurt_3");
        ModSounds.DWELLER_HURT_4 = registerSound("dweller_hurt_4");
        ModSounds.DWELLER_DEATH = registerSound("dweller_death");
        ServerTickEvents.START_SERVER_TICK.register(CaveDweller.INSTANCE::serverTick);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> CaveDweller.resetTimers());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CaveDweller.resetTimers());
    }
    private static java.util.function.Supplier<SoundEvent> registerSound(String name) {
        SoundEvent sound = Registry.register(BuiltInRegistries.SOUND_EVENT, id(name), SoundEvent.createVariableRangeEvent(id(name)));
        return () -> sound;
    }
    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("cave_dweller", name); }
}
