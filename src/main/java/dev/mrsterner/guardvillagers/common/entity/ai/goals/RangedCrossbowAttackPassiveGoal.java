package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

public class RangedCrossbowAttackPassiveGoal<T extends PathAwareEntity & RangedAttackMob & CrossbowUser> extends Goal {
    private final T entity;
    private RangedCrossbowAttackPassiveGoal.CrossbowState crossbowState = RangedCrossbowAttackPassiveGoal.CrossbowState.UNCHARGED;
    private final double speed;
    private final float distanceMoveToEntity;
    private int seeTicks;
    private int timeUntilStrike;

    public RangedCrossbowAttackPassiveGoal(T entity, double p_i50322_2_, float p_i50322_4_) {
        this.entity = entity;
        this.speed = p_i50322_2_;
        this.distanceMoveToEntity = p_i50322_4_ * p_i50322_4_;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return this.hasAttackTarget() && this.isHoldingCrossbow() && !((GuardEntity) this.entity).isEating();
    }

    private boolean isHoldingCrossbow() {
        return this.entity.getMainHandStack().getItem() instanceof CrossbowItem;
    }

    @Override
    public boolean shouldContinue() {
        return this.hasAttackTarget() && (this.canStart() || !this.entity.getNavigation().isIdle()) && this.isHoldingCrossbow();
    }

    private boolean hasAttackTarget() {
        return this.entity.getTarget() != null && this.entity.getTarget().isAlive();
    }

    @Override
    public void stop() {
        super.stop();
        this.entity.setAttacking(false);
        this.entity.setTarget(null);
        ((GuardEntity) this.entity).setKicking(false);
        this.seeTicks = 0;
        if (this.entity.getPose() == EntityPose.CROUCHING)
            this.entity.setPose(EntityPose.STANDING);
        if (this.entity.isUsingItem()) {
            this.entity.stopUsingItem();
            this.entity.setCharging(false);
        }
    }

    public boolean checkFriendlyFire() {
        List<LivingEntity> list = this.entity.world.getNonSpectatingEntities(LivingEntity.class, this.entity.getBoundingBox().expand(5.0D, 1.0D, 5.0D));
        for (LivingEntity guard : list) {
            if (entity != guard) {
                if (guard != entity.getTarget()) {
                    boolean isVillager = guard.getType() == EntityType.VILLAGER || guard.getType() == GuardVillagers.GUARD_VILLAGER || guard.getType() == EntityType.IRON_GOLEM;
                    if (isVillager) {
                        Vec3d vector3d = entity.getRotationVector();
                        Vec3d vector3d1 = guard.getPos().relativize(entity.getPos()).normalize();
                        vector3d1 = new Vec3d(vector3d1.x, vector3d1.y, vector3d1.z);
                        if (vector3d1.dotProduct(vector3d) < 1.0D && entity.canSee(guard))
                            return GuardVillagers.config.general.FriendlyFire;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
        LivingEntity livingentity = this.entity.getTarget();
        if (livingentity != null) {
            this.entity.setAttacking(true);
            boolean flag = this.entity.getVisibilityCache().canSee(livingentity);
            boolean flag1 = this.seeTicks > 0;
            if (flag != flag1) {
                this.seeTicks = 0;
            }

            if (flag) {
                ++this.seeTicks;
            } else {
                --this.seeTicks;
            }

            if (this.entity.getPose() == EntityPose.STANDING && this.entity.world.random.nextInt(4) == 0 && entity.age % 50 == 0) {
                this.entity.setPose(EntityPose.CROUCHING);
            }

            if (this.entity.getPose() == EntityPose.CROUCHING && this.entity.world.random.nextInt(4) == 0 && entity.age % 100 == 0) {
                this.entity.setPose(EntityPose.STANDING);
            }

            double d1 = livingentity.distanceTo(entity);
            if (d1 <= 2.0D) {
                this.entity.getMoveControl().strafeTo(this.entity.isUsingItem() ?- 0.5F : -3.0F, 0.0F);
                this.entity.lookAtEntity(livingentity, 30.0F, 30.0F);
            }

            double d0 = this.entity.squaredDistanceTo(livingentity);
            boolean flag2 = (d0 > (double) this.distanceMoveToEntity || this.seeTicks < 5) && this.timeUntilStrike == 0;
            if (flag2) {
                this.entity.getNavigation().startMovingTo(livingentity, this.isCrossbowUncharged() ? this.speed : this.speed * 0.5D);
            } else {
                this.entity.getNavigation().stop();
            }
            this.entity.lookAtEntity(livingentity, 30.0F, 30.0F);
            this.entity.getLookControl().lookAt(livingentity, 30.0F, 30.0F);
            if (this.crossbowState == RangedCrossbowAttackPassiveGoal.CrossbowState.UNCHARGED && !CrossbowItem.isCharged(entity.getActiveItem()) && !entity.isBlocking()) {
                if (flag) {
                    this.entity.setCurrentHand(GuardVillagers.getHandWith(entity, item -> item instanceof CrossbowItem));
                    this.crossbowState = RangedCrossbowAttackPassiveGoal.CrossbowState.CHARGING;
                    this.entity.setCharging(true);
                }
            } else if (this.crossbowState == RangedCrossbowAttackPassiveGoal.CrossbowState.CHARGING) {
                if (!this.entity.isUsingItem())
                    this.crossbowState = RangedCrossbowAttackPassiveGoal.CrossbowState.UNCHARGED;
                int i = this.entity.getItemUseTime();
                ItemStack itemstack = this.entity.getActiveItem();
                if (i >= CrossbowItem.getPullTime(itemstack) || CrossbowItem.isCharged(entity.getActiveItem())) {
                    this.entity.stopUsingItem();
                    this.crossbowState = RangedCrossbowAttackPassiveGoal.CrossbowState.CHARGED;
                    this.timeUntilStrike = 20 + this.entity.getRandom().nextInt(20);
                    this.entity.setCharging(false);
                }
            } else if (this.crossbowState == RangedCrossbowAttackPassiveGoal.CrossbowState.CHARGED) {
                --this.timeUntilStrike;
                if (this.timeUntilStrike == 0) {
                    this.crossbowState = RangedCrossbowAttackPassiveGoal.CrossbowState.READY_TO_ATTACK;
                }
            } else if (this.crossbowState == RangedCrossbowAttackPassiveGoal.CrossbowState.READY_TO_ATTACK && flag && !checkFriendlyFire() && !entity.isBlocking()) {
                this.entity.attack(livingentity, 1.0F);
                ItemStack itemstack1 = this.entity.getStackInHand(GuardVillagers.getHandWith(entity, item -> item instanceof CrossbowItem));
                CrossbowItem.setCharged(itemstack1, false);
                this.crossbowState = RangedCrossbowAttackPassiveGoal.CrossbowState.UNCHARGED;
            }
        }
    }

    private boolean isCrossbowUncharged() {
        return this.crossbowState == RangedCrossbowAttackPassiveGoal.CrossbowState.UNCHARGED;
    }

    enum CrossbowState {
        UNCHARGED, CHARGING, CHARGED, READY_TO_ATTACK;
    }
}