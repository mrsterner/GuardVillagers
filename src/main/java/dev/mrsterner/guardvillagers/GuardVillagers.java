package dev.mrsterner.guardvillagers;

import dev.mrsterner.guardvillagers.client.screen.GuardVillagerScreenHandler;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.fabricmc.loader.api.FabricLoader;
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

public class GuardVillagers implements ModInitializer {
	public static final String MODID = "guardvillagers";
	public static GuardVillagersConfig config;

	public static final ScreenHandlerType<GuardVillagerScreenHandler> GUARD_SCREEN_HANDLER = ScreenHandlerRegistry.registerExtended(new Identifier(GuardVillagers.MODID, "guard_screen"), GuardVillagerScreenHandler::new);


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
							if (!GuardVillagers.config.generail.ConvertVillagerIfHaveHOTV || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagers.config.generail.ConvertVillagerIfHaveHOTV) {
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


	public static boolean isFabricPermissionsAPILoaded(){
		return FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
	}

	public static boolean hotvChecker(PlayerEntity player) {
		return player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) || !config.generail.giveGuardStuffHOTV;
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
			ParticleEffect particleEffect = ParticleTypes.HAPPY_VILLAGER;
			for (int i = 0; i < 10; ++i) {
				double d0 = villagerEntity.getRandom().nextGaussian() * 0.02D;
				double d1 = villagerEntity.getRandom().nextGaussian() * 0.02D;
				double d2 = villagerEntity.getRandom().nextGaussian() * 0.02D;
				villagerEntity.world.addParticle(particleEffect, villagerEntity.getX() + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getWidth() * 2.0F) - (double) villagerEntity.getWidth(), villagerEntity.getY() + 0.5D + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getWidth()),
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
		villagerEntity.discard();
	}
}
