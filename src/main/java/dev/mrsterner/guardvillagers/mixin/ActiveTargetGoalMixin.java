package dev.mrsterner.guardvillagers.mixin;

import dev.mrsterner.guardvillagers.GuardVillagersConfig;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import dev.mrsterner.guardvillagers.common.events.GuardVillagersEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ActiveTargetGoal.class)
public abstract class ActiveTargetGoalMixin<T extends LivingEntity> extends TrackTargetGoal {

    @Shadow @Nullable protected LivingEntity targetEntity;

    @Shadow @Final protected int reciprocalChance;

    @Shadow protected abstract void findClosestTarget();

    public ActiveTargetGoalMixin(MobEntity mob, boolean checkVisibility) {
        super(mob, checkVisibility);
    }

    @Inject(method = "start", at = @At("HEAD"))
    private void targetEvent(CallbackInfo ci){
        GuardVillagersEvents.ON_TARGET_EVENT.invoker().onTarget(this.mob, this.targetEntity);
        if (target == null || this.mob instanceof GuardEntity)
            return;
        boolean isVillager = target.getType() == EntityType.VILLAGER || target instanceof GuardEntity;
        if (isVillager) {
            List<MobEntity> list = this.mob.world.getNonSpectatingEntities(MobEntity.class, this.mob.getBoundingBox()
            .expand(GuardVillagersConfig.get().GuardVillagerHelpRange, 5.0D, GuardVillagersConfig.get().GuardVillagerHelpRange));
            for (MobEntity mobEntity : list) {
                if ((mobEntity instanceof GuardEntity || mob.getType() == EntityType.IRON_GOLEM)
                && mobEntity.getTarget() == null) {
                    mobEntity.setTarget(this.mob);
                }
            }
        }

        if (this.mob instanceof IronGolemEntity golem && target instanceof GuardEntity)
            golem.setTarget(null);

    }
}
