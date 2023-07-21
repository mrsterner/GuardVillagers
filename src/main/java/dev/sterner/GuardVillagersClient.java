package dev.sterner;

import dev.sterner.client.model.GuardArmorModel;
import dev.sterner.client.model.GuardSteveModel;
import dev.sterner.client.model.GuardVillagerModel;
import dev.sterner.client.renderer.GuardRenderer;
import dev.sterner.client.screen.GuardVillagerScreen;
import dev.sterner.common.network.GuardFollowPacket;
import dev.sterner.common.network.GuardPatrolPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import static dev.sterner.GuardVillagers.*;

public class GuardVillagersClient implements ClientModInitializer {

    public static EntityModelLayer GUARD = new EntityModelLayer(new Identifier(GuardVillagers.MODID + "guard"), "guard");
    public static EntityModelLayer GUARD_STEVE = new EntityModelLayer(new Identifier(GuardVillagers.MODID + "guard_steve"), "guard_steve");
    public static EntityModelLayer GUARD_ARMOR_OUTER = new EntityModelLayer(new Identifier(GuardVillagers.MODID + "guard_armor_outer"), "guard_armor_outer");
    public static EntityModelLayer GUARD_ARMOR_INNER = new EntityModelLayer(new Identifier(GuardVillagers.MODID + "guard_armor_inner"), "guard_armor_inner");

    public static SoundEvent GUARD_AMBIENT = SoundEvent.of(new Identifier(MODID, "entity.guard.ambient"));
    public static SoundEvent GUARD_HURT = SoundEvent.of(new Identifier(MODID, "entity.guard.hurt"));
    public static SoundEvent GUARD_DEATH = SoundEvent.of(new Identifier(MODID, "entity.guard.death"));


    @Override
    public void onInitializeClient() {
        Registry.register(Registries.SOUND_EVENT, new Identifier(MODID, "entity.guard.ambient"), GUARD_AMBIENT);
        Registry.register(Registries.SOUND_EVENT, new Identifier(MODID, "entity.guard.hurt"), GUARD_HURT);
        Registry.register(Registries.SOUND_EVENT, new Identifier(MODID, "entity.guard.death"), GUARD_DEATH);

        HandledScreens.register(GUARD_SCREEN_HANDLER, GuardVillagerScreen::new);
        EntityModelLayerRegistry.registerModelLayer(GUARD, GuardVillagerModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(GUARD_STEVE, GuardSteveModel::createMesh);
        EntityModelLayerRegistry.registerModelLayer(GUARD_ARMOR_OUTER, GuardArmorModel::createOuterArmorLayer);
        EntityModelLayerRegistry.registerModelLayer(GUARD_ARMOR_INNER, GuardArmorModel::createInnerArmorLayer);
        EntityRendererRegistry.register(GUARD_VILLAGER, GuardRenderer::new);

        ServerPlayNetworking.registerGlobalReceiver(GuardFollowPacket.PACKET_TYPE, GuardFollowPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(GuardPatrolPacket.PACKET_TYPE, GuardPatrolPacket::handle);
    }
}
