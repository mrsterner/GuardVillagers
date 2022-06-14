package dev.mrsterner.guardvillagers.mixin;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.GuardVillagersConfig;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import dev.mrsterner.guardvillagers.common.entity.ai.goals.AttackEntityDaytimeGoal;
import dev.mrsterner.guardvillagers.common.entity.ai.goals.HealGolemGoal;
import dev.mrsterner.guardvillagers.common.entity.ai.goals.HealGuardAndPlayerGoal;
import dev.mrsterner.guardvillagers.common.events.GuardVillagersEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void onSpawnedEvent(Entity entity, CallbackInfoReturnable<Boolean> cir){
        GuardVillagersEvents.ON_SPAWNED_ENTITY_EVENT.invoker().onSpawned((ServerWorld) (Object) this, entity);
        if (GuardVillagers.config.general.RaidAnimals) {
            if (entity instanceof RaiderEntity raiderEntity)
                if (raiderEntity.hasActiveRaid()) {
                    ((MobEntityAccessor)raiderEntity).targetSelector().add(5, new TargetGoal<>(raiderEntity, AnimalEntity.class, false));
                }
        }
        /*
        if (GuardVillagers.config.general.AttackAllMobs) {
            if (entity instanceof Monster && !(entity instanceof SpiderEntity)) {
                MobEntity mob = (MobEntity) entity;
                ((MobEntityAccessor)mob).targetSelector().add(2, new ActiveTargetGoal<>(mob, GuardEntity.class, false));
            }
            if (entity instanceof SpiderEntity spider) {
                ((MobEntityAccessor)spider).targetSelector().add(3, new AttackEntityDaytimeGoal<>(spider, GuardEntity.class));
            }
        }

         */
        if (entity instanceof IllagerEntity illager) {
            ((MobEntityAccessor)illager).targetSelector().add(2, new TargetGoal<>(illager, GuardEntity.class, false));
        }

        if (entity instanceof VillagerEntity villagerEntity) {
            if (GuardVillagers.config.general.WitchesVillager)
                ((MobEntityAccessor)villagerEntity).goalSelector().add(2, new FleeEntityGoal<>(villagerEntity, WitchEntity.class, 6.0F, 1.0D, 1.2D));
        }

        if (entity instanceof VillagerEntity villagerEntity) {
            if (GuardVillagers.config.general.BlackSmithHealing)
                ((MobEntityAccessor)villagerEntity).goalSelector().add(1, new HealGolemGoal(villagerEntity));
            if (GuardVillagers.config.general.ClericHealing)
                ((MobEntityAccessor)villagerEntity).goalSelector().add(1, new HealGuardAndPlayerGoal(villagerEntity, 1.0D, 100, 0, 10.0F));
        }

        if (entity instanceof IronGolemEntity golem) {

            RevengeGoal tolerateFriendlyFire = new RevengeGoal(golem, GuardEntity.class).setGroupRevenge();
            ((MobEntityAccessor)golem).targetSelector().getGoals().stream().map(it -> it.getGoal()).filter(it -> it instanceof RevengeGoal).findFirst().ifPresent(angerGoal -> {
                ((MobEntityAccessor)golem).targetSelector().remove(angerGoal);
                ((MobEntityAccessor)golem).targetSelector().add(2, tolerateFriendlyFire);
            });
        }

        if (entity instanceof ZombieEntity zombie) {
            ((MobEntityAccessor)zombie).targetSelector().add(3,new TargetGoal<>(zombie, GuardEntity.class, false));
        }

        if (entity instanceof RavagerEntity ravager) {
            ((MobEntityAccessor)ravager).targetSelector().add(2, new TargetGoal<>(ravager, GuardEntity.class, false));
        }

        if (entity instanceof WitchEntity witch) {
            if (GuardVillagers.config.general.WitchesVillager) {
                ((MobEntityAccessor)witch).targetSelector().add(3, new TargetGoal<>(witch, VillagerEntity.class, true));
                ((MobEntityAccessor)witch).targetSelector().add(3, new TargetGoal<>(witch, IronGolemEntity.class, true));
                ((MobEntityAccessor)witch).targetSelector().add(3, new TargetGoal<>(witch, GuardEntity.class, true));
            }
        }

        if (entity instanceof CatEntity cat) {
            ((MobEntityAccessor)cat).goalSelector().add(1, new FleeEntityGoal<>(cat, IllagerEntity.class, 12.0F, 1.0D, 1.2D));
        }
    }
}
