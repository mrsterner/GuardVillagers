package dev.sterner.common.entity.goal;

import dev.sterner.common.entity.GuardEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.VillagerEntity;

import java.util.EnumSet;
import java.util.List;

public class GuardLookAtAndStopMovingWhenBeingTheInteractionTarget extends Goal {
    private final GuardEntity guard;
    private VillagerEntity villager;

    public GuardLookAtAndStopMovingWhenBeingTheInteractionTarget(GuardEntity guard) {
        this.guard = guard;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        List<VillagerEntity> list = this.guard.getWorld().getNonSpectatingEntities(VillagerEntity.class, guard.getBoundingBox().expand(10.0D));
        if (!list.isEmpty()) {
            for (VillagerEntity villager : list) {
                if (villager.getBrain().hasMemoryModule(MemoryModuleType.INTERACTION_TARGET) && villager.getBrain().getOptionalMemory(MemoryModuleType.INTERACTION_TARGET).get().equals(guard)) {
                    this.villager = villager;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return canStart();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        guard.getNavigation().stop();
        guard.lookAtEntity(villager, 30.0F, 30.0F);
        guard.getLookControl().lookAt(villager);
    }
}