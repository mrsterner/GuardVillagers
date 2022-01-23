package dev.mrsterner.guardvillagers.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.GuardVillagersConfig;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.stat.Stat;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.Stack;

public class GuardInventoryScreen extends HandledScreen<GuardVillagerScreenHandler> {
    private static final Identifier GUARD_GUI_TEXTURES = new Identifier(GuardVillagers.MODID, "textures/container/inventory.png");
    private static final Identifier GUARD_FOLLOWING_ICON = new Identifier(GuardVillagers.MODID, "textures/container/following_icons.png");
    private static final Identifier GUARD_NOT_FOLLOWING_ICON = new Identifier(GuardVillagers.MODID, "textures/container/not_following_icons.png");
    private static final Identifier PATROL_ICON = new Identifier(GuardVillagers.MODID, "textures/container/patrollingui.png");
    private static final Identifier NOT_PATROLLING_ICON = new Identifier(GuardVillagers.MODID, "textures/container/notpatrollingui.png");
    private final GuardEntity guard;
    private PlayerEntity player;
    private float mousePosX;
    private float mousePosY;
    private boolean buttonPressed;

    public GuardInventoryScreen(GuardVillagerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.titleX = 80;
        this.playerInventoryTitleX = 100;
        this.passEvents = false;
        this.player = inventory.player;
        this.guard = handler.guard;
    }

    @Override
    protected void init() {
        super.init();
        if (player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
            this.addDrawableChild(new GuardGuiButton(this.x + 100, this.height / 2 - 40, 20, 18, 0, 0, 19, GUARD_FOLLOWING_ICON, GUARD_NOT_FOLLOWING_ICON, true, (p_214086_1_) -> {
                //TODO GuardPacketHandler.INSTANCE.sendToServer(new GuardFollowPacket(guard.getId()));
            }));
        }
        if (GuardVillagersConfig.get().setGuardPatrolHotv && player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) || !GuardVillagersConfig.get().setGuardPatrolHotv) {
            this.addDrawableChild(new GuardGuiButton(this.x + 120, this.height / 2 - 40, 20, 18, 0, 0, 19, PATROL_ICON, NOT_PATROLLING_ICON, false, (p_214086_1_) -> {
                buttonPressed = !buttonPressed;
               //TODO GuardPacketHandler.INSTANCE.sendToServer(new GuardSetPatrolPosPacket(guard.getId(), buttonPressed));
            }));
        }
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUARD_GUI_TEXTURES);
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
        InventoryScreen.drawEntity(i + 51, j + 75, 30, (float) (i + 51) - this.mousePosX, (float) (j + 75 - 50) - this.mousePosY, this.handler.guard);
    }

    @Override
    protected void drawForeground(MatrixStack matrixStack, int x, int y) {
        super.drawForeground(matrixStack, x, y);
        int health = MathHelper.ceil(handler.guard.getHealth());
        int armor = handler.guard.getArmor();
        Text guardHealthText = new TranslatableText("guardinventory.health", health);
        Text guardArmorText = new TranslatableText("guardinventory.armor", armor);
        this.textRenderer.draw(matrixStack, guardHealthText, 80.0F, 20.0F, 4210752);
        this.textRenderer.draw(matrixStack, guardArmorText, 80.0F, 30.0F, 4210752);
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
        private Identifier texture;
        private Identifier newTexture;
        private boolean isFollowButton;

        public GuardGuiButton(int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn, int yDiffTextIn, Identifier resourceLocationIn, Identifier newTexture, boolean isFollowButton, PressAction pressAction ) {
            super(xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn, yDiffTextIn, resourceLocationIn, pressAction);
            this.texture = resourceLocationIn;
            this.newTexture = newTexture;
            this.isFollowButton = isFollowButton;
        }

        // This is stupid.
        public boolean requirementsForTexture() {

            boolean following = GuardInventoryScreen.this.handler.guard.isFollowing();
            boolean patrol = GuardInventoryScreen.this.handler.guard.isPatrolling();
            return this.isFollowButton ? following : patrol;
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            Identifier icon = this.requirementsForTexture() ? texture : newTexture;
            RenderSystem.setShaderTexture(0, icon);
            int i = this.y;
            if (this.isHovered()) {
                i += this.y;
            }

            RenderSystem.enableDepthTest();
            drawTexture(matrixStack, this.x, this.y, (float) this.x, (float) i, this.width, this.height, this.width, this.height);
            if (this.isHovered()) {
                this.renderTooltip(matrixStack, mouseX, mouseY);
            }
        }
    }
}
