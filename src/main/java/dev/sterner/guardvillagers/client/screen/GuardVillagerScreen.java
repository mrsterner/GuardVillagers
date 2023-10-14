package dev.sterner.guardvillagers.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import dev.sterner.guardvillagers.common.network.GuardFollowPacket;
import dev.sterner.guardvillagers.common.network.GuardPatrolPacket;
import dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class GuardVillagerScreen extends HandledScreen<GuardVillagerScreenHandler> {

    private static final Identifier GUARD_GUI_TEXTURES = new Identifier(GuardVillagers.MODID, "textures/gui/inventory.png");
    private static final ButtonTextures GUARD_FOLLOWING_ICONS = new ButtonTextures(new Identifier(GuardVillagers.MODID, "following/following"), new Identifier(GuardVillagers.MODID, "following/following_highlighted"));
    private static final ButtonTextures GUARD_NOT_FOLLOWING_ICONS = new ButtonTextures(new Identifier(GuardVillagers.MODID, "following/not_following"), new Identifier(GuardVillagers.MODID, "following/not_following_highlighted"));
    private static final ButtonTextures GUARD_PATROLLING_ICONS = new ButtonTextures(new Identifier(GuardVillagers.MODID, "patrolling/patrolling1"), new Identifier(GuardVillagers.MODID,"patrolling/patrolling2"));
    private static final ButtonTextures GUARD_NOT_PATROLLING_ICONS = new ButtonTextures(new Identifier(GuardVillagers.MODID, "patrolling/notpatrolling1"), new Identifier(GuardVillagers.MODID,"patrolling/notpatrolling2"));

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
            this.addDrawableChild(new GuardGuiButton(this.x + 100, this.height / 2 - 40, 20, 18, GUARD_FOLLOWING_ICONS, GUARD_NOT_FOLLOWING_ICONS, true,
                    (button) -> {
                        ClientPlayNetworking.send(new GuardFollowPacket(guardEntity.getId()));
                    })
            );
        }
        if (!GuardVillagersConfig.setGuardPatrolHotv || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
            this.addDrawableChild(new GuardGuiButton(this.x + 120, this.height / 2 - 40, 20, 18, GUARD_PATROLLING_ICONS, GUARD_NOT_PATROLLING_ICONS, false,
                    (button) -> {
                        buttonPressed = !buttonPressed;
                        ClientPlayNetworking.send(new GuardPatrolPacket(guardEntity.getId(), buttonPressed));
                    })
            );
        }
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        ctx.drawTexture(GUARD_GUI_TEXTURES, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
        //InventoryScreen.drawEntity(ctx, i + 51, j + 75, 30, (float) (i + 51) - this.mousePosX, (float) (j + 75 - 50) - this.mousePosY, this.guardEntity);
        InventoryScreen.drawEntity(ctx, i + 26, j + 8, i + 75, j + 78, 30, 0.0625F, this.mousePosX, this.mousePosY, this.guardEntity);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int x, int y) {
        super.drawForeground(ctx, x, y);
        int health = MathHelper.ceil(guardEntity.getHealth());
        int armor = guardEntity.getArmor();
        int statusU = guardEntity.hasStatusEffect(StatusEffects.POISON) ? 4 : 0;
        //Health
        for (int i = 0; i < 10; i++) {
            ctx.drawTexture(ICONS, (i * 8) + 80, 20, 16, 0, 9, 9);
        }
        for (int i = 0; i < health / 2; i++) {
            if (health % 2 != 0 && health / 2 == i + 1) {
                ctx.drawTexture(ICONS, (i * 8) + 80, 20, 16 + 9 * (4 + statusU), 0, 9, 9);
                ctx.drawTexture(ICONS, ((i + 1) * 8) + 80, 20, 16 + 9 * (5 + statusU), 0, 9, 9);
            } else {
                ctx.drawTexture(ICONS, (i * 8) + 80, 20, 16 + 9 * (4 + statusU), 0, 9, 9);
            }
        }
        //Armor
        for (int i = 0; i < 10; i++) {
            ctx.drawTexture(ICONS, (i * 8) + 80, 30, 16, 9, 9, 9);
        }
        for (int i = 0; i < armor / 2; i++) {
            if (armor % 2 != 0 && armor / 2 == i + 1) {
                ctx.drawTexture(ICONS, (i * 8) + 80, 30, 16 + 9 * 2, 9, 9, 9);
                ctx.drawTexture(ICONS, ((i + 1) * 8) + 80, 30, 16 + 9, 9, 9, 9);
            } else {
                ctx.drawTexture(ICONS, (i * 8) + 80, 30, 16 + 9 * 2, 9, 9, 9);
            }
        }

    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ctx, mouseX, mouseY, partialTicks);
        this.mousePosX = (float) mouseX;
        this.mousePosY = (float) mouseY;
        super.render(ctx, mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }


    class GuardGuiButton extends TexturedButtonWidget {
        private final ButtonTextures texture;
        private final ButtonTextures newTexture;
        private final boolean isFollowButton;

        public GuardGuiButton(int xIn, int yIn, int widthIn, int heightIn, ButtonTextures resourceLocationIn, ButtonTextures newTexture, boolean isFollowButton, PressAction pressAction) {
            super(xIn, yIn, widthIn, heightIn, resourceLocationIn, pressAction);
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
        public void renderButton(DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
            ButtonTextures icon = this.requirementsForTexture() ? this.texture : this.newTexture;
            Identifier resourcelocation = icon.get(this.active, this.isSelected());
            ctx.drawGuiTexture(resourcelocation, this.getX(), this.getY(), this.width, this.height);
        }
    }

}
