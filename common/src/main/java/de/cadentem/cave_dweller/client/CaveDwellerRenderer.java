package de.cadentem.cave_dweller.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.cadentem.cave_dweller.client.CaveDwellerEyesLayer;
import de.cadentem.cave_dweller.client.CaveDwellerModel;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.util.Utils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

public class CaveDwellerRenderer
extends GeoEntityRenderer<CaveDwellerEntity> {
    public CaveDwellerRenderer(EntityRendererProvider.Context context) {
        super(context, (GeoModel)new CaveDwellerModel());
        this.shadowRadius = 0.3f;
        this.addRenderLayer(new CaveDwellerEyesLayer((GeoRenderer<CaveDwellerEntity>)this));
    }

    @NotNull
    public ResourceLocation getTextureLocation(@NotNull CaveDwellerEntity instance) {
        return ResourceLocation.fromNamespaceAndPath("cave_dweller", "textures/entity/cave_dweller_texture" + Utils.getTextureAppend() + ".png");
    }

    public void render(CaveDwellerEntity entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        if (entity.isBaby()) {
            poseStack.scale(0.1f, 0.1f, 0.1f);
        } else {
            poseStack.scale(1.3f, 1.3f, 1.3f);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
