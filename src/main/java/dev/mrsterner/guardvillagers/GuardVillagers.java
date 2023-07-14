package dev.mrsterner.guardvillagers;

import dev.mrsterner.guardvillagers.client.screen.GuardVillagerScreenHandler;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;

import java.util.function.Predicate;

public class GuardVillagers implements ModInitializer {
	public static final String MODID = "guardvillagers";
	public static GuardVillagersConfig config;

	public static final ScreenHandlerType<GuardVillagerScreenHandler> GUARD_SCREEN_HANDLER = ScreenHandlerRegistry.registerExtended(new Identifier(GuardVillagers.MODID, "guard_screen"), GuardVillagerScreenHandler::new);


	public static final EntityType<GuardEntity> GUARD_VILLAGER = Registry.register(Registries.ENTITY_TYPE, new Identifier(GuardVillagers.MODID, "guard"),
	FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GuardEntity::new).dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build());

	public static final Item GUARD_SPAWN_EGG = new SpawnEggItem(GUARD_VILLAGER ,5651507, 8412749, new Item.Settings());


	@Override
	public void onInitialize() {
		MidnightConfig.init(MODID, GuardVillagersConfig.class);
		FabricDefaultAttributeRegistry.register(GUARD_VILLAGER, GuardEntity.createAttributes());
		Registry.register(Registries.ITEM, new Identifier(MODID, "guard_spawn_egg"), GUARD_SPAWN_EGG);


		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(GUARD_SPAWN_EGG));

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			ItemStack itemStack = player.getStackInHand(hand);
			if ((itemStack.getItem() instanceof SwordItem || itemStack.getItem() instanceof CrossbowItem) && player.isSneaking()) {
				if (hitResult != null) {
					Entity target = hitResult.getEntity();
					if (target instanceof VillagerEntity villagerEntity) {
						if (!villagerEntity.isBaby()) {
							if (villagerEntity.getVillagerData().getProfession() == VillagerProfession.NONE || villagerEntity.getVillagerData().getProfession() == VillagerProfession.NITWIT) {
								if (!GuardVillagersConfig.ConvertVillagerIfHaveHOTV || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.ConvertVillagerIfHaveHOTV) {
									convertVillager(villagerEntity, player, world);
									if (!player.getAbilities().creativeMode)
										itemStack.decrement(1);
									return ActionResult.SUCCESS;
								}
							}
						}
					}
				}

			}

			return ActionResult.PASS;
		});
	}

	public static boolean hotvChecker(PlayerEntity player) {
		return player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) || !GuardVillagersConfig.giveGuardStuffHOTV;
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
		if (player.getWorld().isClient()) {
			ParticleEffect particleEffect = ParticleTypes.HAPPY_VILLAGER;
			for (int i = 0; i < 10; ++i) {
				double d0 = villagerEntity.getRandom().nextGaussian() * 0.02D;
				double d1 = villagerEntity.getRandom().nextGaussian() * 0.02D;
				double d2 = villagerEntity.getRandom().nextGaussian() * 0.02D;
				villagerEntity.getWorld().addParticle(particleEffect, villagerEntity.getX() + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getWidth() * 2.0F) - (double) villagerEntity.getWidth(), villagerEntity.getY() + 0.5D + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getWidth()),
				villagerEntity.getZ() + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getWidth() * 2.0F) - (double) villagerEntity.getWidth(), d0, d1, d2);
			}
		}
		guard.copyPositionAndRotation(villagerEntity);
		guard.headYaw = villagerEntity.headYaw;
		guard.refreshPositionAndAngles(villagerEntity.getX(), villagerEntity.getY(), villagerEntity.getZ(), villagerEntity.getYaw(), villagerEntity.getPitch());
		guard.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, 1.0F);
		guard.equipStack(EquipmentSlot.MAINHAND, itemstack.copy());
		guard.guardInventory.setStack(5, itemstack.copy());

		int i = GuardEntity.getRandomTypeForBiome(guard.getWorld(), guard.getBlockPos());
		guard.setGuardVariant(i);
		guard.setPersistent();
		guard.setCustomName(villagerEntity.getCustomName());
		guard.setCustomNameVisible(villagerEntity.isCustomNameVisible());
		guard.setEquipmentDropChance(EquipmentSlot.HEAD, 100.0F);
		guard.setEquipmentDropChance(EquipmentSlot.CHEST, 100.0F);
		guard.setEquipmentDropChance(EquipmentSlot.FEET, 100.0F);
		guard.setEquipmentDropChance(EquipmentSlot.LEGS, 100.0F);
		guard.setEquipmentDropChance(EquipmentSlot.MAINHAND, 100.0F);
		guard.setEquipmentDropChance(EquipmentSlot.OFFHAND, 100.0F);
		world.spawnEntity(guard);
		villagerEntity.releaseTicketFor(MemoryModuleType.HOME);
		villagerEntity.releaseTicketFor(MemoryModuleType.JOB_SITE);
		villagerEntity.releaseTicketFor(MemoryModuleType.MEETING_POINT);
		villagerEntity.discard();
	}
}
