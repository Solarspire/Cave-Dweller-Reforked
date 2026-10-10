package fade.cave_dweller.proxy;

import fade.cave_dweller.client.CaveDwellerRenderer;
import fade.cave_dweller.entities.CaveDwellerEntity;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(CaveDwellerEntity.class, CaveDwellerRenderer::new);
    }
}
