package de.cadentem.cave_dweller.client;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.resources.Identifier;
public final class CaveDwellerModel extends GeoModel<CaveDwellerEntity> {
    public Identifier getModelResource(GeoRenderState state) { return id("cave_dweller"); }
    public Identifier getTextureResource(GeoRenderState state) { return id("textures/entity/cave_dweller_texture.png"); }
    public Identifier getAnimationResource(CaveDwellerEntity entity) { return id("cave_dweller"); }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("cave_dweller", path); }
}
