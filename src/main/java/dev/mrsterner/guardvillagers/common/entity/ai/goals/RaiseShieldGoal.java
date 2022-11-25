package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;

public class RaiseShieldGoal extends Goal {

    public final GuardEntity guard;

    public RaiseShieldGoal(GuardEntity guard) {
        this.guard = guard;
    }

    @Override
    public boolean canStart() {
        return !CrossbowItem.isCharged(guard.getMainHandStack()) && (guard.getOffHandStack().getItem() instanceof ShieldItem) && raiseShield() && guard.shieldCoolDown == 0;

    }

    @Override
    public boolean shouldContinue() {
        return this.canStart() && guard.getTarget() != null;
    }

    @Override
    public void start() {
        if (guard.getOffHandStack().getItem() instanceof ShieldItem)
            guard.setCurrentHand(Hand.OFF_HAND);
    }

    @Override
    public void stop() {
        if (!GuardVillagers.config.GuardAlwaysShield)
            guard.stopUsingItem();
    }

    protected boolean raiseShield() {
        LivingEntity target = guard.getTarget();
        if (target != null && guard.shieldCoolDown == 0) {
            boolean ranged = guard.getMainHandStack().getItem() instanceof CrossbowItem || guard.getMainHandStack().getItem() instanceof BowItem;
            return guard.distanceTo(target) <= 4.0D || target instanceof CreeperEntity || target instanceof RangedAttackMob && target.distanceTo(guard) >= 5.0D && !ranged || target instanceof RavagerEntity || GuardVillagers.config.GuardAlwaysShield;
        }
        return false;
    }
}