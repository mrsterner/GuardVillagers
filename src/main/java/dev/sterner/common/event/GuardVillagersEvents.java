package dev.sterner.common.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

public class GuardVillagersEvents {

    public static final Event<OnTarget> ON_TARGET_EVENT = createArrayBacked(OnTarget.class, listeners -> (entity, target) -> {
        for (OnTarget listener : listeners) {
            listener.onTarget(entity, target);
        }
    });

    @FunctionalInterface
    public interface OnTarget {
        void onTarget(LivingEntity entity, LivingEntity target);
    }

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
