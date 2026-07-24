package fade.cave_dweller.client;

import fade.cave_dweller.CaveDweller;
import fade.cave_dweller.entities.CaveDwellerEntity;
import fade.cave_dweller.util.Utils;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

public class CaveDwellerModel extends AnimatedGeoModel<CaveDwellerEntity> {
    @Override
    public ResourceLocation getModelLocation(CaveDwellerEntity ignored) {
        return new ResourceLocation(CaveDweller.MODID, "geo/cave_dweller.geo" + Utils.getTextureAppend() + ".json");
    }

    @Override
    public ResourceLocation getTextureLocation(CaveDwellerEntity ignored) {
        return new ResourceLocation(CaveDweller.MODID, "textures/entity/cave_dweller_texture" + Utils.getTextureAppend() + ".png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(CaveDwellerEntity ignored) {
        return new ResourceLocation(CaveDweller.MODID, "animations/cave_dweller.animation.json");
    }

    @Override
    public void setLivingAnimations(CaveDwellerEntity animatable, Integer instanceId, AnimationEvent animationEvent) {
        IBone head = this.getAnimationProcessor().getBone("head");
        if (head != null && !animationEvent.getExtraDataOfType(EntityModelData.class).isEmpty()) {
            EntityModelData entityData = (EntityModelData) animationEvent.getExtraDataOfType(EntityModelData.class).get(0);
            head.setRotationX(entityData.headPitch * ((float) Math.PI / 180F));
            head.setRotationY(entityData.netHeadYaw * ((float) Math.PI / 180F));
        }
        super.setLivingAnimations(animatable, instanceId, animationEvent);
    }
}
