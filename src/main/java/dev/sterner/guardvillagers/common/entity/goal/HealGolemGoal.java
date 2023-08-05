package dev.sterner.guardvillagers.common.entity.goal;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.village.VillagerProfession;

import java.util.EnumSet;
import java.util.List;

public class HealGolemGoal extends Goal {
    public final MobEntity healer;
    public IronGolemEntity golem;
    public boolean hasStartedHealing;

    public HealGolemGoal(MobEntity mob) {
        healer = mob;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (((VillagerEntity) this.healer).getVillagerData().getProfession() != VillagerProfession.WEAPONSMITH && (((VillagerEntity) this.healer).getVillagerData().getProfession() != VillagerProfession.TOOLSMITH)
                && (((VillagerEntity) this.healer).getVillagerData().getProfession() != VillagerProfession.ARMORER) || this.healer.isSleeping()) {
            return false;
        }
        List<IronGolemEntity> list = this.healer.getWorld().getNonSpectatingEntities(IronGolemEntity.class, this.healer.getBoundingBox().expand(10.0D));
        if (!list.isEmpty()) {
            for (IronGolemEntity golem : list) {
                if (!golem.isInvisible() && golem.isAlive() && golem.getType() == EntityType.IRON_GOLEM) {
                    if (golem.getHealth() <= 60.0D || this.hasStartedHealing && golem.getHealth() < golem.getMaxHealth()) {
                        healer.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
                        this.golem = golem;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void stop() {
        healer.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.hasStartedHealing = false;
        super.stop();
    }

    @Override
    public void start() {
        if (golem == null)
            return;
        healer.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
        this.healGolem();
    }

    @Override
    public void tick() {
        if (golem.getHealth() < golem.getMaxHealth()) {
            this.healGolem();
        }
    }

    public void healGolem() {
        healer.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
        healer.getNavigation().startMovingTo(golem, 0.5);
        if (healer.distanceTo(golem) <= 2.0D) {
            this.hasStartedHealing = true;
            healer.swingHand(Hand.MAIN_HAND);
            golem.heal(15.0F);
            float f1 = 1.0F + (golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.2F;
            golem.playSound(SoundEvents.ENTITY_IRON_GOLEM_REPAIR, 1.0F, f1);
        }
    }

}