package de.cadentem.cave_dweller.client;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
public final class CaveDwellerEyesLayer extends AutoGlowingGeoLayer<CaveDwellerEntity, Void, CaveDwellerRenderState> {
    public CaveDwellerEyesLayer(CaveDwellerRenderer renderer) { super(renderer); }
    // The reference renders full-bright eyes even during the invisible encounter phase.
    @Override protected RenderType getRenderType(CaveDwellerRenderState state) {
        return RenderTypes.entityCutout(getTextureResource(state));
    }
    @Override protected int getBrightness(CaveDwellerRenderState state) { return net.minecraft.util.LightCoordsUtil.FULL_BRIGHT; }
    @Override protected Identifier getTextureResource(CaveDwellerRenderState state) {
        return Identifier.fromNamespaceAndPath("cave_dweller", "textures/entity/cave_dweller_eyes_texture.png");
    }
}
