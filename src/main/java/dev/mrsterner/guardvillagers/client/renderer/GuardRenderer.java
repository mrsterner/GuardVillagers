package dev.mrsterner.guardvillagers.client.renderer;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.GuardVillagersClient;
import dev.mrsterner.guardvillagers.client.model.GuardArmorModel;
import dev.mrsterner.guardvillagers.client.model.GuardSteveModel;
import dev.mrsterner.guardvillagers.client.model.GuardVillagerModel;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import org.jetbrains.annotations.Nullable;

public class GuardRenderer extends BipedEntityRenderer<GuardEntity, BipedEntityModel<GuardEntity>> {

    private final BipedEntityModel<GuardEntity> steve;
    private final BipedEntityModel<GuardEntity> normal = this.getModel();

    public GuardRenderer(EntityRendererFactory.Context context) {
        super(context, new GuardVillagerModel(context.getPart(GuardVillagersClient.GUARD)), 0.5F);
        this.steve = new GuardSteveModel(context.getPart(GuardVillagersClient.GUARD_STEVE));
        if (GuardVillagers.config.useSteveModel)
            this.model = steve;
        else
            this.model = normal;
        this.addFeature(new ArmorFeatureRenderer<>(this, !GuardVillagers.config.useSteveModel ?
        new GuardArmorModel(context.getPart(GuardVillagersClient.GUARD_ARMOR_INNER)) : new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)), !GuardVillagers.config.useSteveModel ?
        new GuardArmorModel(context.getPart(GuardVillagersClient.GUARD_ARMOR_OUTER)) : new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR))));

    }

    @Override
    public void render(GuardEntity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        this.setModelVisibilities(entityIn);
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    private void setModelVisibilities(GuardEntity entityIn) {
        BipedEntityModel<GuardEntity> guardmodel = this.getModel();
        ItemStack itemstack = entityIn.getMainHandStack();
        ItemStack itemstack1 = entityIn.getOffHandStack();
        guardmodel.setVisible(true);
        BipedEntityModel.ArmPose bipedmodel$armpose = this.getArmPose(entityIn, itemstack, itemstack1,
        Hand.MAIN_HAND);
        BipedEntityModel.ArmPose bipedmodel$armpose1 = this.getArmPose(entityIn, itemstack, itemstack1,
        Hand.OFF_HAND);
        guardmodel.sneaking = entityIn.isSneaking();
        if (entityIn.getMainArm() == Arm.RIGHT) {
            guardmodel.rightArmPose = bipedmodel$armpose;
            guardmodel.leftArmPose = bipedmodel$armpose1;
        } else {
            guardmodel.rightArmPose = bipedmodel$armpose1;
            guardmodel.leftArmPose = bipedmodel$armpose;
        }
    }

    private BipedEntityModel.ArmPose getArmPose(GuardEntity entityIn, ItemStack itemStackMain, ItemStack itemStackOff, Hand handIn) {
        BipedEntityModel.ArmPose bipedmodel$armpose = BipedEntityModel.ArmPose.EMPTY;
        ItemStack itemstack = handIn == Hand.MAIN_HAND ? itemStackMain : itemStackOff;
        if (!itemstack.isEmpty()) {
            bipedmodel$armpose = BipedEntityModel.ArmPose.ITEM;
            if (entityIn.getItemUseTimeLeft() > 0) {
                UseAction useaction = itemstack.getUseAction();
                switch (useaction) {
                    case BLOCK:
                        bipedmodel$armpose = BipedEntityModel.ArmPose.BLOCK;
                        break;
                    case BOW:
                        bipedmodel$armpose = BipedEntityModel.ArmPose.BOW_AND_ARROW;
                        break;
                    case SPEAR:
                        bipedmodel$armpose = BipedEntityModel.ArmPose.THROW_SPEAR;
                        break;
                    case CROSSBOW:
                        if (handIn == entityIn.getActiveHand()) {
                            bipedmodel$armpose = BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
                        }
                        break;
                    default:
                        bipedmodel$armpose = BipedEntityModel.ArmPose.EMPTY;
                        break;
                }
            } else {
                boolean flag1 = itemStackMain.getItem() instanceof CrossbowItem;
                boolean flag2 = itemStackOff.getItem() instanceof CrossbowItem;
                if (flag1 && entityIn.isAttacking()) {
                    bipedmodel$armpose = BipedEntityModel.ArmPose.CROSSBOW_HOLD;
                }

                if (flag2 && itemStackMain.getItem().getUseAction(itemStackMain) == UseAction.NONE
                && entityIn.isAttacking()) {
                    bipedmodel$armpose = BipedEntityModel.ArmPose.CROSSBOW_HOLD;
                }
            }
        }
        return bipedmodel$armpose;
    }

    @Override
    protected void scale(GuardEntity entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Nullable
    @Override
    public Identifier getTexture(GuardEntity entity) {
        return !GuardVillagers.config.useSteveModel
        ? new Identifier(GuardVillagers.MODID,
        "textures/entity/guard/guard_" + entity.getGuardVariant() + ".png")
        : new Identifier(GuardVillagers.MODID,
        "textures/entity/guard/guard_steve_" + entity.getGuardVariant() + ".png");
    }
}