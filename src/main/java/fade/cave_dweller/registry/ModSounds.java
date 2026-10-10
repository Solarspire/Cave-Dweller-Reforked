package fade.cave_dweller.registry;

import fade.cave_dweller.CaveDweller;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;

public final class ModSounds {
    public static SoundEvent CAVENOISE_1;
    public static SoundEvent CAVENOISE_2;
    public static SoundEvent CAVENOISE_3;
    public static SoundEvent CAVENOISE_4;
    public static SoundEvent CHASE_STEP_1;
    public static SoundEvent CHASE_STEP_2;
    public static SoundEvent CHASE_STEP_3;
    public static SoundEvent CHASE_STEP_4;
    public static SoundEvent CHASE_1;
    public static SoundEvent CHASE_2;
    public static SoundEvent CHASE_3;
    public static SoundEvent CHASE_4;
    public static SoundEvent FLEE_1;
    public static SoundEvent FLEE_2;
    public static SoundEvent SPOTTED;
    public static SoundEvent DISAPPEAR;
    public static SoundEvent DWELLER_HURT_1;
    public static SoundEvent DWELLER_HURT_2;
    public static SoundEvent DWELLER_HURT_3;
    public static SoundEvent DWELLER_HURT_4;
    public static SoundEvent DWELLER_DEATH;

    private ModSounds() {
    }

    public static void register(RegistryEvent.Register<SoundEvent> event) {
        CAVENOISE_1 = register(event, "cavenoise_1");
        CAVENOISE_2 = register(event, "cavenoise_2");
        CAVENOISE_3 = register(event, "cavenoise_3");
        CAVENOISE_4 = register(event, "cavenoise_4");
        CHASE_STEP_1 = register(event, "chase_step_1");
        CHASE_STEP_2 = register(event, "chase_step_2");
        CHASE_STEP_3 = register(event, "chase_step_3");
        CHASE_STEP_4 = register(event, "chase_step_4");
        CHASE_1 = register(event, "chase_1");
        CHASE_2 = register(event, "chase_2");
        CHASE_3 = register(event, "chase_3");
        CHASE_4 = register(event, "chase_4");
        FLEE_1 = register(event, "flee_1");
        FLEE_2 = register(event, "flee_2");
        SPOTTED = register(event, "spotted");
        DISAPPEAR = register(event, "disappear");
        DWELLER_HURT_1 = register(event, "dweller_hurt_1");
        DWELLER_HURT_2 = register(event, "dweller_hurt_2");
        DWELLER_HURT_3 = register(event, "dweller_hurt_3");
        DWELLER_HURT_4 = register(event, "dweller_hurt_4");
        DWELLER_DEATH = register(event, "dweller_death");
    }

    private static SoundEvent register(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation id = new ResourceLocation(CaveDweller.MODID, name);
        SoundEvent sound = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }
}
