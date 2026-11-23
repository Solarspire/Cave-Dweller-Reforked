package de.cadentem.cave_dweller.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CaveDwellerRenderer extends GeoEntityRenderer<CaveDwellerEntity> {
    public CaveDwellerRenderer(EntityRendererProvider.Context context) {
        super(context, new CaveDwellerModel());
        this.shadowRadius = 0.3f;
        this.addRenderLayer(new CaveDwellerEyesLayer(this));
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, CaveDwellerEntity entity,
                                    BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        float scale = entity.isBaby() ? 0.1f : 1.3f;
        super.scaleModelForRender(scale, scale, poseStack, entity, model, isReRender, partialTick, packedLight, packedOverlay);
    }
}
