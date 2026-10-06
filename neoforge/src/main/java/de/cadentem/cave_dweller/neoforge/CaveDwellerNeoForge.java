package de.cadentem.cave_dweller.neoforge;
import de.cadentem.cave_dweller.CaveDweller;
import de.cadentem.cave_dweller.config.ServerConfig;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.registry.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
@Mod("cave_dweller")
public final class CaveDwellerNeoForge {
    public CaveDwellerNeoForge(IEventBus bus) {
        ServerConfig.load(FMLPaths.CONFIGDIR.get());
        var entities = DeferredRegister.create(Registries.ENTITY_TYPE, "cave_dweller");
        ModEntityTypes.CAVE_DWELLER = entities.register("cave_dweller", () ->
                EntityType.Builder.of(CaveDwellerEntity::new, MobCategory.MONSTER).sized(0.5f, 2.7f).build("cave_dweller:cave_dweller"));
        var items = DeferredRegister.createItems("cave_dweller");
        var egg = items.register("cave_dweller_spawn_egg", () ->
                new DeferredSpawnEggItem(ModEntityTypes.CAVE_DWELLER, 0xC4C4C4, 0x0C0F3D, new Item.Properties()));
        var sounds = DeferredRegister.create(Registries.SOUND_EVENT, "cave_dweller");
        ModSounds.CAVENOISE_1 = sounds.register("cavenoise_1", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "cavenoise_1")));
        ModSounds.CAVENOISE_2 = sounds.register("cavenoise_2", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "cavenoise_2")));
        ModSounds.CAVENOISE_3 = sounds.register("cavenoise_3", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "cavenoise_3")));
        ModSounds.CAVENOISE_4 = sounds.register("cavenoise_4", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "cavenoise_4")));
        ModSounds.CHASE_STEP_1 = sounds.register("chase_step_1", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "chase_step_1")));
        ModSounds.CHASE_STEP_2 = sounds.register("chase_step_2", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "chase_step_2")));
        ModSounds.CHASE_STEP_3 = sounds.register("chase_step_3", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "chase_step_3")));
        ModSounds.CHASE_STEP_4 = sounds.register("chase_step_4", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "chase_step_4")));
        ModSounds.CHASE_1 = sounds.register("chase_1", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "chase_1")));
        ModSounds.CHASE_2 = sounds.register("chase_2", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "chase_2")));
        ModSounds.CHASE_3 = sounds.register("chase_3", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "chase_3")));
        ModSounds.CHASE_4 = sounds.register("chase_4", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "chase_4")));
        ModSounds.FLEE_1 = sounds.register("flee_1", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "flee_1")));
        ModSounds.FLEE_2 = sounds.register("flee_2", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "flee_2")));
        ModSounds.SPOTTED = sounds.register("spotted", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "spotted")));
        ModSounds.DISAPPEAR = sounds.register("disappear", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "disappear")));
        ModSounds.DWELLER_HURT_1 = sounds.register("dweller_hurt_1", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "dweller_hurt_1")));
        ModSounds.DWELLER_HURT_2 = sounds.register("dweller_hurt_2", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "dweller_hurt_2")));
        ModSounds.DWELLER_HURT_3 = sounds.register("dweller_hurt_3", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "dweller_hurt_3")));
        ModSounds.DWELLER_HURT_4 = sounds.register("dweller_hurt_4", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "dweller_hurt_4")));
        ModSounds.DWELLER_DEATH = sounds.register("dweller_death", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cave_dweller", "dweller_death")));
        entities.register(bus); items.register(bus); sounds.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(ModEntityTypes.CAVE_DWELLER.get(), CaveDwellerEntity.getAttributeBuilder()));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(egg.get());
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) -> CaveDweller.INSTANCE.serverTick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> CaveDweller.resetTimers());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> CaveDweller.resetTimers());
    }
}
