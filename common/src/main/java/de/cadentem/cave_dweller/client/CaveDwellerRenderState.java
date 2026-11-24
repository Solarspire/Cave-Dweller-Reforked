package de.cadentem.cave_dweller.client;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.constant.dataticket.OverridingDataTicket;
import software.bernie.geckolib.renderer.base.GeoRenderState;

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
    @SuppressWarnings("unchecked")
    public <D> D getGeckolibData(DataTicket<D> ticket) {
        D value = data.getGeckolibData(ticket);
        // Overriding tickets (e.g. ENTITY_BODY_YAW) must be resolved against this state, not the delegate
        if (value == null && ticket instanceof OverridingDataTicket<?, ?> overriding && overriding.canExtractFrom(this))
            return (D) extract(overriding);
        return value;
    }

    @Override
    public <D> D getOrDefaultGeckolibData(DataTicket<D> ticket, D fallback) {
        D value = getGeckolibData(ticket);
        return value != null ? value : fallback;
    }

    @Override
    public <D> D getOrDefaultGeckolibData(DataTicket<D> ticket, Supplier<D> fallback) {
        D value = getGeckolibData(ticket);
        return value != null ? value : fallback.get();
    }

    private <C> Object extract(OverridingDataTicket<?, C> ticket) {
        return ticket.extractFrom(ticket.getOverriddenClass().cast(this));
    }

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return data.getDataMap();
    }
}
