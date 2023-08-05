package dev.sterner.guardvillagers.common.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

public class GuardVillagersEvents {

    public static final Event<OnSpawned> ON_SPAWNED_ENTITY_EVENT = createArrayBacked(OnSpawned.class, listeners -> (serverWorld, entity) -> {
        for (OnSpawned listener : listeners) {
            listener.onSpawned(serverWorld, entity);
        }
    });

    @FunctionalInterface
    public interface OnSpawned {
        void onSpawned(ServerWorld serverWorld, Entity entity);
    }
}
