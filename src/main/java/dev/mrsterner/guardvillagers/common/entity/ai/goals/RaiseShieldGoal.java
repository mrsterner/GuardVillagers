package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import dev.mrsterner.guardvillagers.GuardVillagersConfig;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
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
        /*TODO
        return !CrossbowItem.isCharged(guard.getMainHandStack()) && (guard.getOffHandStack().getItem().canPerformAction(guard.getOffHandStack(),
        GuardEntity.ToolActions.SHIELD_BLOCK) && raiseShield() && guard.shieldCoolDown == 0
        && !guard.getOffHandStack().getItem().equals(ForgeRegistries.ITEMS.getValue(new ResourceLocation("bigbrain:buckler"))));

         */
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return this.canStart();
    }

    @Override
    public void start() {
        /*TODO
        if (GuardEntity.canPerformAction(guard.getOffHandStack(), GuardEntity.ToolActions.SHIELD_BLOCK))
            guard.startUsingItem(Hand.OFF_HAND);

         */
    }

    @Override
    public void stop() {
        if (!GuardVillagersConfig.get().GuardAlwaysShield)
            guard.stopUsingItem();
    }

    protected boolean raiseShield() {
        LivingEntity target = guard.getTarget();
        if (target != null && guard.shieldCoolDown == 0) {
            boolean ranged = guard.getMainHandStack().getItem() instanceof CrossbowItem || guard.getMainHandStack().getItem() instanceof BowItem;
            if (guard.distanceTo(target) <= 4.0D || target instanceof CreeperEntity || target instanceof RangedAttackMob && target.distanceTo(guard) >= 5.0D && !ranged || target instanceof RavagerEntity || GuardVillagersConfig.get().GuardAlwaysShield) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}