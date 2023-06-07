package dev.mrsterner.guardvillagers.mixin;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.GuardVillagersConfig;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import dev.mrsterner.guardvillagers.common.events.GuardVillagersEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }


    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;getAttacker()Lnet/minecraft/entity/Entity;"), cancellable = true)
    private void onDamageTakenEvent(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
        Entity entity2 = source.getAttacker();
        if(entity2 != null){
            GuardVillagersEvents.ON_DAMAGE_TAKEN_EVENT.invoker().onDamageTaken((LivingEntity) (Object) this, source);
            LivingEntity entity = (LivingEntity) (Object) this;
            if (entity == null)
                return;
            boolean isVillager = entity.getType() == EntityType.VILLAGER || entity instanceof GuardEntity;
            boolean isGolem = isVillager || entity.getType() == EntityType.IRON_GOLEM;
            if (isGolem && entity2 instanceof GuardEntity && !GuardVillagers.config.guardArrowsHurtVillagers) {
                cir.cancel();
            }
            if (isVillager && entity2 instanceof MobEntity) {
                List<MobEntity> list = entity2.getWorld().getNonSpectatingEntities(MobEntity.class, entity2.getBoundingBox().expand(GuardVillagers.config.GuardVillagerHelpRange, 5.0D, GuardVillagers.config.GuardVillagerHelpRange));
                for (MobEntity mob : list) {
                    boolean type = mob instanceof GuardEntity || mob.getType() == EntityType.IRON_GOLEM;
                    boolean trueSourceGolem = entity2 instanceof GuardEntity || entity2.getType() == EntityType.IRON_GOLEM;
                    if (!trueSourceGolem && type && mob.getTarget() == null)
                        mob.setTarget((MobEntity) entity2);
                }
            }
        }
    }
}
