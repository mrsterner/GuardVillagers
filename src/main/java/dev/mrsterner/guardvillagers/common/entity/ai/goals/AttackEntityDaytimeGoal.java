package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.entity.mob.SpiderEntity;

public class AttackEntityDaytimeGoal<T extends LivingEntity> extends TargetGoal<T> {
    public AttackEntityDaytimeGoal(SpiderEntity spider, Class<T> classTarget) {
        super(spider, classTarget, true);
    }

    @Override
    public boolean canStart() {
        float f = this.mob.method_5718();
        return !(f >= 0.5F) && super.canStart();
    }
}