package de.cadentem.cave_dweller.client;

import java.util.Map;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;

public final class CaveDwellerRenderState extends LivingEntityRenderState implements GeoRenderState {
    private final GeoRenderState data = new GeoRenderState.Impl();

    @Override
    public <D> void addGeckolibData(DataTicket<D> ticket, D value) {
        data.addGeckolibData(ticket, value);
    }

    @Override
    public boolean hasGeckolibData(DataTicket<?> ticket) {
        return data.hasGeckolibData(ticket);
    }

    @Override
    public <D> D getGeckolibData(DataTicket<D> ticket) {
        return data.getGeckolibData(ticket);
    }

    @Override
    public <D> D getOrDefaultGeckolibData(DataTicket<D> ticket, D fallback) {
        return data.getOrDefaultGeckolibData(ticket, fallback);
    }

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return data.getDataMap();
    }
}
