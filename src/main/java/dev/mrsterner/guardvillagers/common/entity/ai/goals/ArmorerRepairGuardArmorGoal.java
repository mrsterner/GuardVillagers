package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.village.VillagerProfession;

import java.util.List;

public class ArmorerRepairGuardArmorGoal extends Goal {
    private final GuardEntity guard;
    private VillagerEntity villager;

    public ArmorerRepairGuardArmorGoal(GuardEntity guard) {
        this.guard = guard;
    }

    @Override
    public boolean canStart() {
        List<VillagerEntity> list = this.guard.world.getNonSpectatingEntities(VillagerEntity.class, this.guard.getBoundingBox().expand(10.0D, 3.0D, 10.0D));
        if (!list.isEmpty()) {
            for (VillagerEntity mob : list) {
                if (mob != null) {
                    boolean isArmorerOrWeaponSmith = mob.getVillagerData().getProfession() == VillagerProfession.ARMORER || mob.getVillagerData().getProfession() == VillagerProfession.WEAPONSMITH;
                    if (isArmorerOrWeaponSmith && guard.getTarget() == null) {
                        if (mob.getVillagerData().getProfession() == VillagerProfession.ARMORER) {
                            for (int i = 0; i < guard.guardInventory.size() - 2; ++i) {
                                ItemStack itemstack = guard.guardInventory.getStack(i);
                                if (itemstack.isDamaged() && itemstack.getItem() instanceof ArmorItem && itemstack.getDamage() >= itemstack.getMaxDamage() / 2) {
                                    this.villager = mob;
                                    return true;
                                }
                            }
                        }
                        if (mob.getVillagerData().getProfession() == VillagerProfession.WEAPONSMITH) {
                            for (int i = 4; i < 6; ++i) {
                                ItemStack itemstack = guard.guardInventory.getStack(i);
                                if (itemstack.isDamaged() && itemstack.getDamage() >= itemstack.getMaxDamage() / 2) {
                                    this.villager = mob;
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
        guard.getLookControl().lookAt(villager, 30.0F, 30.0F);
        if (guard.distanceTo(villager) >= 2.0D) {
            guard.getNavigation().startMovingTo(villager, 0.5D);
            villager.getNavigation().startMovingTo(guard, 0.5D);
        } else {
            VillagerProfession profession = villager.getVillagerData().getProfession();
            if (profession == VillagerProfession.ARMORER) {
                for (int i = 0; i < guard.guardInventory.size() - 2; ++i) {
                    ItemStack itemstack = guard.guardInventory.getStack(i);
                    if (itemstack.isDamaged() && itemstack.getItem() instanceof ArmorItem && itemstack.getDamage() >= itemstack.getMaxDamage() / 2 + guard.getRandom().nextInt(5)) {
                        itemstack.setDamage(itemstack.getDamage() - guard.getRandom().nextInt(5));
                    }
                }
            }
            if (profession == VillagerProfession.WEAPONSMITH) {
                for (int i = 4; i < 6; ++i) {
                    ItemStack itemstack = guard.guardInventory.getStack(i);
                    if (itemstack.isDamaged() && itemstack.getDamage() >= itemstack.getMaxDamage() / 2 + guard.getRandom().nextInt(5)) {
                        itemstack.setDamage(itemstack.getDamage() - guard.getRandom().nextInt(5));
                    }
                }
            }
        }
    }
}