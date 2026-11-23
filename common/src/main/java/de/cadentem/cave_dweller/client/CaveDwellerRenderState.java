package de.cadentem.cave_dweller.client;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public final class CaveDwellerRenderState extends LivingEntityRenderState implements GeoRenderState {
    private final Map<DataTicket<?>, Object> data = new IdentityHashMap<>();

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return data;
    }
}
