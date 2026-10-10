package fade.cave_dweller.client;

import fade.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class CaveDwellerRenderer extends GeoEntityRenderer<CaveDwellerEntity> {
    private boolean renderingEyesPass;

    public CaveDwellerRenderer(RenderManager renderManager) {
        super(renderManager, new CaveDwellerModel());
        this.shadowSize = 0.3F;
        this.addLayer(new CaveDwellerEyesLayer(this));
    }

    @Override
    public void renderEarly(CaveDwellerEntity entity, float ticks, float red, float green, float blue, float partialTicks) {
        if (!this.renderingEyesPass) {
            if (entity.isChild()) {
                GlStateManager.scale(0.1F, 0.1F, 0.1F);
            } else {
                GlStateManager.scale(1.3F, 1.3F, 1.3F);
            }
        }
        super.renderEarly(entity, ticks, red, green, blue, partialTicks);
    }

    public void setRenderingEyesPass(boolean renderingEyesPass) {
        this.renderingEyesPass = renderingEyesPass;
    }
}
