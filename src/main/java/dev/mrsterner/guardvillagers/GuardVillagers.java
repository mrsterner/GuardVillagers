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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.function.Predicate;

public class GuardVillagers implements ModInitializer, ClientModInitializer {
	public static final String MODID = "guardvillagers";
	public static GuardVillagersConfig config;

	public static final ScreenHandlerType<GuardVillagerScreenHandler> GUARD_SCREEN_HANDLER = ScreenHandlerRegistry.registerExtended(new Identifier(MODID, "guard_screen"), GuardVillagerScreenHandler::new);

	public static EntityModelLayer GUARD = new EntityModelLayer(new Identifier(GuardVillagers.MODID + "guard"), "guard");
	public static EntityModelLayer GUARD_STEVE = new EntityModelLayer(new Identifier(GuardVillagers.MODID + "guard_steve"), "guard_steve");
	public static EntityModelLayer GUARD_ARMOR_OUTER = new EntityModelLayer(new Identifier(GuardVillagers.MODID + "guard_armor_outer"), "guard_armor_outer");
	public static EntityModelLayer GUARD_ARMOR_INNER = new EntityModelLayer(new Identifier(GuardVillagers.MODID + "guard_armor_inner"), "guard_armor_inner");

	public static final EntityType<GuardEntity> GUARD_VILLAGER = Registry.register(Registry.ENTITY_TYPE, new Identifier(GuardVillagers.MODID, "guard"),
	FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GuardEntity::new).dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build());

	public static final Item GUARD_SPAWN_EGG = new SpawnEggItem(GUARD_VILLAGER ,5651507, 8412749, new FabricItemSettings().group(ItemGroup.MISC));

	@Override
	public void onInitialize() {
		AutoConfig.register(GuardVillagersConfig.class, GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(GuardVillagersConfig.class).getConfig();
		FabricDefaultAttributeRegistry.register(GUARD_VILLAGER, GuardEntity.createAttributes());
		Registry.register(Registry.ITEM, new Identifier(MODID, "guard_spawn_egg"), GUARD_SPAWN_EGG);
	}

	@Override
	public void onInitializeClient() {
		ScreenRegistry.register(GUARD_SCREEN_HANDLER, GuardVillagerScreen::new);
		EntityModelLayerRegistry.registerModelLayer(GUARD, GuardVillagerModel::createBodyLayer);
		EntityModelLayerRegistry.registerModelLayer(GUARD_STEVE, GuardSteveModel::createMesh);
		EntityModelLayerRegistry.registerModelLayer(GUARD_ARMOR_OUTER, GuardArmorModel::createOuterArmorLayer);
		EntityModelLayerRegistry.registerModelLayer(GUARD_ARMOR_INNER, GuardArmorModel::createInnerArmorLayer);
		EntityRendererRegistry.register(GUARD_VILLAGER, GuardRenderer::new);



	}

	public static boolean hotvChecker(PlayerEntity player) {
		return player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.get().giveGuardStuffHOTV || !GuardVillagersConfig.get().giveGuardStuffHOTV;
	}

	public static Hand getHandWith(LivingEntity livingEntity, Predicate<Item> itemPredicate) {
		return itemPredicate.test(livingEntity.getMainHandStack().getItem()) ? Hand.MAIN_HAND : Hand.OFF_HAND;
	}
}
