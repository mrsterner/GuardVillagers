package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.VillagerEntity;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class VillagerGossipToGuardGoal extends Goal {
    protected final VillagerEntity villager;
    protected GuardEntity guard;

    public VillagerGossipToGuardGoal(VillagerEntity villager) {
        this.villager = villager;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.villager.getBrain().hasMemoryModule(MemoryModuleType.INTERACTION_TARGET) && this.villager.getBrain().getOptionalMemory(MemoryModuleType.INTERACTION_TARGET).get() instanceof GuardEntity guard) {
            this.guard = guard;
            long gameTime = guard.getWorld().getTime();
            if (!nearbyVillagersInteractingWithGuards() && (gameTime < this.guard.lastGossipTime || gameTime >= this.guard.lastGossipTime + 1200L))
                return this.guard.getTarget() == null && !this.villager.getWorld().isNight();
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return !nearbyVillagersInteractingWithGuards() && guard.getTarget() == null && this.villager.getBrain().hasMemoryModule(MemoryModuleType.INTERACTION_TARGET) && this.villager.getBrain().getOptionalMemory(MemoryModuleType.INTERACTION_TARGET).get().equals(guard);
    }

    @Override
    public void start() {
        this.villager.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, guard);
    }

    @Override
    public void tick() {
        this.villager.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, guard);
        if (!nearbyVillagersInteractingWithGuards() && this.villager.getBrain().hasMemoryModule(MemoryModuleType.INTERACTION_TARGET) && this.villager.getBrain().getOptionalMemory(MemoryModuleType.INTERACTION_TARGET).get().equals(guard)) {
            LookTargetUtil.lookAt(villager, guard);
            if (this.villager.distanceTo(guard) > 2.0D) {
                this.villager.getNavigation().startMovingTo(guard, 0.5D);
            } else {
                this.villager.getNavigation().stop();
                guard.gossip(villager, guard.getWorld().getTime());
            }
            this.villager.lookAtEntity(guard, 30.0F, 30.0F);
            this.villager.getLookControl().lookAt(guard, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        this.villager.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
    }

    private boolean nearbyVillagersInteractingWithGuards() {
        if (villager.getBrain().hasMemoryModule(MemoryModuleType.MOBS)) {
            Optional<List<LivingEntity>> list = villager.getBrain().getOptionalMemory(MemoryModuleType.MOBS);
            for (LivingEntity entity : list.get()) {
                if (entity instanceof VillagerEntity nearbyVillager) {
                    if (nearbyVillager.getBrain().hasMemoryModule(MemoryModuleType.INTERACTION_TARGET))
                        return nearbyVillager.getBrain().hasMemoryModule(MemoryModuleType.INTERACTION_TARGET) && nearbyVillager.getBrain().getOptionalMemory(MemoryModuleType.INTERACTION_TARGET).get().equals(guard);
                }
            }
        }
        return false;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }
}