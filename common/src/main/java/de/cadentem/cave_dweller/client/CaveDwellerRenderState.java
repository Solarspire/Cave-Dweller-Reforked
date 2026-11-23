package de.cadentem.cave_dweller.client;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import java.util.HashMap;
import java.util.Map;

public final class CaveDwellerRenderState extends LivingEntityRenderState implements GeoRenderState {
    private final Map<DataTicket<?>, Object> data = new HashMap<>();

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return data;
    }
}
