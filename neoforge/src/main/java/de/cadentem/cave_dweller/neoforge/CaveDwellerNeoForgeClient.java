package de.cadentem.cave_dweller.neoforge;
import de.cadentem.cave_dweller.client.CaveDwellerRenderer;
import de.cadentem.cave_dweller.registry.ModEntityTypes;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid = "cave_dweller", value = Dist.CLIENT)
public final class CaveDwellerNeoForgeClient {
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.CAVE_DWELLER.get(), CaveDwellerRenderer::new);
    }
}
