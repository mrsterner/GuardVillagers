package dev.sterner.guardvillagers.mixin;

import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import dev.sterner.guardvillagers.common.entity.goal.AttackEntityDaytimeGoal;
import dev.sterner.guardvillagers.common.entity.goal.HealGolemGoal;
import dev.sterner.guardvillagers.common.entity.goal.HealGuardAndPlayerGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements StructureWorldAccess {

    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Inject(method = "spawnEntity", at = @At("TAIL"))
    private void onSpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (GuardVillagersConfig.raidAnimals) {
            if (entity instanceof RaiderEntity raiderEntity)
                if (raiderEntity.hasActiveRaid()) {
                    raiderEntity.targetSelector.add(5, new ActiveTargetGoal<>(raiderEntity, AnimalEntity.class, false));
                }
        }

        if (GuardVillagersConfig.attackAllMobs) {
            if (entity instanceof HostileEntity && !(entity instanceof SpiderEntity)) {
                MobEntity mob = (MobEntity) entity;
                mob.targetSelector.add(2, new ActiveTargetGoal<>(mob, GuardEntity.class, false));
            }
            if (entity instanceof SpiderEntity spider) {
                spider.targetSelector.add(3, new AttackEntityDaytimeGoal<>(spider, GuardEntity.class));
            }
        }


        if (entity instanceof IllagerEntity illager) {
            if (GuardVillagersConfig.illagersRunFromPolarBears) {
                illager.goalSelector.add(2, new FleeEntityGoal<>(illager, PolarBearEntity.class, 6.0F, 1.0D, 1.2D));
            }

            illager.targetSelector.add(2, new ActiveTargetGoal<>(illager, GuardEntity.class, false));
        }

        if (entity instanceof VillagerEntity villagerEntity) {
            if (GuardVillagersConfig.villagersRunFromPolarBears)
                villagerEntity.goalSelector.add(2, new FleeEntityGoal<>(villagerEntity, PolarBearEntity.class, 6.0F, 1.0D, 1.2D));
            if (GuardVillagersConfig.witchesVillager)
                villagerEntity.goalSelector.add(2, new FleeEntityGoal<>(villagerEntity, WitchEntity.class, 6.0F, 1.0D, 1.2D));
        }

        if (entity instanceof VillagerEntity villagerEntity) {
            if (GuardVillagersConfig.blackSmithHealing)
                villagerEntity.goalSelector.add(1, new HealGolemGoal(villagerEntity));
            if (GuardVillagersConfig.clericHealing)
                villagerEntity.goalSelector.add(1, new HealGuardAndPlayerGoal(villagerEntity, 1.0D, 100, 0, 10.0F));
        }

        if (entity instanceof IronGolemEntity golem) {

            RevengeGoal tolerateFriendlyFire = new RevengeGoal(golem, GuardEntity.class).setGroupRevenge();
            golem.targetSelector.getGoals().stream().map(PrioritizedGoal::getGoal).filter(it -> it instanceof RevengeGoal).findFirst().ifPresent(angerGoal -> {
                golem.targetSelector.remove(angerGoal);
                golem.targetSelector.add(2, tolerateFriendlyFire);
            });
        }

        if (entity instanceof ZombieEntity zombie) {
            zombie.targetSelector.add(3, new ActiveTargetGoal<>(zombie, GuardEntity.class, false));
        }

        if (entity instanceof RavagerEntity ravager) {
            ravager.targetSelector.add(2, new ActiveTargetGoal<>(ravager, GuardEntity.class, false));
        }

        if (entity instanceof WitchEntity witch) {
            if (GuardVillagersConfig.witchesVillager) {
                witch.targetSelector.add(3, new ActiveTargetGoal<>(witch, VillagerEntity.class, true));
                witch.targetSelector.add(3, new ActiveTargetGoal<>(witch, IronGolemEntity.class, true));
                witch.targetSelector.add(3, new ActiveTargetGoal<>(witch, GuardEntity.class, true));
            }
        }

        if (entity instanceof CatEntity cat) {
            cat.goalSelector.add(1, new FleeEntityGoal<>(cat, IllagerEntity.class, 12.0F, 1.0D, 1.2D));
        }
    }
}
