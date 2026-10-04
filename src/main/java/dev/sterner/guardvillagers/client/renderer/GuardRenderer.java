package dev.sterner.guardvillagers.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersClient;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.client.model.*;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

public class GuardRenderer extends HumanoidMobRenderer<GuardEntity, GuardBipedRenderState, HumanoidModel<GuardBipedRenderState>> {

    private final HumanoidModel<GuardBipedRenderState> normal = this.getModel();

    public GuardRenderer(EntityRendererProvider.Context context) {
        super(context, new GuardVillagerModel(context.bakeLayer(GuardVillagersClient.GUARD)), 0.5F);

        HumanoidModel<GuardBipedRenderState> steve = new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER));
        if (GuardVillagersConfig.useSteveModel)
            this.model = steve;
        else
            this.model = normal;

        GuardArmorModel headOuter = new GuardArmorModel(context.bakeLayer(GuardVillagersClient.GUARD_ARMOR_OUTER_HEAD));
        GuardArmorModel chestOuter = new GuardArmorModel(context.bakeLayer(GuardVillagersClient.GUARD_ARMOR_OUTER_CHEST));
        GuardArmorModel feetOuter = new GuardArmorModel(context.bakeLayer(GuardVillagersClient.GUARD_ARMOR_OUTER_FEET));
        GuardArmorModel legsInner = new GuardArmorModel(context.bakeLayer(GuardVillagersClient.GUARD_ARMOR_INNER_LEGS));
        refreshVisibility(headOuter, EquipmentSlot.HEAD);
        refreshVisibility(chestOuter, EquipmentSlot.CHEST);
        refreshVisibility(legsInner, EquipmentSlot.LEGS);
        refreshVisibility(feetOuter, EquipmentSlot.FEET);
        ArmorModelSet<HumanoidModel<GuardBipedRenderState>> adult = new ArmorModelSet(headOuter, chestOuter, legsInner, feetOuter);
        this.addLayer(new HumanoidArmorLayer(this, adult, adult, context.getEquipmentRenderer()));

    }

    private static void refreshVisibility(HumanoidModel<?> m, EquipmentSlot slot) {
        m.head.visible = m.hat.visible = false;
        m.body.visible = m.rightArm.visible = m.leftArm.visible = false;
        m.rightLeg.visible = m.leftLeg.visible = false;
        switch (slot) {
            case HEAD:
                m.head.visible = true;
                m.hat.visible = true;
                break;
            case CHEST:
                m.body.visible = true;
                m.rightArm.visible = true;
                m.leftArm.visible = true;
                break;
            case LEGS:
            case FEET:
                m.rightLeg.visible = true;
                m.leftLeg.visible = true;
        }

    }

    @Override
    public GuardBipedRenderState createRenderState() {
        return new GuardBipedRenderState();
    }

    @Override
    public void extractRenderState(GuardEntity entity, GuardBipedRenderState state, float f) {
        super.extractRenderState(entity, state, f);
        state.guardVariant = entity.getGuardVariant();
        state.isDiscrete = entity.isShiftKeyDown();
        state.mainArm = entity.getMainArm();
        state.mainHandStack = entity.getMainHandItem();
        state.offHandStack = entity.getOffhandItem();
        state.hasRangedWeapon = isRanged(state.mainHandStack) || isRanged(state.offHandStack);
        HumanoidModel.ArmPose mainPose = this.getArmPose(entity, state.mainHandStack, state.offHandStack, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose offPose = this.getArmPose(entity, state.mainHandStack, state.offHandStack, InteractionHand.OFF_HAND);
        if (state.mainArm == HumanoidArm.RIGHT) {
            state.rightArmPose = mainPose;
            state.leftArmPose = offPose;
        } else {
            state.rightArmPose = offPose;
            state.leftArmPose = mainPose;
        }
    }

    private static boolean isRanged(ItemStack s) {
        if (s != null && !s.isEmpty()) {
            Item it = s.getItem();
            return it instanceof BowItem || it instanceof CrossbowItem;
        } else {
            return false;
        }
    }

    private HumanoidModel.ArmPose getArmPose(GuardEntity entityIn, ItemStack itemStackMain, ItemStack itemStackOff, InteractionHand handIn) {
        HumanoidModel.ArmPose bipedmodel$armpose = HumanoidModel.ArmPose.EMPTY;
        ItemStack itemstack = handIn == InteractionHand.MAIN_HAND ? itemStackMain : itemStackOff;
        if (!itemstack.isEmpty()) {
            bipedmodel$armpose = HumanoidModel.ArmPose.ITEM;
            if (entityIn.getUseItemRemainingTicks() > 0) {
                ItemUseAnimation useaction = itemstack.getUseAnimation();
                switch (useaction) {
                    case BLOCK:
                        bipedmodel$armpose = HumanoidModel.ArmPose.BLOCK;
                        break;
                    case BOW:
                        bipedmodel$armpose = HumanoidModel.ArmPose.BOW_AND_ARROW;
                        break;
                    case SPEAR:
                        bipedmodel$armpose = HumanoidModel.ArmPose.THROW_TRIDENT;
                        break;
                    case CROSSBOW:
                        if (handIn == entityIn.getUsedItemHand()) {
                            bipedmodel$armpose = HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                        }
                        break;
                    default:
                        bipedmodel$armpose = HumanoidModel.ArmPose.EMPTY;
                        break;
                }
            } else {
                boolean flag1 = itemStackMain.getItem() instanceof CrossbowItem;
                boolean flag2 = itemStackOff.getItem() instanceof CrossbowItem;
                if (flag1 && entityIn.isAggressive()) {
                    bipedmodel$armpose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
                }

                if (flag2 && itemStackMain.getItem().getUseAnimation(itemStackMain) == ItemUseAnimation.NONE
                        && entityIn.isAggressive()) {
                    bipedmodel$armpose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
                }
            }
        }
        return bipedmodel$armpose;
    }

    @Override
    protected void scale(GuardBipedRenderState state, PoseStack matrices) {
        matrices.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public Identifier getTextureLocation(GuardBipedRenderState state) {
        return !GuardVillagersConfig.useSteveModel
                ? GuardVillagers.id(
                "textures/entity/guard/guard_" + state.guardVariant + ".png")
                : GuardVillagers.id(
                "textures/entity/guard/guard_steve_" + state.guardVariant + ".png");
    }
}