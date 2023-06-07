package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;

import java.util.List;

public class GuardEatFoodGoal extends Goal {
    public final GuardEntity guard;

    public GuardEatFoodGoal(GuardEntity guard) {
        this.guard = guard;
    }

    @Override
    public boolean canStart() {
        return !guard.isRunningToEat() && guard.getHealth() < guard.getMaxHealth() && GuardEatFoodGoal.isConsumable(guard.getOffHandStack()) && guard.isEating() || guard.getHealth() < guard.getMaxHealth() && GuardEatFoodGoal.isConsumable(guard.getOffHandStack()) && guard.getTarget() == null && !guard.isAttacking();
    }

    public static boolean isConsumable(ItemStack stack) {
        return stack.getUseAction() == UseAction.EAT  && stack.getCount() > 0 || stack.getUseAction() == UseAction.DRINK && !(stack.getItem() instanceof SplashPotionItem) && stack.getCount() > 0;
    }

    @Override
    public boolean shouldContinue() {
        List<LivingEntity> list = this.guard.getWorld().getNonSpectatingEntities(LivingEntity.class, this.guard.getBoundingBox().expand(5.0D, 3.0D, 5.0D));
        if (!list.isEmpty()) {
            for (LivingEntity mob : list) {
                if (mob != null) {
                    if (mob instanceof MobEntity && ((MobEntity) mob).getTarget() instanceof GuardEntity) {
                        return false;
                    }
                }
            }
        }
        return guard.isUsingItem() && guard.getTarget() == null && guard.getHealth() < guard.getMaxHealth() || guard.getTarget() != null && guard.getHealth() < guard.getMaxHealth() / 2 + 2 && guard.isEating();
        // Guards will only keep eating until they're up to full health if they're not aggroed, otherwise they will just heal back above half health and then join back the fight.
    }

    @Override
    public void start() {
        if (guard.getTarget() == null)
            guard.setEating(true);
        guard.setCurrentHand(Hand.OFF_HAND);
    }

    @Override
    public void stop() {
        guard.setEating(false);
        guard.stopUsingItem();
    }
}