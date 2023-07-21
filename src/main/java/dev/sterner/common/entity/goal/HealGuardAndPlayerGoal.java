package dev.sterner.common.entity.goal;

import dev.sterner.GuardVillagers;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.VillagerProfession;

import java.util.EnumSet;
import java.util.List;

public class HealGuardAndPlayerGoal extends Goal {
    private final MobEntity healer;
    private LivingEntity mob;
    private int rangedAttackTime = -1;
    private final double entityMoveSpeed;
    private int seeTime;
    private final int attackIntervalMin;
    private final int maxRangedAttackTime;
    private final float attackRadius;
    private final float maxAttackDistance;
    protected final TargetPredicate predicate = TargetPredicate.createNonAttackable().setBaseMaxDistance(64.0D);

    public HealGuardAndPlayerGoal(MobEntity healer, double movespeed, int attackIntervalMin, int maxAttackTime, float maxAttackDistanceIn) {
        this.healer = healer;
        this.entityMoveSpeed = movespeed;
        this.attackIntervalMin = attackIntervalMin;
        this.maxRangedAttackTime = maxAttackTime;
        this.attackRadius = maxAttackDistanceIn;
        this.maxAttackDistance = maxAttackDistanceIn * maxAttackDistanceIn;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (((VillagerEntity) this.healer).getVillagerData().getProfession() != VillagerProfession.CLERIC || this.healer.isSleeping()) {
            return false;
        }
        List<LivingEntity> list = this.healer.getWorld().getNonSpectatingEntities(LivingEntity.class, this.healer.getBoundingBox().expand(10.0D, 3.0D, 10.0D));
        if (!list.isEmpty()) {
            for (LivingEntity mob : list) {
                if (mob != null) {
                    if (mob instanceof VillagerEntity && mob.isAlive() && mob.getHealth() < mob.getMaxHealth() && mob != healer || mob.getType() == GuardVillagers.GUARD_VILLAGER && mob != null && mob.isAlive() && mob.getHealth() < mob.getMaxHealth()
                            || mob instanceof PlayerEntity && mob.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && !((PlayerEntity) mob).getAbilities().creativeMode && mob.getHealth() < mob.getMaxHealth()) {
                        this.mob = mob;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return this.canStart() && mob != null && mob.getHealth() < mob.getMaxHealth();
    }

    @Override
    public void stop() {
        this.mob = null;
        this.seeTime = 0;
        this.healer.getBrain().forget(MemoryModuleType.LOOK_TARGET);
        this.rangedAttackTime = 0;
    }

    @Override
    public void tick() {
        if (mob == null)
            return;
        double d0 = this.healer.squaredDistanceTo(this.mob.getX(), this.mob.getY(), this.mob.getZ());
        boolean flag = this.healer.getVisibilityCache().canSee(mob);
        if (flag) {
            ++this.seeTime;
        } else {
            this.seeTime = 0;
        }
        LookTargetUtil.lookAt(healer, mob);
        if (!(d0 > (double) this.maxAttackDistance) && this.seeTime >= 5) {
            this.healer.getNavigation().stop();
        } else {
            this.healer.getNavigation().startMovingTo(this.healer, this.entityMoveSpeed);
        }
        if (mob.distanceTo(healer) <= 3.0D) {
            healer.getMoveControl().strafeTo(-0.5F, 0);
        }
        if (--this.rangedAttackTime == 0) {
            if (!flag) {
                return;
            }
            float f = this.attackRadius;
            float distanceFactor = MathHelper.clamp(f, 0.10F, 0.10F);
            this.throwPotion(mob, distanceFactor);
            this.rangedAttackTime = MathHelper.floor(f * (float) (this.maxRangedAttackTime - this.attackIntervalMin) + (float) this.attackIntervalMin);
        } else if (this.rangedAttackTime < 0) {
            this.rangedAttackTime = MathHelper.floor(MathHelper.lerp(Math.sqrt(d0) / (double) this.attackRadius, this.attackIntervalMin, this.maxAttackDistance));
        }
    }

    public void throwPotion(LivingEntity target, float distanceFactor) {
        Vec3d vec3d = target.getVelocity();
        double d0 = target.getX() + vec3d.x - healer.getX();
        double d1 = target.getEyeY() - (double) 1.1F - healer.getY();
        double d2 = target.getZ() + vec3d.z - healer.getZ();
        float f = MathHelper.sqrt((float) (d0 * d0 + d2 * d2));
        Potion potion = Potions.REGENERATION;
        if (target.getHealth() <= 4.0F) {
            potion = Potions.HEALING;
        } else {
            potion = Potions.REGENERATION;
        }
        PotionEntity potionentity = new PotionEntity(healer.getWorld(), healer);
        potionentity.setItem(PotionUtil.setPotion(new ItemStack(Items.SPLASH_POTION), potion));
        potionentity.setPitch(-20.0F);
        potionentity.setVelocity(d0, d1 + (double) (f * 0.2F), d2, 0.75F, 8.0F);
        healer.getWorld().playSound(null, healer.getX(), healer.getY(), healer.getZ(), SoundEvents.ENTITY_SPLASH_POTION_THROW, healer.getSoundCategory(), 1.0F, 0.8F + healer.getRandom().nextFloat() * 0.4F);
        healer.getWorld().spawnEntity(potionentity);
    }
}