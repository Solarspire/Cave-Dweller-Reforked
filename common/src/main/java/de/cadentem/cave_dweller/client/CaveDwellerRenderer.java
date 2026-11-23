package de.cadentem.cave_dweller.client;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.BoneSnapshots;
import software.bernie.geckolib.renderer.base.RenderPassInfo;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
public final class CaveDwellerRenderer extends GeoEntityRenderer<CaveDwellerEntity, CaveDwellerRenderState> {
    public CaveDwellerRenderer(EntityRendererProvider.Context context) {
        super(context, new CaveDwellerModel());
        this.shadowRadius = 0.3f;
        withRenderLayer(new CaveDwellerEyesLayer(this));
    }
    @Override public CaveDwellerRenderState createRenderState(CaveDwellerEntity entity, Void unused) {
        return new CaveDwellerRenderState();
    }
    @Override public void addRenderData(CaveDwellerEntity entity, Void unused, CaveDwellerRenderState state, float partialTick) {
        state.addGeckolibData(CaveDwellerEntity.ANIMATION, entity.selectAnimation(state.getOrDefaultGeckolibData(DataTickets.IS_MOVING, false)));
    }
    @Override public void scaleModelForRender(RenderPassInfo<CaveDwellerRenderState> pass, float width, float height) {
        float scale = pass.renderState().isBaby ? 0.1f : 1.3f;
        super.scaleModelForRender(pass, scale, scale);
    }
    @Override public void adjustModelBonesForRender(RenderPassInfo<CaveDwellerRenderState> pass, BoneSnapshots snapshots) {
        DefaultAnimations.hardcodedHeadRotation(pass, snapshots, "head");
    }
}
