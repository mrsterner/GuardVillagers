package dev.mrsterner.guardvillagers;

import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.stat.Stat;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class GuardVillagers implements ModInitializer {
	public static final String MODID = "guardvillagers";
	public static GuardVillagersConfig config;

	public static boolean hotvChecker(PlayerEntity player) {
		return player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.get().giveGuardStuffHOTV || !GuardVillagersConfig.get().giveGuardStuffHOTV;
	}

	public static Hand getHandWith(LivingEntity livingEntity, Predicate<Item> itemPredicate) {
		return itemPredicate.test(livingEntity.getMainHandStack().getItem()) ? Hand.MAIN_HAND : Hand.OFF_HAND;
	}
	public static final EntityType<GuardEntity> GUARD = create("guard", GuardEntity.createAttributes(), FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GuardEntity::new).dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build());
	private static final Map<EntityType<?>, Identifier> ENTITY_TYPES = new LinkedHashMap<>();

	private static <T extends LivingEntity> EntityType<T> create(String name, DefaultAttributeContainer.Builder attributes, EntityType<T> type) {
		FabricDefaultAttributeRegistry.register(type, attributes);
		ENTITY_TYPES.put(type, new Identifier(MODID, name));
		return type;
	}



	@Override
	public void onInitialize() {
		AutoConfig.register(GuardVillagersConfig.class, GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(GuardVillagersConfig.class).getConfig();

		ENTITY_TYPES.keySet().forEach(entityType -> Registry.register(Registry.ENTITY_TYPE, ENTITY_TYPES.get(entityType), entityType));
	}
}
