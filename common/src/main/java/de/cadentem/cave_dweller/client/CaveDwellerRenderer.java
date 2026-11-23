package de.cadentem.cave_dweller.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class CaveDwellerRenderer extends GeoEntityRenderer<CaveDwellerEntity, CaveDwellerRenderState> {
    public CaveDwellerRenderer(EntityRendererProvider.Context context) {
        super(context, new CaveDwellerModel());
        this.shadowRadius = 0.3f;
        addRenderLayer(new CaveDwellerEyesLayer(this));
    }

    @Override
    protected CaveDwellerRenderState createBaseRenderState(CaveDwellerEntity entity) {
        return new CaveDwellerRenderState();
    }

    @Override
    public void addRenderData(CaveDwellerEntity entity, Void unused, CaveDwellerRenderState state) {
        boolean moving = state.getOrDefaultGeckolibData(DataTickets.IS_MOVING, false);
        state.addGeckolibData(CaveDwellerEntity.ANIMATION, entity.selectAnimation(moving));
    }

    @Override
    public void scaleModelForRender(CaveDwellerRenderState state, float width, float height,
                                    PoseStack poseStack, BakedGeoModel model, boolean isReRender) {
        float scale = state.isBaby ? 0.1f : 1.3f;
        super.scaleModelForRender(state, scale, scale, poseStack, model, isReRender);
    }
}
