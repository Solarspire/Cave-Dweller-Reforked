package fade.cave_dweller.client;

import fade.cave_dweller.CaveDweller;
import fade.cave_dweller.entities.CaveDwellerEntity;
import fade.cave_dweller.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.renderers.geo.GeoLayerRenderer;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;

public class CaveDwellerEyesLayer extends GeoLayerRenderer<CaveDwellerEntity> {
    public CaveDwellerEyesLayer(IGeoRenderer<CaveDwellerEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(
        CaveDwellerEntity entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        Color color
    ) {
        ResourceLocation texture = new ResourceLocation(
            CaveDweller.MODID,
            "textures/entity/cave_dweller_eyes_texture" + Utils.getTextureAppend() + ".png"
        );

        Minecraft.getMinecraft().renderEngine.bindTexture(texture);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        GlStateManager.disableLighting();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        if (this.getRenderer() instanceof CaveDwellerRenderer) {
            CaveDwellerRenderer renderer = (CaveDwellerRenderer) this.getRenderer();
            renderer.setRenderingEyesPass(true);
            try {
                renderer.render(
                    this.getEntityModel().getModel(this.getEntityModel().getModelLocation(entity)),
                    entity,
                    partialTicks,
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
                );
            } finally {
                renderer.setRenderingEyesPass(false);
            }
        } else {
            this.getRenderer().render(
                this.getEntityModel().getModel(this.getEntityModel().getModelLocation(entity)),
                entity,
                partialTicks,
                1.0F,
                1.0F,
                1.0F,
                1.0F
            );
        }

        int packedLight = entity.getBrightnessForRender();
        int lightX = packedLight % 65536;
        int lightY = packedLight / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
