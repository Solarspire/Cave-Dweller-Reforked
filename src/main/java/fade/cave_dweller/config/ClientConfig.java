package fade.cave_dweller.config;

import fade.cave_dweller.CaveDweller;
import net.minecraftforge.common.config.Config;

@Config(modid = CaveDweller.MODID, name = CaveDweller.MODID + "/client")
public class ClientConfig {
    @Config.Comment("Use updated textures by the user '...'.")
    public static boolean useUpdatedTextures = true;
}
