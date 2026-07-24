package fade.cave_dweller.client;

import fade.cave_dweller.CaveDweller;
import fade.cave_dweller.network.CaveSoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

@SideOnly(Side.CLIENT)
public final class HandleCaveSound {
    private HandleCaveSound() {
    }

    public static void handle(CaveSoundPacket packet) {
        SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(packet.soundResource);
        if (soundEvent == null) {
            CaveDweller.LOG.error("Sound Event [{}] was null while handling packet", packet.soundResource);
            return;
        }

        ISound sound = new PositionedSoundRecord(
            packet.soundResource,
            SoundCategory.AMBIENT,
            packet.volume,
            packet.pitch,
            false,
            0,
            ISound.AttenuationType.LINEAR,
            packet.playerPosition.getX(),
            packet.playerPosition.getY(),
            packet.playerPosition.getZ()
        );
        Minecraft.getMinecraft().getSoundHandler().playSound(sound);
    }
}
