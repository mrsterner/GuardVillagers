package dev.mrsterner.guardvillagers;

import dev.mrsterner.guardvillagers.client.model.GuardArmorModel;
import dev.mrsterner.guardvillagers.client.model.GuardSteveModel;
import dev.mrsterner.guardvillagers.client.model.GuardVillagerModel;
import dev.mrsterner.guardvillagers.client.renderer.GuardRenderer;
import dev.mrsterner.guardvillagers.client.screen.GuardVillagerScreen;
import dev.mrsterner.guardvillagers.client.screen.GuardVillagerScreenHandler;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;

import java.util.function.Predicate;

import static dev.mrsterner.guardvillagers.GuardVillagers.GUARD_SCREEN_HANDLER;
import static dev.mrsterner.guardvillagers.GuardVillagers.GUARD_VILLAGER;

@Environment(EnvType.CLIENT)
public class GuardVillagersClient implements ClientModInitializer {

	public static EntityModelLayer GUARD = new EntityModelLayer(new Identifier(GuardVillagers.MODID + "guard"), "guard");
	public static EntityModelLayer GUARD_STEVE = new EntityModelLayer(new Identifier(GuardVillagers.MODID + "guard_steve"), "guard_steve");
	public static EntityModelLayer GUARD_ARMOR_OUTER = new EntityModelLayer(new Identifier(GuardVillagers.MODID + "guard_armor_outer"), "guard_armor_outer");
	public static EntityModelLayer GUARD_ARMOR_INNER = new EntityModelLayer(new Identifier(GuardVillagers.MODID + "guard_armor_inner"), "guard_armor_inner");





	@Override
	public void onInitializeClient() {
		ScreenRegistry.register(GUARD_SCREEN_HANDLER, GuardVillagerScreen::new);
		EntityModelLayerRegistry.registerModelLayer(GUARD, GuardVillagerModel::createBodyLayer);
		EntityModelLayerRegistry.registerModelLayer(GUARD_STEVE, GuardSteveModel::createMesh);
		EntityModelLayerRegistry.registerModelLayer(GUARD_ARMOR_OUTER, GuardArmorModel::createOuterArmorLayer);
		EntityModelLayerRegistry.registerModelLayer(GUARD_ARMOR_INNER, GuardArmorModel::createInnerArmorLayer);
		EntityRendererRegistry.register(GUARD_VILLAGER, GuardRenderer::new);

		ServerPlayNetworking.registerGlobalReceiver(GuardVillagerScreen.ID, ((server, player, handler, buf, responseSender) -> {
			int entityId = buf.readInt();
			server.execute(() -> {
				Entity entity = player.world.getEntityById(entityId);
				if(entity instanceof GuardEntity guardEntity){
					guardEntity.setFollowing(!guardEntity.isFollowing());
					guardEntity.setOwnerId(player.getUuid());
					guardEntity.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1,1);
				}

			});
		}));
		ServerPlayNetworking.registerGlobalReceiver(GuardVillagerScreen.ID_2, ((server, player, handler, buf, responseSender) -> {
			int entityId = buf.readInt();
			boolean pressed = buf.readBoolean();
			server.execute(() -> {
				Entity entity = player.world.getEntityById(entityId);
				if(entity instanceof GuardEntity guardEntity){
					BlockPos pos = pressed ? null : guardEntity.getBlockPos();
					if (guardEntity.getBlockPos() != null)
						guardEntity.setPatrolPos(pos);
					guardEntity.setPatrolling(pressed);
				}

			});
		}));
	}
}
