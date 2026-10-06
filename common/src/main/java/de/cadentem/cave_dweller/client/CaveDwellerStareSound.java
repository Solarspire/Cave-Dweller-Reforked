package de.cadentem.cave_dweller.client;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.registry.ModSounds;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** One cancellable, positional tension track per dweller. Used only by clients. */
public final class CaveDwellerStareSound extends AbstractTickableSoundInstance {
    private static final Map<CaveDwellerEntity, CaveDwellerStareSound> ACTIVE = new WeakHashMap<>();
    private final WeakReference<CaveDwellerEntity> owner;

    private CaveDwellerStareSound(CaveDwellerEntity entity) {
        super(ModSounds.SPOTTED.get(), SoundSource.HOSTILE, RandomSource.create());
        this.owner = new WeakReference<>(entity);
        this.volume = 3.0f;
        this.pitch = 1.0f;
        this.updatePosition(entity);
    }

    public static void setPlaying(CaveDwellerEntity entity, boolean playing) {
        CaveDwellerStareSound previous = ACTIVE.remove(entity);
        if (previous != null) previous.cancel();
        if (playing && entity.isStareSoundPlaying()) {
            CaveDwellerStareSound sound = new CaveDwellerStareSound(entity);
            ACTIVE.put(entity, sound);
            Minecraft.getInstance().getSoundManager().play(sound);
        }
    }

    private void cancel() {
        this.stop();
        Minecraft.getInstance().getSoundManager().stop(this);
    }

    @Override
    public void tick() {
        CaveDwellerEntity entity = this.owner.get();
        if (entity == null || !entity.isStareSoundPlaying()) {
            if (entity != null) ACTIVE.remove(entity, this);
            this.cancel();
        } else {
            this.updatePosition(entity);
        }
    }

    private void updatePosition(CaveDwellerEntity entity) {
        this.x = entity.getX();
        this.y = entity.getY();
        this.z = entity.getZ();
    }
}
