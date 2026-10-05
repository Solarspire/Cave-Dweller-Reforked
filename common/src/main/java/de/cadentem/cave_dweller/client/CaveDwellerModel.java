package de.cadentem.cave_dweller.client;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.util.Utils;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class CaveDwellerModel
extends GeoModel<CaveDwellerEntity> {
    public ResourceLocation getModelResource(CaveDwellerEntity ignored) {
        return ResourceLocation.fromNamespaceAndPath("cave_dweller", "geo/cave_dweller.geo" + Utils.getTextureAppend() + ".json");
    }

    public ResourceLocation getTextureResource(CaveDwellerEntity ignored) {
        return ResourceLocation.fromNamespaceAndPath("cave_dweller", "textures/entity/cave_dweller_texture" + Utils.getTextureAppend() + ".png");
    }

    public ResourceLocation getAnimationResource(CaveDwellerEntity ignored) {
        return ResourceLocation.fromNamespaceAndPath("cave_dweller", "animations/cave_dweller.animation.json");
    }

    public void setCustomAnimations(CaveDwellerEntity animatable, long instanceId, AnimationState<CaveDwellerEntity> animationState) {
        GeoBone head = this.getAnimationProcessor().getBone("head");
        if (head != null) {
            EntityModelData entityData = (EntityModelData)animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(entityData.headPitch() * ((float)Math.PI / 180));
            head.setRotY(entityData.netHeadYaw() * ((float)Math.PI / 180));
        }
        super.setCustomAnimations(animatable, instanceId, animationState);
    }
}
