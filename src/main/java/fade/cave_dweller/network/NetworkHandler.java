package fade.cave_dweller.network;

import fade.cave_dweller.CaveDweller;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class NetworkHandler {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(CaveDweller.MODID);

    private NetworkHandler() {
    }

    public static void register() {
        CHANNEL.registerMessage(CaveSoundPacket.Handler.class, CaveSoundPacket.class, 0, Side.CLIENT);
    }
}
