package dev.mrsterner.guardvillagers.client.model;

import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;

public class GuardSteveModel extends PlayerEntityModel<GuardEntity> {
    public GuardSteveModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setAngles(GuardEntity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netbipedHeadYaw, float bipedHeadPitch) {
        super.setAngles(entityIn, limbSwing, limbSwingAmount, ageInTicks, netbipedHeadYaw, bipedHeadPitch);
        if (entityIn.getKickTicks() > 0) {
            float f1 = 1.0F - (float) MathHelper.abs(10 - 2 * entityIn.getKickTicks()) / 10.0F;
            this.rightLeg.pitch = MathHelper.lerp(f1, this.rightLeg.pitch, -1.40F);
        }
        if (entityIn.getMainArm() == Arm.RIGHT) {
            this.eatingAnimationRightHand(Hand.MAIN_HAND, entityIn, ageInTicks);
            this.eatingAnimationLeftHand(Hand.OFF_HAND, entityIn, ageInTicks);
        } else {
            this.eatingAnimationRightHand(Hand.OFF_HAND, entityIn, ageInTicks);
            this.eatingAnimationLeftHand(Hand.MAIN_HAND, entityIn, ageInTicks);
        }
    }

    public static TexturedModelData createMesh() {
        ModelData meshdefinition = PlayerEntityModel.getTexturedModelData(Dilation.NONE, false);
        return TexturedModelData.of(meshdefinition, 64, 64);
    }

    public void eatingAnimationRightHand(Hand hand, GuardEntity entity, float ageInTicks) {
        ItemStack itemstack = entity.getStackInHand(hand);
        boolean drinkingoreating = itemstack.getUseAction() == UseAction.EAT
        || itemstack.getUseAction() == UseAction.DRINK;
        if (entity.isEating() && drinkingoreating
        || entity.getItemUseTimeLeft() > 0 && drinkingoreating && entity.getActiveHand() == hand) {
            this.rightArm.yaw = -0.5F;
            this.rightArm.pitch = -1.3F;
            this.rightArm.roll = MathHelper.cos(ageInTicks) * 0.1F;
            this.head.pitch = MathHelper.cos(ageInTicks) * 0.2F;
            this.head.yaw = 0.0F;
            this.hat.copyTransform(head);
        }
    }

    public void eatingAnimationLeftHand(Hand hand, GuardEntity entity, float ageInTicks) {
        ItemStack itemstack = entity.getStackInHand(hand);
        boolean drinkingoreating = itemstack.getUseAction() == UseAction.EAT
        || itemstack.getUseAction() == UseAction.DRINK;
        if (entity.isEating() && drinkingoreating
        || entity.getItemUseTimeLeft() > 0 && drinkingoreating && entity.getActiveHand() == hand) {
            this.leftArm.yaw = 0.5F;
            this.leftArm.pitch = -1.3F;
            this.leftArm.roll = MathHelper.cos(ageInTicks) * 0.1F;
            this.head.pitch = MathHelper.cos(ageInTicks) * 0.2F;
            this.head.yaw = 0.0F;
            this.hat.copyTransform(head);
        }
    }
}
