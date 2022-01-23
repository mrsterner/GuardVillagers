package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.BowItem;

import java.util.EnumSet;

public class RangedBowAttackPassiveGoal<T extends GuardEntity & RangedAttackMob> extends Goal {
    private final T entity;
    private final double moveSpeedAmp;
    private int attackCooldown;
    private final float maxAttackDistance;
    private int attackTime = -1;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public RangedBowAttackPassiveGoal(T mob, double moveSpeedAmpIn, int attackCooldownIn, float maxAttackDistanceIn) {
        this.entity = mob;
        this.moveSpeedAmp = moveSpeedAmpIn;
        this.attackCooldown = attackCooldownIn;
        this.maxAttackDistance = maxAttackDistanceIn * maxAttackDistanceIn;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    public void setAttackCooldown(int attackCooldownIn) {
        this.attackCooldown = attackCooldownIn;
    }

    @Override
    public boolean canStart() {
        return this.entity.getTarget() != null && this.isBowInMainhand() && !this.entity.isEating() && !this.entity.isBlocking();
    }

    protected boolean isBowInMainhand() {
        return this.entity.getMainHandStack().getItem() instanceof BowItem;
    }

    @Override
    public boolean shouldContinue() {
        return (this.canStart() || !this.entity.getNavigation().isIdle()) && this.isBowInMainhand();
    }

    @Override
    public void start() {
        super.start();
        this.entity.setAttacking(true);
    }

    @Override
    public void stop() {
        super.stop();
        this.entity.setAttacking(false);
        this.seeTime = 0;
        this.attackTime = -1;
        this.entity.stopUsingItem();
    }

    @Override
    public void tick() {
        LivingEntity livingentity = this.entity.getTarget();
        if (livingentity != null) {
            double d0 = this.entity.squaredDistanceTo(livingentity.getX(), livingentity.getY(), livingentity.getZ());
            boolean flag = this.entity.getVisibilityCache().canSee(livingentity);
            boolean flag1 = this.seeTime > 0;
            if (flag != flag1) {
                this.seeTime = 0;
            }

            if (flag) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            if (!(d0 > (double) this.maxAttackDistance) && this.seeTime >= 20) {
                this.entity.getNavigation().stop();
                ++this.strafingTime;
            } else {
                this.entity.getNavigation().startMovingTo(livingentity, this.moveSpeedAmp);
                this.strafingTime = -1;
            }

            if (this.strafingTime >= 20) {
                if ((double) this.entity.getRandom().nextFloat() < 0.3D) {
                    this.strafingClockwise = !this.strafingClockwise;
                }

                if ((double) this.entity.getRandom().nextFloat() < 0.3D) {
                    this.strafingBackwards = !this.strafingBackwards;
                }

                this.strafingTime = 0;
            }

            if (this.strafingTime > -1) {
                if (d0 > (double) (this.maxAttackDistance * 0.75F)) {
                    this.strafingBackwards = false;
                } else if (d0 < (double) (this.maxAttackDistance * 0.25F)) {
                    this.strafingBackwards = true;
                }
                if (entity.getPatrolPos() == null)
                    this.entity.getMoveControl().strafeTo(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
                this.entity.lookAtEntity(livingentity, 30.0F, 30.0F);
            }
            this.entity.lookAtEntity(livingentity, 30.0F, 30.0F);
            this.entity.getLookControl().lookAt(livingentity, 30.0F, 30.0F);
            if (this.entity.isUsingItem() && !this.entity.isBlocking()) {
                if (!flag && this.seeTime < -60) {
                    this.entity.stopUsingItem();
                } else if (flag) {
                    int i = this.entity.getItemUseTime();
                    if (i >= 20) {
                        System.out.println("AttackGoal i>20 ");
                        this.entity.stopUsingItem();
                        this.entity.attack(livingentity, BowItem.getPullProgress(i));
                        this.attackTime = this.attackCooldown;
                    }
                }
            } else if (--this.attackTime <= 0 && this.seeTime >= -60 && !this.entity.isBlocking()) {
                this.entity.setCurrentHand(GuardVillagers.getHandWith(entity, item -> item instanceof BowItem));
            }

        }
    }
}