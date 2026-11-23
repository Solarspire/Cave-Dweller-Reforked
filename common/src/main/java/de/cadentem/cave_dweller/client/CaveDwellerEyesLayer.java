package de.cadentem.cave_dweller.client;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
public final class CaveDwellerEyesLayer extends AutoGlowingGeoLayer<CaveDwellerEntity, Void, CaveDwellerRenderState> {
    public CaveDwellerEyesLayer(CaveDwellerRenderer renderer) { super(renderer); }
    // The reference renders full-bright eyes even during the invisible encounter phase.
    @Override protected RenderType getRenderType(CaveDwellerRenderState state) {
        return RenderType.entityCutout(getTextureResource(state));
    }
    @Override protected int getBrightness(CaveDwellerRenderState state) { return net.minecraft.client.renderer.LightTexture.FULL_BRIGHT; }
    @Override protected ResourceLocation getTextureResource(CaveDwellerRenderState state) {
        return ResourceLocation.fromNamespaceAndPath("cave_dweller", "textures/entity/cave_dweller_eyes_texture.png");
    }
}
