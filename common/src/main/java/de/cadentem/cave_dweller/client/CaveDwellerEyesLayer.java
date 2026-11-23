package de.cadentem.cave_dweller.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.util.Utils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class CaveDwellerEyesLayer
extends GeoRenderLayer<CaveDwellerEntity> {
    public static ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("cave_dweller", "textures/entity/cave_dweller_eyes_texture" + Utils.getTextureAppend() + ".png");

    public CaveDwellerEyesLayer(GeoRenderer<CaveDwellerEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, CaveDwellerEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, int renderColor) {
        RenderType eyesRenderType = RenderType.entityCutoutNoCull(TEXTURE);
        this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, eyesRenderType, bufferSource.getBuffer(eyesRenderType), partialTick, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
