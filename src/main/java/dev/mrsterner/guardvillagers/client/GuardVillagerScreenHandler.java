package dev.mrsterner.guardvillagers.client;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.common.IMerchant;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import dev.mrsterner.guardvillagers.common.registy.GuardVillagersScreenHandlers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.Nullable;

public class GuardVillagerScreenHandler extends ScreenHandler {
    public final Inventory guardInventory;
    public final GuardEntity guard;
    private static final EquipmentSlot[] EQUIPMENT_SLOT_ORDER = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};


    public GuardVillagerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleIMerchant(playerInventory.player));
    }

    public GuardVillagerScreenHandler(int id, PlayerInventory playerInventory, IMerchant guard) {
        super(GuardVillagersScreenHandlers.GUARD_SCREEN_HANDLER, id);
        this.guardInventory = guard.getGuard().guardInventory;
        this.guard = guard.getGuard();
        //guardInventory.onOpen(playerInventory.player);
        this.addSlot(new Slot(guardInventory, 0, 8, 9) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return EQUIPMENT_SLOT_ORDER[0] == MobEntity.getPreferredEquipmentSlot(stack) && GuardVillagers.hotvChecker(guard.getCurrentCustomer());
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guard.getGuard().equipStack(EquipmentSlot.HEAD, stack);
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
                return EQUIPMENT_SLOT_ORDER[1] == MobEntity.getPreferredEquipmentSlot(stack) && GuardVillagers.hotvChecker(guard.getCurrentCustomer());
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guard.getGuard().equipStack(EquipmentSlot.CHEST, stack);
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
                return EQUIPMENT_SLOT_ORDER[2] == MobEntity.getPreferredEquipmentSlot(stack) && GuardVillagers.hotvChecker(guard.getCurrentCustomer());
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guard.getGuard().equipStack(EquipmentSlot.LEGS, stack);
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerIn) {
                return GuardVillagers.hotvChecker(guard.getCurrentCustomer());
            }

            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE);
            }
        });
        this.addSlot(new Slot(guardInventory, 3, 8, 62) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return EQUIPMENT_SLOT_ORDER[3] == MobEntity.getPreferredEquipmentSlot(stack) && GuardVillagers.hotvChecker(guard.getCurrentCustomer());
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guard.getGuard().equipStack(EquipmentSlot.FEET, stack);
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
                return GuardVillagers.hotvChecker(guard.getCurrentCustomer());
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guard.getGuard().equipStack(EquipmentSlot.OFFHAND, stack);
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
                return GuardVillagers.hotvChecker(guard.getCurrentCustomer());
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerIn) {
                return GuardVillagers.hotvChecker(playerIn);
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);
                guard.getGuard().equipStack(EquipmentSlot.MAINHAND, stack);
            }
        });
        for (int l = 0; l < 3; ++l) {
            for (int j1 = 0; j1 < 9; ++j1) {
                this.addSlot(new Slot(guard.getCurrentCustomer().getInventory(), j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18));
            }
        }

        for (int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(guard.getCurrentCustomer().getInventory(), i1, 8 + i1 * 18, 142));
        }
    }


    public static class SimpleIMerchant implements IMerchant {
        private final PlayerEntity player;
        private GuardEntity guardEntity;

        public SimpleIMerchant(PlayerEntity player) {
            this.player = player;
        }

        @Override
        public GuardEntity getGuard() {
            return guardEntity;
        }

        @Override
        public void setCurrentCustomer(@Nullable PlayerEntity customer) {

        }

        @Override
        public @Nullable PlayerEntity getCurrentCustomer() {
            return null;
        }


        @Override
        public boolean isClient() {
            return this.player.getWorld().isClient;
        }

        @Override
        @Environment(EnvType.CLIENT)
        public void setGuardClientside(GuardEntity guardEntity) {
            this.guardEntity = guardEntity;
        }
    }


    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
