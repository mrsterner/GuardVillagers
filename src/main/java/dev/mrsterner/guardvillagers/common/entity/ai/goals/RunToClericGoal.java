package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;

import java.util.List;

public class RunToClericGoal extends Goal {
    public final GuardEntity guard;
    public VillagerEntity cleric;

    public RunToClericGoal(GuardEntity guard) {
        this.guard = guard;
    }

    @Override
    public boolean canStart() {
        List<VillagerEntity> list = this.guard.world.getNonSpectatingEntities(VillagerEntity.class, this.guard.getBoundingBox().expand(10.0D, 3.0D, 10.0D));
        if (!list.isEmpty()) {
            for (VillagerEntity mob : list) {
                if (mob != null) {
                    if (mob.getVillagerData().getProfession() == VillagerProfession.CLERIC && guard.getHealth() < guard.getMaxHealth() && guard.getTarget() == null && !guard.hasStatusEffect(StatusEffects.REGENERATION)) {
                        this.cleric = mob;
                        return GuardVillagers.config.generail.ClericHealing;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
        guard.lookAtEntity(cleric, 30.0F, 30.0F);
        guard.getLookControl().lookAt(cleric, 30.0F, 30.0F);
        if (guard.distanceTo(cleric) >= 6.0D) {
            guard.getNavigation().startMovingTo(cleric, 0.5D);
        } else {
            guard.getMoveControl().strafeTo(-1.0F, 0.0F);
            guard.getNavigation().stop();
        }
    }
}