package dev.sterner.common.entity.goal;

import dev.sterner.GuardVillagers;
import dev.sterner.GuardVillagersConfig;
import dev.sterner.common.entity.GuardEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class RangedCrossbowAttackPassiveGoal<T extends PathAwareEntity & RangedAttackMob & CrossbowUser> extends Goal {
    public static final UniformIntProvider PATHFINDING_DELAY_RANGE = TimeHelper.betweenSeconds(1, 2);
    private final T mob;
    private final double speedModifier;
    private final float attackRadiusSqr;
    protected double wantedX;
    protected double wantedY;
    protected double wantedZ;
    private CrossbowState crossbowState = CrossbowState.UNCHARGED;
    private int seeTime;
    private int attackDelay;
    private int updatePathDelay;
    private int runTime;

    public RangedCrossbowAttackPassiveGoal(T pMob, double pSpeedModifier, float pAttackRadius) {
        this.mob = pMob;
        this.speedModifier = pSpeedModifier;
        this.attackRadiusSqr = pAttackRadius * pAttackRadius;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return this.isValidTarget() && this.isHoldingCrossbow();
    }

    private boolean isHoldingCrossbow() {
        return this.mob.isHolding(is -> is.getItem() instanceof CrossbowItem);
    }

    @Override
    public boolean shouldContinue() {
        return this.isValidTarget() && (this.canStart() || !this.mob.getNavigation().isIdle()) && this.isHoldingCrossbow();
    }

    private boolean isValidTarget() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.setAttacking(false);
        this.mob.setTarget(null);
        this.seeTime = 0;
        if (this.mob.isUsingItem()) {
            this.mob.stopUsingItem();
            this.mob.setCharging(false);
        }
        this.mob.setPose(EntityPose.STANDING);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.mob.setAttacking(true);
    }

    @Override
    public void tick() {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity != null) {
            boolean canSee = this.mob.getVisibilityCache().canSee(livingentity);
            boolean hasSeenEntityRecently = this.seeTime > 0;
            if (canSee != hasSeenEntityRecently) {
                this.seeTime = 0;
            }
            if (canSee) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }
            double d0 = this.mob.squaredDistanceTo(livingentity);
            double d1 = livingentity.distanceTo(this.mob);
            if (d1 <= 4.0D) {
                this.mob.getMoveControl().strafeTo(this.mob.isUsingItem() ? -0.5F : -3.0F, 0.0F);
                this.mob.lookAtEntity(livingentity, 30.0F, 30.0F);
            }
            if (this.mob.getRandom().nextInt(50) == 0) {
                if (this.mob.isInPose(EntityPose.STANDING))
                    this.mob.setPose(EntityPose.CROUCHING);
                else
                    this.mob.setPose(EntityPose.STANDING);
            }
            boolean canSee2 = (d0 > (double) this.attackRadiusSqr || this.seeTime < 5) && this.attackDelay == 0;
            if (canSee2) {
                --this.updatePathDelay;
                if (this.updatePathDelay <= 0) {
                    this.mob.getNavigation().startMovingTo(livingentity, this.canRun() ? this.speedModifier : this.speedModifier * 0.5D);
                    this.updatePathDelay = PATHFINDING_DELAY_RANGE.get(this.mob.getRandom());
                }
            } else {
                this.updatePathDelay = 0;
                this.mob.getNavigation().stop();
            }
            this.mob.lookAtEntity(livingentity, 30.0F, 30.0F);
            this.mob.getLookControl().lookAt(livingentity, 30.0F, 30.0F);
            if (this.friendlyInLineOfSight() && GuardVillagersConfig.friendlyFire)
                this.crossbowState = CrossbowState.FIND_NEW_POSITION;
            if (this.crossbowState == CrossbowState.FIND_NEW_POSITION && GuardVillagersConfig.friendlyFire) {
                this.mob.stopUsingItem();
                this.mob.setCharging(false);
                if (this.findPosition())
                    this.mob.getNavigation().startMovingTo(this.wantedX, this.wantedY, this.wantedZ, this.mob.isSneaking() ? 0.5F : 1.2D);
                this.crossbowState = CrossbowState.UNCHARGED;
            } else if (this.crossbowState == CrossbowState.UNCHARGED) {
                if (hasSeenEntityRecently) {
                    this.mob.setCurrentHand(GuardVillagers.getHandWith(this.mob, item -> item instanceof CrossbowItem));
                    this.crossbowState = CrossbowState.CHARGING;
                    this.mob.setCharging(true);
                }
            } else if (this.crossbowState == CrossbowState.CHARGING) {
                if (!this.mob.isUsingItem()) {
                    this.crossbowState = CrossbowState.UNCHARGED;
                }
                int i = this.mob.getItemUseTime();
                ItemStack itemstack = this.mob.getActiveItem();
                if (i >= CrossbowItem.getPullTime(itemstack) || CrossbowItem.isCharged(itemstack)) {
                    this.mob.stopUsingItem();
                    this.crossbowState = CrossbowState.CHARGED;
                    this.attackDelay = 10 + this.mob.getRandom().nextInt(5);
                    this.mob.setCharging(false);
                }
            } else if (this.crossbowState == CrossbowState.CHARGED) {
                --this.attackDelay;
                if (this.attackDelay == 0) {
                    this.crossbowState = CrossbowState.READY_TO_ATTACK;
                }
            } else if (this.crossbowState == CrossbowState.READY_TO_ATTACK && canSee) {
                this.mob.attack(livingentity, 1.0F);
                ItemStack itemstack1 = this.mob.getStackInHand(GuardVillagers.getHandWith(this.mob, item -> item instanceof CrossbowItem));
                CrossbowItem.setCharged(itemstack1, false);
                this.crossbowState = CrossbowState.UNCHARGED;
            }
        }

    }

    private boolean friendlyInLineOfSight() {
        List<Entity> list = this.mob.getWorld().getOtherEntities(this.mob, this.mob.getBoundingBox().expand(5.0D));
        for (Entity guard : list) {
            if (guard != this.mob.getTarget()) {
                boolean isVillager = ((GuardEntity)this.mob).getOwner() == guard || guard.getType() == EntityType.VILLAGER || guard.getType() == GuardVillagers.GUARD_VILLAGER || guard.getType() == EntityType.IRON_GOLEM;
                if (isVillager) {
                    Vec3d vector3d = this.mob.getRotationVector();
                    Vec3d vector3d1 = guard.getPos().relativize(this.mob.getPos()).normalize();
                    vector3d1 = new Vec3d(vector3d1.x, vector3d1.y, vector3d1.z);
                    if (vector3d1.dotProduct(vector3d) < 1.0D && this.mob.canSee(guard) && guard.distanceTo(this.mob) <= 4.0D)
                        return true;
                }
            }
        }
        return false;
    }

    public boolean findPosition() {
        Vec3d vector3d = this.getPosition();
        if (vector3d == null) {
            return false;
        } else {
            this.wantedX = vector3d.x;
            this.wantedY = vector3d.y;
            this.wantedZ = vector3d.z;
            return true;
        }
    }

    @Nullable
    protected Vec3d getPosition() {
        if (this.isValidTarget())
            return NoPenaltyTargeting.findFrom(this.mob, 16, 7, this.mob.getTarget().getPos());
        else
            return NoPenaltyTargeting.find(this.mob, 16, 7);
    }

    private boolean canRun() {
        return this.crossbowState == CrossbowState.UNCHARGED;
    }

    public enum CrossbowState {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK,
        FIND_NEW_POSITION
    }
}