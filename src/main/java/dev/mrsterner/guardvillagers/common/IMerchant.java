package dev.mrsterner.guardvillagers.common;

import dev.mrsterner.guardvillagers.client.GuardVillagerScreenHandler;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import dev.mrsterner.guardvillagers.common.registy.GuardVillagersScreenHandlers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

public interface IMerchant {

    GuardEntity getGuard();

    void setCurrentCustomer(@Nullable PlayerEntity customer);

    @Environment(EnvType.CLIENT)
    default void setGuardClientside(GuardEntity trader) {
    }

    @Nullable
    PlayerEntity getCurrentCustomer();
/*
    default void sendOffers(PlayerEntity player, Text test) {
        OptionalInt optionalInt = player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, playerx) -> {
            return new GuardVillagerScreenHandler(syncId, playerInventory, this);
        }, test));
    }

 */
    boolean isClient();
}
