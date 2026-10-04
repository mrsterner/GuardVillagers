package dev.sterner.guardvillagers.client.model;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class GuardBipedRenderState extends HumanoidRenderState {
    public int kickTicks;
    public boolean hasRangedWeapon;
    public int guardVariant;
    public ItemStack mainHandStack;
    public ItemStack offHandStack;
    public boolean isEating;
    public int itemUseTimeLeft;
    public InteractionHand activeHand;

    public GuardBipedRenderState() {
        this.mainHandStack = ItemStack.EMPTY;
        this.offHandStack = ItemStack.EMPTY;
    }

    public ItemStack getStackInHand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? this.mainHandStack : this.offHandStack;
    }
}
