package dev.mrsterner.guardvillagers.common.events;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

public final class GuardVillagersEvents {
    private GuardVillagersEvents(){}

    public static final Event<OnTarget> ON_TARGET_EVENT = createArrayBacked(OnTarget.class, listeners -> (entity, target) -> {
        for (OnTarget listener : listeners) {
            listener.onTarget(entity, target);
        }
    });
    @FunctionalInterface
    public interface OnTarget {
        void onTarget(LivingEntity entity, LivingEntity target);
    }

    public static final Event<OnDamageTaken> ON_DAMAGE_TAKEN_EVENT = createArrayBacked(OnDamageTaken.class, listeners -> (entity, damageSource) -> {
        for (OnDamageTaken listener : listeners) {
            listener.onDamageTaken(entity, damageSource);
        }
    });
    @FunctionalInterface
    public interface OnDamageTaken {
        void onDamageTaken(LivingEntity entity, DamageSource damageSource);
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

    public static final Event<OnConsumed> ON_CONSUMED_EVENT = createArrayBacked(OnConsumed.class, listeners -> (livingEntity, itemStack, i, finish) -> {
        for (OnConsumed listener : listeners) {
            listener.onConsumed(livingEntity, itemStack, i , finish);
        }
        return finish;
    });
    @FunctionalInterface
    public interface OnConsumed {
        ItemStack onConsumed(LivingEntity livingEntity, ItemStack itemStack, int i, ItemStack finish);
    }
}