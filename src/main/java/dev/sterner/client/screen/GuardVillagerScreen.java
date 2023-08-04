package dev.sterner.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sterner.GuardVillagers;
import dev.sterner.GuardVillagersConfig;
import dev.sterner.common.entity.GuardEntity;
import dev.sterner.common.screenhandler.GuardVillagerScreenHandler;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class GuardVillagerScreen extends HandledScreen<GuardVillagerScreenHandler> {

    public static final Identifier ID = new Identifier(GuardVillagers.MODID, "guard_follow");
    public static final Identifier ID_2 = new Identifier(GuardVillagers.MODID, "guard_patroll");
    private static final Identifier GUARD_GUI_TEXTURES = new Identifier(GuardVillagers.MODID, "textures/gui/inventory.png");
    private static final Identifier GUARD_FOLLOWING_ICON = new Identifier(GuardVillagers.MODID, "textures/gui/following_icons.png");
    private static final Identifier GUARD_NOT_FOLLOWING_ICON = new Identifier(GuardVillagers.MODID, "textures/gui/not_following_icons.png");
    private static final Identifier PATROL_ICON = new Identifier(GuardVillagers.MODID, "textures/gui/patrollingui.png");
    private static final Identifier NOT_PATROLLING_ICON = new Identifier(GuardVillagers.MODID, "textures/gui/notpatrollingui.png");

    private static final Identifier ICONS = new Identifier("textures/gui/icons.png");
    private final PlayerEntity player;
    private final GuardEntity guardEntity;
    private float mousePosX;
    private float mousePosY;
    private boolean buttonPressed;

    public GuardVillagerScreen(GuardVillagerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, handler.guardEntity.getDisplayName());
        this.titleX = 80;
        this.playerInventoryTitleX = 100;
        this.player = inventory.player;
        guardEntity = handler.guardEntity;
    }

    @Override
    protected void init() {
        super.init();
        if (!GuardVillagersConfig.followHero || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
            this.addDrawableChild(new GuardGuiButton(this.x + 100, this.height / 2 - 40, 20, 18, 0, 0, 19, GUARD_FOLLOWING_ICON, GUARD_NOT_FOLLOWING_ICON, true,
                    (button) -> {
                        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                        buf.writeInt(guardEntity.getId());
                        buf.writeBoolean(buttonPressed);
                        ClientPlayNetworking.send(ID, buf);
                    })
            );
        }
        if (!GuardVillagersConfig.setGuardPatrolHotv || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
            this.addDrawableChild(new GuardGuiButton(this.x + 120, this.height / 2 - 40, 20, 18, 0, 0, 19, PATROL_ICON, NOT_PATROLLING_ICON, false,
                    (button) -> {
                        buttonPressed = !buttonPressed;
                        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                        buf.writeInt(guardEntity.getId());
                        buf.writeBoolean(buttonPressed);
                        ClientPlayNetworking.send(ID_2, buf);
                    })
            );
        }
    }

    @Override
    protected void drawBackground(MatrixStack matrixStack, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUARD_GUI_TEXTURES);
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(matrixStack, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
        InventoryScreen.drawEntity(i + 51, j + 75, 30, (float) (i + 51) - this.mousePosX, (float) (j + 75 - 50) - this.mousePosY, this.guardEntity);
    }

    @Override
    protected void drawForeground(MatrixStack matrixStack, int x, int y) {
        super.drawForeground(matrixStack, x, y);
        int health = MathHelper.ceil(guardEntity.getHealth());
        int armor = guardEntity.getArmor();
        RenderSystem.setShaderTexture(0, GUI_ICONS_TEXTURE);
        int statusU = guardEntity.hasStatusEffect(StatusEffects.POISON) ? 4 : 0;
        //Health
        for (int i = 0; i < 10; i++) {
            this.drawTexture(matrixStack, (i * 8) + 80, 20, 16, 0, 9, 9);
        }
        for (int i = 0; i < health / 2; i++) {
            if (health % 2 != 0 && health / 2 == i + 1) {
                this.drawTexture(matrixStack, (i * 8) + 80, 20, 16 + 9 * (4 + statusU), 0, 9, 9);
                this.drawTexture(matrixStack, ((i + 1) * 8) + 80, 20, 16 + 9 * (5 + statusU), 0, 9, 9);
            } else {
                this.drawTexture(matrixStack, (i * 8) + 80, 20, 16 + 9 * (4 + statusU), 0, 9, 9);
            }
        }
        //Armor
        for (int i = 0; i < 10; i++) {
            this.drawTexture(matrixStack, (i * 8) + 80, 30, 16, 9, 9, 9);
        }
        for (int i = 0; i < armor / 2; i++) {
            if (armor % 2 != 0 && armor / 2 == i + 1) {
                this.drawTexture(matrixStack, (i * 8) + 80, 30, 16 + 9 * 2, 9, 9, 9);
                this.drawTexture(matrixStack, ((i + 1) * 8) + 80, 30, 16 + 9, 9, 9, 9);
            } else {
                this.drawTexture(matrixStack, (i * 8) + 80, 30, 16 + 9 * 2, 9, 9, 9);
            }
        }

    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        this.mousePosX = (float) mouseX;
        this.mousePosY = (float) mouseY;
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(matrixStack, mouseX, mouseY);
    }


    class GuardGuiButton extends TexturedButtonWidget {
        private final Identifier texture;
        private final Identifier newTexture;
        private final boolean isFollowButton;

        public GuardGuiButton(int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn, int yDiffTextIn, Identifier resourceLocationIn, Identifier newTexture, boolean isFollowButton, PressAction pressAction) {
            super(xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn, yDiffTextIn, resourceLocationIn, pressAction);
            this.texture = resourceLocationIn;
            this.newTexture = newTexture;
            this.isFollowButton = isFollowButton;
        }

        public boolean requirementsForTexture() {
            boolean following = guardEntity.isFollowing();
            boolean patrol = guardEntity.isPatrolling();
            return this.isFollowButton ? following : patrol;
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            Identifier icon = this.requirementsForTexture() ? texture : newTexture;
            RenderSystem.setShaderTexture(0, icon);
            int i = this.v;
            if (this.isHovered()) {
                i += this.hoveredVOffset;
            }

            RenderSystem.enableDepthTest();
            drawTexture(matrixStack, this.x, this.y, (float) v, (float) i, this.width, this.height, textureWidth, textureHeight);
        }
    }

}
