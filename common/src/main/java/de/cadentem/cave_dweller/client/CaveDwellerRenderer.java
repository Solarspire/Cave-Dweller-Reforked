package de.cadentem.cave_dweller.client;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
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
