package dev.sterner.guardvillagers.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

public class GuardSteveModel extends PlayerModel {
    public GuardSteveModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setupAnim(AvatarRenderState playerEntityRenderState) {
        super.setupAnim(playerEntityRenderState);

        if (playerEntityRenderState instanceof GuardPlayerRenderState state) {
            if (state.kickTicks > 0) {
                float f1 = 1.0F - (float) Mth.abs(10 - 2 * state.kickTicks) / 10.0F;
                this.rightLeg.xRot = Mth.lerp(f1, this.rightLeg.xRot, -1.40F);
            }

            var ageInTicks = state.ageInTicks;
            if (state.mainArm == HumanoidArm.RIGHT) {
                this.eatingAnimationRightHand(InteractionHand.MAIN_HAND, state, ageInTicks);
                this.eatingAnimationLeftHand(InteractionHand.OFF_HAND, state, ageInTicks);
            } else {
                this.eatingAnimationRightHand(InteractionHand.OFF_HAND, state, ageInTicks);
                this.eatingAnimationLeftHand(InteractionHand.MAIN_HAND, state, ageInTicks);
            }
        }
    }

    public static LayerDefinition createMesh() {
        MeshDefinition meshdefinition = PlayerModel.createMesh(CubeDeformation.NONE, false);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public void eatingAnimationRightHand(InteractionHand hand, GuardPlayerRenderState entity, float ageInTicks) {
        ItemStack itemstack = entity.getStackInHand(hand);
        boolean drinkingoreating = itemstack.getUseAnimation() == ItemUseAnimation.EAT
                || itemstack.getUseAnimation() == ItemUseAnimation.DRINK;
        if (entity.isEating && drinkingoreating
                || entity.itemUseTimeLeft > 0 && drinkingoreating && entity.useItemHand == hand) {
            this.rightArm.yRot = -0.5F;
            this.rightArm.xRot = -1.3F;
            this.rightArm.zRot = Mth.cos(ageInTicks) * 0.1F;
            this.head.xRot = Mth.cos(ageInTicks) * 0.2F;
            this.head.yRot = 0.0F;
            this.hat.loadPose(head.storePose());
        }
    }

    public void eatingAnimationLeftHand(InteractionHand hand, GuardPlayerRenderState entity, float ageInTicks) {
        ItemStack itemstack = entity.getStackInHand(hand);
        boolean drinkingoreating = itemstack.getUseAnimation() == ItemUseAnimation.EAT
                || itemstack.getUseAnimation() == ItemUseAnimation.DRINK;
        if (entity.isEating && drinkingoreating
                || entity.itemUseTimeLeft > 0 && drinkingoreating && entity.useItemHand == hand) {
            this.leftArm.yRot = 0.5F;
            this.leftArm.xRot = -1.3F;
            this.leftArm.zRot = Mth.cos(ageInTicks) * 0.1F;
            this.head.xRot = Mth.cos(ageInTicks) * 0.2F;
            this.head.yRot = 0.0F;
            this.hat.loadPose(head.storePose());
        }
    }
}