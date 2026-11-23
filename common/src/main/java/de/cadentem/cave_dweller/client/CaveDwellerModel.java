package de.cadentem.cave_dweller.client;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.processing.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
public final class CaveDwellerModel extends GeoModel<CaveDwellerEntity> {
    public ResourceLocation getModelResource(GeoRenderState state) { return id("cave_dweller"); }
    public ResourceLocation getTextureResource(GeoRenderState state) { return id("textures/entity/cave_dweller_texture.png"); }
    public ResourceLocation getAnimationResource(CaveDwellerEntity entity) { return id("cave_dweller"); }
    @Override
    public void setCustomAnimations(AnimationState<CaveDwellerEntity> state) {
        var head = getBone("head");
        head.ifPresent(bone -> {
            bone.setRotX(state.getDataOrDefault(DataTickets.ENTITY_PITCH, 0f) * Mth.DEG_TO_RAD);
            bone.setRotY(state.getDataOrDefault(DataTickets.ENTITY_YAW, 0f) * Mth.DEG_TO_RAD);
        });
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("cave_dweller", path); }
}
