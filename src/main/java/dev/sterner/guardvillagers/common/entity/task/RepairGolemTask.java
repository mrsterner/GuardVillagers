package dev.sterner.guardvillagers.common.entity.task;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;

public class RepairGolemTask extends WorkAtPoi {
    private IronGolem golem;
    private boolean hasStartedHealing;

    public RepairGolemTask() {
        super();
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, Villager owner) {
        List<IronGolem> list = owner.level().getEntitiesOfClass(IronGolem.class, owner.getBoundingBox().inflate(10.0D, 5.0D, 10.0D));
        if (!list.isEmpty()) {
            for (IronGolem golem : list) {
                if (!golem.isInvisible() && golem.isAlive() && golem.getType() == EntityTypes.IRON_GOLEM) {
                    if (golem.getHealth() <= 60.0D || this.hasStartedHealing && golem.getHealth() < golem.getMaxHealth()) {
                        this.golem = golem;
                        owner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void stop(ServerLevel serverLevel, Villager livingEntity, long l) {
        if (golem.getHealth() == golem.getMaxHealth()) {
            this.hasStartedHealing = false;
            livingEntity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    @Override
    protected void start(ServerLevel worldIn, Villager entityIn, long gameTimeIn) {
        if (golem == null)
            return;
        entityIn.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
        this.healGolem(entityIn);
    }

    @Override
    protected void tick(ServerLevel serverLevel, Villager livingEntity, long l) {
        if (golem.getHealth() < golem.getMaxHealth())
            this.healGolem(livingEntity);
    }

    public void healGolem(Villager healer) {
        healer.getNavigation().moveTo(golem, 0.5);
        if (healer.distanceTo(golem) <= 2.0D) {
            this.hasStartedHealing = true;
            healer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
            healer.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            golem.heal(15.0F);
            float pitch = 1.0F + (golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.2F;
            golem.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, pitch);
        }
    }
}