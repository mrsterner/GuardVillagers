package dev.sterner.guardvillagers.mixin;

import dev.sterner.guardvillagers.common.event.GuardVillagersEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void onSpawnedEvent(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        GuardVillagersEvents.ON_SPAWNED_ENTITY_EVENT.invoker().onSpawned((ServerWorld) (Object) this, entity);
    }
}
