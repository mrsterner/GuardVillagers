package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
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

    public static boolean isConsumable(ItemStack stack) {
        return stack.getUseAction() == UseAction.EAT || stack.getUseAction() == UseAction.DRINK && !(stack.getItem() instanceof SplashPotionItem);
    }

    @Override
    public boolean canStart() {
        return guard.getHealth() < guard.getMaxHealth() && GuardEatFoodGoal.isConsumable(guard.getOffHandStack()) && guard.isEating() || guard.getHealth() < guard.getMaxHealth() && GuardEatFoodGoal.isConsumable(guard.getOffHandStack()) && guard.getTarget() == null && !guard.isAttacking();
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
        guard.setCurrentHand(Hand.OFF_HAND);
    }
}