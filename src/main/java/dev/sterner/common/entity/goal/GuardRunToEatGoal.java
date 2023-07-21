package dev.sterner.common.entity.goal;

import dev.sterner.common.entity.GuardEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

public class GuardRunToEatGoal extends WanderAroundGoal {
    private final GuardEntity guard;
    private int walkTimer;

    public GuardRunToEatGoal(GuardEntity guard) {
        super(guard, 1.0D);
        this.guard = guard;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.TARGET, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return guard.getHealth() < (guard.getMaxHealth() / 2) && GuardEatFoodGoal.isConsumable(guard.getOffHandStack()) && !guard.isEating() && guard.getTarget() != null && this.getWanderTarget() != null;
    }

    @Override
    public void start() {
        super.start();
        this.guard.setTarget(null);
        if (this.walkTimer <= 0) {
            this.walkTimer = 20;
        }
    }

    @Override
    public void tick() {
        --walkTimer;
        List<LivingEntity> list = this.guard.getWorld().getNonSpectatingEntities(LivingEntity.class, this.guard.getBoundingBox().expand(5.0D, 3.0D, 5.0D));
        if (!list.isEmpty()) {
            for (LivingEntity mob : list) {
                if (mob != null) {
                    if (mob.getAttacking() instanceof GuardEntity || mob instanceof MobEntity && ((MobEntity) mob).getTarget() instanceof GuardEntity) {
                        if (walkTimer < 20)
                            this.walkTimer += 5;
                    }
                }
            }
        }
    }


    @Override
    protected Vec3d getWanderTarget() {
        List<LivingEntity> list = this.guard.getWorld().getNonSpectatingEntities(LivingEntity.class, this.guard.getBoundingBox().expand(5.0D, 3.0D, 5.0D));
        if (!list.isEmpty()) {
            for (LivingEntity mob : list) {
                if (mob != null) {
                    if (mob.getAttacking() instanceof GuardEntity || mob instanceof MobEntity && ((MobEntity) mob).getTarget() instanceof GuardEntity) {
                        return NoPenaltyTargeting.findFrom(guard, 16, 7, mob.getPos());
                    }
                }
            }
        }
        return super.getWanderTarget();
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && this.walkTimer > 0 && !guard.isEating();
    }

    @Override
    public void stop() {
        super.stop();
        this.guard.setCurrentHand(Hand.OFF_HAND);
        this.guard.getNavigation().stop();
    }
}