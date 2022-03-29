package dev.mrsterner.guardvillagers.client.screen;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import com.mojang.datafixers.util.Pair;

public class GuardVillagerScreenHandler extends ScreenHandler {
    private final PlayerEntity player;
    public final GuardEntity guardEntity;
    public final Inventory guardInventory;
    private static final EquipmentSlot[] EQUIPMENT_SLOT_ORDER = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public GuardVillagerScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory,
        playerInventory.player.getWorld().getEntityById(buf.readVarInt()) instanceof GuardEntity guard ? guard : null);
    }

    public GuardVillagerScreenHandler(int syncId, PlayerInventory playerInventory, GuardEntity guardEntity) {
        this(syncId, playerInventory, guardEntity.guardInventory, guardEntity);
    }

    public GuardVillagerScreenHandler(int id, PlayerInventory playerInventory, Inventory inventory, GuardEntity guardEntity) {
        super(GuardVillagers.GUARD_SCREEN_HANDLER, id);
        this.guardInventory = inventory;
        this.player = playerInventory.player;
        this.guardEntity = guardEntity;
        inventory.onOpen(playerInventory.player);
        this.addSlot(new Slot(guardInventory, 0, 8, 9) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return EQUIPMENT_SLOT_ORDER[0] == MobEntity.getPreferredEquipmentSlot(stack) && GuardVillagers.hotvChecker(player);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guardEntity.equipStack(EquipmentSlot.HEAD, stack);
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
                return EQUIPMENT_SLOT_ORDER[1] == MobEntity.getPreferredEquipmentSlot(stack) && GuardVillagers.hotvChecker(player);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guardEntity.equipStack(EquipmentSlot.CHEST, stack);
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
                return EQUIPMENT_SLOT_ORDER[2] == MobEntity.getPreferredEquipmentSlot(stack) && GuardVillagers.hotvChecker(player);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guardEntity.equipStack(EquipmentSlot.LEGS, stack);
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
                return EQUIPMENT_SLOT_ORDER[3] == MobEntity.getPreferredEquipmentSlot(stack) && GuardVillagers.hotvChecker(player);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guardEntity.equipStack(EquipmentSlot.FEET, stack);
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
                return GuardVillagers.hotvChecker(player);
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guardEntity.equipStack(EquipmentSlot.OFFHAND, stack);
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
                return GuardVillagers.hotvChecker(player);
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerIn) {
                return GuardVillagers.hotvChecker(playerIn);
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guardEntity.equipStack(EquipmentSlot.MAINHAND, stack);
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
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            int i = this.guardInventory.size();
            if (index < i) {
                if (!this.insertItem(itemstack1, i, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(1).canInsert(itemstack1) && !this.getSlot(1).hasStack()) {
                if (!this.insertItem(itemstack1, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(0).canInsert(itemstack1)) {
                if (!this.insertItem(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (i <= 2 || !this.insertItem(itemstack1, 2, i, false)) {
                int j = i + 27;
                int k = j + 9;
                if (index >= j && index < k) {
                    if (!this.insertItem(itemstack1, i, j, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= i && index < j) {
                    if (!this.insertItem(itemstack1, j, k, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.insertItem(itemstack1, j, j, false)) {
                    return ItemStack.EMPTY;
                }

                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return itemstack;

    }

    @Override
    public void close(PlayerEntity player) {
        super.close(player);
        this.guardInventory.onClose(player);
        this.guardEntity.interacting = false;
    }


}
