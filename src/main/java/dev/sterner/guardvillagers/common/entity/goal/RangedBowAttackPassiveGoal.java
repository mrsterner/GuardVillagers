package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;

public class RangedBowAttackPassiveGoal<T extends GuardEntity & RangedAttackMob> extends Goal {
        private final T actor;
        private final double speed;
        private int attackInterval;
        private final float squaredRange;
        private int cooldown = -1;
        private int targetSeeingTicker;
        private boolean movingToLeft;
        private boolean backward;
        private int combatTicks = -1;

        public RangedBowAttackPassiveGoal(T actor, double speed, int attackInterval, float range) {
            this.actor = actor;
            this.speed = speed;
            this.attackInterval = attackInterval;
            this.squaredRange = range * range;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        public void setAttackInterval(int attackInterval) {
            this.attackInterval = attackInterval;
        }

        @Override
        public boolean canUse() {
            if (this.actor.getTarget() == null) {
                return false;
            }
            return this.isHoldingBow();
        }

        protected boolean isHoldingBow() {
            return this.actor.isHolding(Items.BOW);
        }

        @Override
        public boolean canContinueToUse() {
            return (this.canUse() || !this.actor.getNavigation().isDone()) && this.isHoldingBow();
        }

        @Override
        public void start() {
            super.start();
            this.actor.setAggressive(true);
        }

        @Override
        public void stop() {
            super.stop();
            this.actor.setAggressive(false);
            this.targetSeeingTicker = 0;
            this.cooldown = -1;
            this.actor.stopUsingItem();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            boolean bl2;
            LivingEntity livingEntity = ((Mob)this.actor).getTarget();
            if (livingEntity == null) {
                return;
            }
            double d = this.actor.distanceToSqr(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
            boolean bl = this.actor.getSensing().hasLineOfSight(livingEntity);
            bl2 = this.targetSeeingTicker > 0;
            if (bl != bl2) {
                this.targetSeeingTicker = 0;
            }
            this.targetSeeingTicker = bl ? ++this.targetSeeingTicker : --this.targetSeeingTicker;
            if (d > (double)this.squaredRange || this.targetSeeingTicker < 20) {
                this.actor.getNavigation().moveTo(livingEntity, this.speed);
                this.combatTicks = -1;
            } else {
                this.actor.getNavigation().stop();
                ++this.combatTicks;
            }
            if (this.combatTicks >= 20) {
                if ((double) this.actor.getRandom().nextFloat() < 0.3) {
                    boolean bl4 = this.movingToLeft = !this.movingToLeft;
                }
                if ((double) this.actor.getRandom().nextFloat() < 0.3) {
                    this.backward = !this.backward;
                }
                this.combatTicks = 0;
            }
            if (this.combatTicks > -1) {
                if (d > (double)(this.squaredRange * 0.75f)) {
                    this.backward = false;
                } else if (d < (double)(this.squaredRange * 0.25f)) {
                    this.backward = true;
                }
                this.actor.getMoveControl().strafe(this.backward ? -0.5f : 0.5f, this.movingToLeft ? 0.5f : -0.5f);
                Entity entity = this.actor.getControlledVehicle();
                if (entity instanceof Mob) {
                    Mob mobEntity = (Mob)entity;
                    mobEntity.lookAt(livingEntity, 30.0f, 30.0f);
                }
                this.actor.lookAt(livingEntity, 30.0f, 30.0f);
            } else {
                this.actor.getLookControl().setLookAt(livingEntity, 30.0f, 30.0f);
            }
            if (this.actor.isUsingItem()) {
                int i;
                if (!bl && this.targetSeeingTicker < -60) {
                    this.actor.stopUsingItem();
                } else if (bl && (i = this.actor.getTicksUsingItem()) >= 20) {
                    this.actor.stopUsingItem();
                    this.actor.performRangedAttack(livingEntity, BowItem.getPowerForTime(i));
                    this.cooldown = this.attackInterval;
                }
            } else if (--this.cooldown <= 0 && this.targetSeeingTicker >= -60) {
                this.actor.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.actor, Items.BOW));
            }
        }
    }

