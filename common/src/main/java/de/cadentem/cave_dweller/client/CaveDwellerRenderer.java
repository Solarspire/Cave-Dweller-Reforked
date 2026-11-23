package de.cadentem.cave_dweller.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class CaveDwellerRenderer extends GeoEntityRenderer<CaveDwellerEntity, CaveDwellerRenderState> {
    public CaveDwellerRenderer(EntityRendererProvider.Context context) {
        super(context, new CaveDwellerModel());
        this.shadowRadius = 0.3f;
        withRenderLayer(new CaveDwellerEyesLayer(this));
    }

    @Override
    public CaveDwellerRenderState createRenderState(CaveDwellerEntity entity, Void unused) {
        return new CaveDwellerRenderState();
    }

    @Override
    public void addRenderData(CaveDwellerEntity entity, Void unused, CaveDwellerRenderState state, float partialTick) {
        boolean moving = state.getOrDefaultGeckolibData(DataTickets.IS_MOVING, false);
        state.addGeckolibData(CaveDwellerEntity.ANIMATION, entity.selectAnimation(moving));
    }

    @Override
    public void scaleModelForRender(CaveDwellerRenderState state, float width, float height,
                                    PoseStack poseStack, BakedGeoModel model, CameraRenderState cameraState) {
        float scale = state.isBaby ? 0.1f : 1.3f;
        super.scaleModelForRender(state, scale, scale, poseStack, model, cameraState);
    }
}
