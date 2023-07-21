package dev.sterner.common.entity.goal;

import dev.sterner.GuardVillagersConfig;
import dev.sterner.common.entity.GuardEntity;
import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Hand;

public class RaiseShieldGoal extends Goal {

    public final GuardEntity guard;

    public RaiseShieldGoal(GuardEntity guard) {
        this.guard = guard;
    }

    @Override
    public boolean canStart() {
        return !CrossbowItem.isCharged(guard.getMainHandStack()) && (guard.getOffHandStack().getItem().canPerformAction(guard.getOffHandStack(),
                ToolActions.SHIELD_BLOCK) && raiseShield() && guard.shieldCoolDown == 0);
    }

    @Override
    public boolean shouldContinue() {
        return this.canStart();
    }

    @Override
    public void start() {
        if (guard.getOffHandStack().getItem().canPerformAction(guard.getOffHandStack(), ToolActions.SHIELD_BLOCK))
            guard.setCurrentHand(Hand.OFF_HAND);
    }

    @Override
    public void stop() {
        if (!GuardVillagersConfig.guardAlwaysShield)
            guard.stopUsingItem();
    }

    protected boolean raiseShield() {
        LivingEntity target = guard.getTarget();
        if (target != null && guard.shieldCoolDown == 0) {
            boolean ranged = guard.getMainHandStack().getItem() instanceof CrossbowItem || guard.getMainHandStack().getItem() instanceof BowItem;
            return guard.distanceTo(target) <= 4.0D || target instanceof CreeperEntity || target instanceof RangedAttackMob && target.distanceTo(guard) >= 5.0D && !ranged || target instanceof RavagerEntity || GuardVillagersConfig.guardAlwaysShield;
        }
        return false;
    }
}