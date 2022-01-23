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
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
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
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;

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

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			ItemStack itemStack = player.getStackInHand(hand);
			if ((itemStack.getItem() instanceof SwordItem || itemStack.getItem() instanceof CrossbowItem) && player.isSneaking()) {
				Entity target = hitResult.getEntity();
				if (target instanceof VillagerEntity villagerEntity) {
					if (!villagerEntity.isBaby()) {
						if (villagerEntity.getVillagerData().getProfession() == VillagerProfession.NONE || villagerEntity.getVillagerData().getProfession() == VillagerProfession.NITWIT) {
							if (!GuardVillagersConfig.get().ConvertVillagerIfHaveHOTV || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.get().ConvertVillagerIfHaveHOTV) {
								convertVillager(villagerEntity, player, world);
								if (!player.getAbilities().creativeMode)
									itemStack.decrement(1);
								return ActionResult.SUCCESS;
							}
						}
					}
				}
			}

			return ActionResult.PASS;
		});
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

	private static void convertVillager(VillagerEntity villagerEntity, PlayerEntity player, World world) {
		player.swingHand(Hand.MAIN_HAND);
		ItemStack itemstack = player.getEquippedStack(EquipmentSlot.MAINHAND);
		GuardEntity guard = GUARD_VILLAGER.create(world);
		if (guard == null)
			return;
		if (player.world.isClient()) {
			ParticleEffect iparticledata = ParticleTypes.HAPPY_VILLAGER;
			for (int i = 0; i < 10; ++i) {
				double d0 = villagerEntity.getRandom().nextGaussian() * 0.02D;
				double d1 = villagerEntity.getRandom().nextGaussian() * 0.02D;
				double d2 = villagerEntity.getRandom().nextGaussian() * 0.02D;
				villagerEntity.world.addParticle(iparticledata, villagerEntity.getX() + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getWidth() * 2.0F) - (double) villagerEntity.getWidth(), villagerEntity.getY() + 0.5D + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getWidth()),
				villagerEntity.getZ() + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getWidth() * 2.0F) - (double) villagerEntity.getWidth(), d0, d1, d2);
			}
		}
		guard.copyPositionAndRotation(villagerEntity);
		guard.limbDistance = villagerEntity.limbDistance;
		guard.lastLimbDistance = villagerEntity.lastLimbDistance;
		guard.headYaw = villagerEntity.headYaw;
		guard.refreshPositionAndAngles(villagerEntity.getX(), villagerEntity.getY(), villagerEntity.getZ(), villagerEntity.getYaw(), villagerEntity.getPitch());
		guard.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, 1.0F);
		guard.equipStack(EquipmentSlot.MAINHAND, itemstack.copy());
		int i = GuardEntity.getRandomTypeForBiome(guard.world, guard.getBlockPos());
		guard.setGuardVariant(i);
		guard.setPersistent();
		world.spawnEntity(guard);
		villagerEntity.releaseTicketFor(MemoryModuleType.HOME);
		villagerEntity.releaseTicketFor(MemoryModuleType.JOB_SITE);
		villagerEntity.releaseTicketFor(MemoryModuleType.MEETING_POINT);
		villagerEntity.remove(Entity.RemovalReason.DISCARDED);
		villagerEntity.discard();

	}
}
