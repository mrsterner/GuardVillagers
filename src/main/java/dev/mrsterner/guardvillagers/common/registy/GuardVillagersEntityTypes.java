package dev.mrsterner.guardvillagers.common.registy;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.LinkedHashMap;
import java.util.Map;

public class GuardVillagersEntityTypes {
    private static final Map<EntityType<?>, Identifier> ENTITY_TYPES = new LinkedHashMap<>();

    public static final EntityType<GuardEntity> GUARD_VILLAGER = create("guard", GuardEntity.createAttributes(), FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GuardEntity::new).dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build());


    private static <T extends Entity> EntityType<T> register(String id, EntityType.EntityFactory<T> factory) {
        EntityType<T> type = FabricEntityTypeBuilder.create(SpawnGroup.MISC, factory).dimensions(new EntityDimensions(0.8F, 0.8F, true)).disableSummon().spawnableFarFromPlayer().trackRangeBlocks(90).trackedUpdateRate(50).build();
        ENTITY_TYPES.put(type, new Identifier(GuardVillagers.MODID, id));
        return type;
    }

    private static <T extends LivingEntity> EntityType<T> create(String name, DefaultAttributeContainer.Builder attributes, EntityType<T> type) {
        FabricDefaultAttributeRegistry.register(type, attributes);
        ENTITY_TYPES.put(type, new Identifier(GuardVillagers.MODID, name));
        return type;
    }

    public static void init(){
        ENTITY_TYPES.keySet().forEach(entityType -> Registry.register(Registry.ENTITY_TYPE, ENTITY_TYPES.get(entityType), entityType));
    }
}
