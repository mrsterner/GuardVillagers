package dev.sterner.guardvillagers.common.entity.task;

import com.google.common.collect.ImmutableMap;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.villager.Villager;

public class ShareGossipWithGuard extends Behavior<Villager> {
    public ShareGossipWithGuard() {
        super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverWorld, Villager villagerEntity) {
        return villagerEntity.getBrain()
                .getMemory(MemoryModuleType.INTERACTION_TARGET)
                .filter(e -> e instanceof GuardEntity)
                .isPresent();
    }

    @Override
    protected boolean canStillUse(ServerLevel serverWorld, Villager villagerEntity, long time) {
        return this.checkExtraStartConditions(serverWorld, villagerEntity);
    }

    @Override
    protected void start(ServerLevel serverWorld, Villager villagerEntity, long time) {
        GuardEntity guard = (GuardEntity) villagerEntity.getBrain()
                .getMemory(MemoryModuleType.INTERACTION_TARGET).get();
        walkTowardsEachOther(villagerEntity, guard, 0.5F);
    }

    @Override
    protected void tick(ServerLevel serverWorld, Villager villagerEntity, long time) {
        GuardEntity guard = (GuardEntity) villagerEntity.getBrain()
                .getMemory(MemoryModuleType.INTERACTION_TARGET).get();
        if (villagerEntity.distanceToSqr(guard) < 5.0D) {
            walkTowardsEachOther(villagerEntity, guard, 0.5F);
            guard.gossip(villagerEntity, time);
        }
    }

    @Override
    protected void stop(ServerLevel serverWorld, Villager villagerEntity, long time) {
        villagerEntity.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
    }

    private void walkTowardsEachOther(Villager villager, GuardEntity guard, float speed) {
        villager.getLookControl().setLookAt(guard, 30.0f, 30.0f);
        guard.getLookControl().setLookAt(villager, 30.0f, 30.0f);
        villager.getNavigation().moveTo(guard, speed);
    }
}