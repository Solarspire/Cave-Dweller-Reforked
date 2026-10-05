package de.cadentem.cave_dweller.fabric;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import de.cadentem.cave_dweller.client.CaveDwellerRenderer;
import de.cadentem.cave_dweller.registry.ModEntityTypes;
public final class CaveDwellerFabricClient implements ClientModInitializer {
    public void onInitializeClient() { EntityRendererRegistry.register(ModEntityTypes.CAVE_DWELLER.get(), CaveDwellerRenderer::new); }
}
