package fade.cave_dweller.network;

import fade.cave_dweller.client.HandleCaveSound;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class CaveSoundPacket implements IMessage {
    public ResourceLocation soundResource;
    public BlockPos playerPosition;
    public float volume;
    public float pitch;

    public CaveSoundPacket() {
    }

    public CaveSoundPacket(ResourceLocation soundResource, BlockPos playerPosition, float volume, float pitch) {
        this.soundResource = soundResource;
        this.playerPosition = playerPosition;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeResourceLocation(this.soundResource);
        buffer.writeBlockPos(this.playerPosition);
        buffer.writeFloat(this.volume);
        buffer.writeFloat(this.pitch);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        this.soundResource = buffer.readResourceLocation();
        this.playerPosition = buffer.readBlockPos();
        this.volume = buffer.readFloat();
        this.pitch = buffer.readFloat();
    }

    public static class Handler implements IMessageHandler<CaveSoundPacket, IMessage> {
        @Override
        public IMessage onMessage(final CaveSoundPacket message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    HandleCaveSound.handle(message);
                }
            });
            return null;
        }
    }
}
