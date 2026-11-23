package de.cadentem.cave_dweller.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class CaveDwellerEyesLayer extends GeoRenderLayer<CaveDwellerEntity, Void, CaveDwellerRenderState> {
    private static final RenderType EYES = RenderType.entityCutoutNoCull(
            ResourceLocation.fromNamespaceAndPath("cave_dweller", "textures/entity/cave_dweller_eyes_texture.png"));

    public CaveDwellerEyesLayer(CaveDwellerRenderer renderer) {
        super(renderer);
    }

    @Override
    public void render(CaveDwellerRenderState state, PoseStack poseStack, BakedGeoModel model, RenderType renderType,
                       MultiBufferSource buffers, VertexConsumer bodyBuffer, int light, int overlay, int color) {
        // The body buffer is absent during the invisible encounter phase.
        getRenderer().reRender(state, poseStack, model, buffers, EYES, buffers.getBuffer(EYES),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
