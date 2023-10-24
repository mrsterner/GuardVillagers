package dev.sterner.guardvillagers.mixin;

import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import io.github.fabricators_of_create.porting_lib.entity.events.living.LivingEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Targeter;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity implements Targeter {
    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "setTarget", at = @At("TAIL"))
    private void onSetTarget(@Nullable LivingEntity target, CallbackInfo ci) {
        if (target == null || ((MobEntity)(Object)this) instanceof GuardEntity) {
            return;
        }
        boolean isVillager = target.getType() == EntityType.VILLAGER || target instanceof GuardEntity;
        if (isVillager) {
            List<MobEntity> list = ((MobEntity)(Object)this).getWorld().getNonSpectatingEntities(MobEntity.class, ((MobEntity)(Object)this).getBoundingBox().expand(GuardVillagersConfig.guardVillagerHelpRange, 5.0D, GuardVillagersConfig.guardVillagerHelpRange));
            for (MobEntity mobEntity : list) {
                if ((mobEntity instanceof GuardEntity || ((MobEntity)(Object)this).getType() == EntityType.IRON_GOLEM) && mobEntity.getTarget() == null) {
                    mobEntity.setTarget(((MobEntity)(Object)this));
                }
            }
        }

        if (((MobEntity)(Object)this) instanceof IronGolemEntity golem && target instanceof GuardEntity) {
            golem.setTarget(null);
        }
    }
}
