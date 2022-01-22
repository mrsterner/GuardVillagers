package dev.mrsterner.guardvillagers.client;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.GuardVillagersConfig;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.Nullable;

public class GuardVillagerScreenHandler extends ScreenHandler {
    private final Inventory guardInventory;
    private final GuardEntity guard;
    private static final EquipmentSlot[] EQUIPMENT_SLOT_ORDER = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};




    public GuardVillagerScreenHandler(int id, PlayerInventory playerInventory, Inventory guardInventory, final GuardEntity guard) {
        super((ScreenHandlerType<?>) null, id);
        this.guardInventory = guardInventory;
        this.guard = guard;
        guardInventory.onOpen(playerInventory.player);
        this.addSlot(new Slot(guardInventory, 0, 8, 9) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return EQUIPMENT_SLOT_ORDER[0] == MobEntity.getPreferredEquipmentSlot(stack) && GuardVillagers.hotvChecker(playerInventory.player);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guard.equipStack(EquipmentSlot.HEAD, stack);
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerIn) {
                return GuardVillagers.hotvChecker(playerInventory.player);
            }

            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PlayerScreenHandler.EMPTY_HELMET_SLOT_TEXTURE);
            }
        });
        this.addSlot(new Slot(guardInventory, 1, 8, 26) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return EQUIPMENT_SLOT_ORDER[1] == MobEntity.getPreferredEquipmentSlot(stack) && GuardVillagers.hotvChecker(playerInventory.player);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guard.equipStack(EquipmentSlot.CHEST, stack);
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerIn) {
                return GuardVillagers.hotvChecker(playerInventory.player);
            }

            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PlayerScreenHandler.EMPTY_CHESTPLATE_SLOT_TEXTURE);
            }
        });
        this.addSlot(new Slot(guardInventory, 2, 8, 44) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return EQUIPMENT_SLOT_ORDER[2] == MobEntity.getPreferredEquipmentSlot(stack) && GuardVillagers.hotvChecker(playerInventory.player);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guard.equipStack(EquipmentSlot.LEGS, stack);
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerIn) {
                return GuardVillagers.hotvChecker(playerInventory.player);
            }

            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE);
            }
        });
        this.addSlot(new Slot(guardInventory, 3, 8, 62) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return EQUIPMENT_SLOT_ORDER[3] == MobEntity.getPreferredEquipmentSlot(stack) && GuardVillagers.hotvChecker(playerInventory.player);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guard.equipStack(EquipmentSlot.FEET, stack);
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerIn) {
                return GuardVillagers.hotvChecker(playerInventory.player);
            }

            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PlayerScreenHandler.EMPTY_BOOTS_SLOT_TEXTURE);
            }
        });
        this.addSlot(new Slot(guardInventory, 4, 77, 62) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return GuardVillagers.hotvChecker(playerInventory.player);
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guard.equipStack(EquipmentSlot.OFFHAND, stack);
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerIn) {
                return GuardVillagers.hotvChecker(playerInventory.player);
            }

            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PlayerScreenHandler.EMPTY_OFFHAND_ARMOR_SLOT);
            }
        });

        this.addSlot(new Slot(guardInventory, 5, 77, 44) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return GuardVillagers.hotvChecker(playerInventory.player);
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerIn) {
                return GuardVillagers.hotvChecker(playerIn);
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guard.equipStack(EquipmentSlot.MAINHAND, stack);
            }
        });
        for (int l = 0; l < 3; ++l) {
            for (int j1 = 0; j1 < 9; ++j1) {
                this.addSlot(new Slot(playerInventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18));
            }
        }

        for (int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(playerInventory, i1, 8 + i1 * 18, 142));
        }
    }



    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
