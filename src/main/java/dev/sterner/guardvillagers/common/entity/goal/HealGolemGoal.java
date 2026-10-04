package dev.sterner.guardvillagers.common.entity.goal;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;

public class HealGolemGoal extends Goal {
    public final Mob healer;
    public IronGolem golem;
    public boolean hasStartedHealing;

    public HealGolemGoal(Mob mob) {
        healer = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!((Villager) this.healer).getVillagerData().profession().is(VillagerProfession.WEAPONSMITH) && !(((Villager) this.healer).getVillagerData().profession().is(VillagerProfession.TOOLSMITH))
                && !(((Villager) this.healer).getVillagerData().profession().is(VillagerProfession.ARMORER)) || this.healer.isSleeping()) {
            return false;
        }
        List<IronGolem> list = this.healer.level().getEntitiesOfClass(IronGolem.class, this.healer.getBoundingBox().inflate(10.0D));
        if (!list.isEmpty()) {
            for (IronGolem golem : list) {
                if (!golem.isInvisible() && golem.isAlive() && golem.getType() == EntityTypes.IRON_GOLEM) {
                    if (golem.getHealth() <= 60.0D || this.hasStartedHealing && golem.getHealth() < golem.getMaxHealth()) {
                        healer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
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
        healer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.hasStartedHealing = false;
        super.stop();
    }

    @Override
    public void start() {
        if (golem == null)
            return;
        healer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
        this.healGolem();
    }

    @Override
    public void tick() {
        if (golem.getHealth() < golem.getMaxHealth()) {
            this.healGolem();
        }
    }

    public void healGolem() {
        healer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
        healer.getNavigation().moveTo(golem, 0.5);
        if (healer.distanceTo(golem) <= 2.0D) {
            this.hasStartedHealing = true;
            healer.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            golem.heal(15.0F);
            float f1 = 1.0F + (golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.2F;
            golem.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, f1);
        }
    }

}