package dev.mrsterner.guardvillagers.client.screen;

import dev.mrsterner.guardvillagers.GuardVillagers;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import com.mojang.datafixers.util.Pair;

public class GuardVillagerScreenHandler extends ScreenHandler {
    public Inventory guardInventory;
    private static final EquipmentSlot[] EQUIPMENT_SLOT_ORDER = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};



    public GuardVillagerScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, new SimpleInventory(12));
    }


    public GuardVillagerScreenHandler(int id, PlayerInventory playerInventory, Inventory inventory) {
        super(GuardVillagers.GUARD_SCREEN_HANDLER, id);
        this.guardInventory = inventory;
        //this.guard = guard;
        inventory.onOpen(playerInventory.player);
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
                //guard.equipStack(EquipmentSlot.HEAD, stack);
            }

                @Override
            public boolean canTakeItems(PlayerEntity playerIn) {
                return GuardVillagers.hotvChecker(playerIn);
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
                //guard.equipStack(EquipmentSlot.CHEST, stack);
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerIn) {
                return GuardVillagers.hotvChecker(playerIn);
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
                //guard.equipStack(EquipmentSlot.LEGS, stack);
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
                //guard.equipStack(EquipmentSlot.FEET, stack);
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerIn) {
                return GuardVillagers.hotvChecker(playerIn);
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
                //guard.equipStack(EquipmentSlot.OFFHAND, stack);
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerIn) {
                return GuardVillagers.hotvChecker(playerIn);
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
                //guard.equipStack(EquipmentSlot.MAINHAND, stack);
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

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (index < this.guardInventory.size()) {
                if (!this.insertItem(originalStack, this.guardInventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.guardInventory.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;

    }
}