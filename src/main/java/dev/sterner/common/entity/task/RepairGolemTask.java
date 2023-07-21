package dev.sterner.common.entity.task;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.brain.task.VillagerWorkTask;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import java.util.List;

public class RepairGolemTask extends VillagerWorkTask {
    private IronGolemEntity golem;
    private boolean hasStartedHealing;

    public RepairGolemTask() {
        super();
    }

    @Override
    protected boolean shouldRun(ServerWorld worldIn, VillagerEntity owner) {
        List<IronGolemEntity> list = owner.getWorld().getNonSpectatingEntities(IronGolemEntity.class, owner.getBoundingBox().expand(10.0D, 5.0D, 10.0D));
        if (!list.isEmpty()) {
            for (IronGolemEntity golem : list) {
                if (!golem.isInvisible() && golem.isAlive() && golem.getType() == EntityType.IRON_GOLEM) {
                    if (golem.getHealth() <= 60.0D || this.hasStartedHealing && golem.getHealth() < golem.getMaxHealth()) {
                        this.golem = golem;
                        owner.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void finishRunning(ServerWorld worldIn, VillagerEntity entityIn, long gameTimeIn) {
        if (golem.getHealth() == golem.getMaxHealth()) {
            this.hasStartedHealing = false;
            entityIn.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    @Override
    protected void run(ServerWorld worldIn, VillagerEntity entityIn, long gameTimeIn) {
        if (golem == null)
            return;
        entityIn.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
        this.healGolem(entityIn);
    }

    @Override
    protected void keepRunning(ServerWorld worldIn, VillagerEntity entityIn, long gameTimeIn) {
        if (golem.getHealth() < golem.getMaxHealth())
            this.healGolem(entityIn);
    }

    public void healGolem(VillagerEntity healer) {
        healer.getNavigation().startMovingTo(golem, 0.5);
        if (healer.distanceTo(golem) <= 2.0D) {
            this.hasStartedHealing = true;
            healer.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
            healer.swingHand(Hand.MAIN_HAND);
            golem.heal(15.0F);
            float pitch = 1.0F + (golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.2F;
            golem.playSound(SoundEvents.ENTITY_IRON_GOLEM_REPAIR, 1.0F, pitch);
        }
    }
}