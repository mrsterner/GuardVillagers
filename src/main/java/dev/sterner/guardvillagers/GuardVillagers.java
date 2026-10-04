package dev.sterner.guardvillagers;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import dev.sterner.guardvillagers.common.network.GuardData;
import dev.sterner.guardvillagers.common.network.GuardFollowPacket;
import dev.sterner.guardvillagers.common.network.GuardPatrolPacket;
import dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class GuardVillagers implements ModInitializer {
    public static final String MODID = "guardvillagers";

    public static final MenuType<GuardVillagerScreenHandler> GUARD_SCREEN_HANDLER =
            new ExtendedMenuType<>(GuardVillagerScreenHandler::new, GuardData.PACKET_CODEC);

    public static final ResourceKey<EntityType<?>> GUARD_VILLAGER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("guard"));
    public static final EntityType<GuardEntity> GUARD_VILLAGER =  EntityType.Builder.of(GuardEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build(GUARD_VILLAGER_KEY);

    public static final ResourceKey<Item> GUARD_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, id("guard_spawn_egg"));
    public static final Item GUARD_SPAWN_EGG = registerItem(GUARD_SPAWN_EGG_KEY, SpawnEggItem::new, new Item.Properties().spawnEgg(GUARD_VILLAGER));

    public static InteractionHand getHandWith(LivingEntity livingEntity, Predicate<Item> itemPredicate) {
        return itemPredicate.test(livingEntity.getMainHandItem().getItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static SoundEvent GUARD_AMBIENT = SoundEvent.createVariableRangeEvent(id( "entity.guard.ambient"));
    public static SoundEvent GUARD_HURT = SoundEvent.createVariableRangeEvent(id( "entity.guard.hurt"));
    public static SoundEvent GUARD_DEATH = SoundEvent.createVariableRangeEvent(id("entity.guard.death"));

    public static Identifier id(String name){
        return Identifier.fromNamespaceAndPath(MODID, name);
    }

    private static Item registerItem(final ResourceKey<Item> key, final Function<Item.Properties, Item> itemFactory, final Item.Properties properties) {
        Item item = itemFactory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    @Override
    public void onInitialize() {
        MidnightConfig.init(MODID, GuardVillagersConfig.class);
        FabricDefaultAttributeRegistry.register(GUARD_VILLAGER, GuardEntity.createAttributes());

        Registry.register(BuiltInRegistries.ENTITY_TYPE, GUARD_VILLAGER_KEY, GUARD_VILLAGER);

        Registry.register(BuiltInRegistries.MENU, id("guard_screen"), GUARD_SCREEN_HANDLER);
        Registry.register(BuiltInRegistries.SOUND_EVENT, id("entity.guard.ambient"), GUARD_AMBIENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, id( "entity.guard.hurt"), GUARD_HURT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, id( "entity.guard.death"), GUARD_DEATH);

        PayloadTypeRegistry.serverboundPlay().register(GuardFollowPacket.ID, GuardFollowPacket.PACKET_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GuardPatrolPacket.ID, GuardPatrolPacket.PACKET_CODEC);

        PayloadTypeRegistry.clientboundPlay().register(GuardFollowPacket.ID, GuardFollowPacket.PACKET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GuardPatrolPacket.ID, GuardPatrolPacket.PACKET_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(GuardFollowPacket.ID, GuardFollowPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(GuardPatrolPacket.ID, GuardPatrolPacket::handle);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(GUARD_SPAWN_EGG));

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::onDamage);
        UseEntityCallback.EVENT.register(this::villagerConvert);

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Villager villagerEntity) {
                var spawnChance = Mth.clamp(GuardVillagersConfig.spawnChancePerVillager, 0f, 1f);
                if (world.getRandom().nextFloat() < spawnChance) {
                    GuardEntity guardEntity = GUARD_VILLAGER.create(world, EntitySpawnReason.MOB_SUMMONED);
                    guardEntity.spawnWithArmor= true;
                    guardEntity.finalizeSpawn(world, world.getCurrentDifficultyAt(villagerEntity.blockPosition()), EntitySpawnReason.NATURAL, null);
                    guardEntity.snapTo(villagerEntity.blockPosition(), 0.0f, 0.0f);

                    int i = GuardEntity.getRandomTypeForBiome(guardEntity.level(), guardEntity.blockPosition());
                    guardEntity.setGuardVariant(i);
                    guardEntity.setPersistenceRequired();
                    guardEntity.setCustomName(villagerEntity.getCustomName());
                    guardEntity.setCustomNameVisible(villagerEntity.isCustomNameVisible());
                    guardEntity.setDropChance(EquipmentSlot.HEAD, 100.0F);
                    guardEntity.setDropChance(EquipmentSlot.CHEST, 100.0F);
                    guardEntity.setDropChance(EquipmentSlot.FEET, 100.0F);
                    guardEntity.setDropChance(EquipmentSlot.LEGS, 100.0F);
                    guardEntity.setDropChance(EquipmentSlot.MAINHAND, 100.0F);
                    guardEntity.setDropChance(EquipmentSlot.OFFHAND, 100.0F);

                    world.addFreshEntityWithPassengers(guardEntity);
                }
            }
        });
    }


    private boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (entity == null || attacker == null)
            return true;
        boolean shouldDamage = true;
        boolean isVillager = entity.getType() == EntityTypes.VILLAGER || entity.getType() == GuardVillagers.GUARD_VILLAGER;
        boolean isGolem = isVillager || entity.getType() == EntityTypes.IRON_GOLEM;
        if (isGolem && attacker.getType() == GuardVillagers.GUARD_VILLAGER && !GuardVillagersConfig.guardArrowsHurtVillagers) {
            shouldDamage = false;
        }
        if (isVillager && attacker instanceof Mob) {
            List<Mob> list = attacker.level().getEntitiesOfClass(Mob.class, attacker.getBoundingBox().inflate(GuardVillagersConfig.guardVillagerHelpRange, 5.0D, GuardVillagersConfig.guardVillagerHelpRange));
            for (Mob mob : list) {
                boolean type = mob.getType() == GUARD_VILLAGER || mob.getType() == EntityTypes.IRON_GOLEM;
                boolean trueSourceGolem = attacker.getType() == GUARD_VILLAGER || attacker.getType() == EntityTypes.IRON_GOLEM;
                if (!trueSourceGolem && type && mob.getTarget() == null)
                    mob.setTarget((Mob) attacker);
            }
        }
        return shouldDamage;
    }

    private InteractionResult villagerConvert(Player player, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult entityHitResult) {
        ItemStack itemStack = player.getItemInHand(hand);
        if ((itemStack.is(ItemTags.SWORDS) || itemStack.getItem() instanceof CrossbowItem) && player.isShiftKeyDown()) {
            if (entityHitResult != null) {
                Entity target = entityHitResult.getEntity();
                if (target instanceof Villager villagerEntity) {
                    if (!villagerEntity.isBaby()) {
                        if (villagerEntity.getVillagerData().profession().is(VillagerProfession.NONE) || villagerEntity.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
                            if (!GuardVillagersConfig.convertVillagerIfHaveHotv || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.convertVillagerIfHaveHotv) {
                                convertVillager(villagerEntity, player, world);
                                if (!player.getAbilities().instabuild)
                                    itemStack.shrink(1);
                                return InteractionResult.SUCCESS;
                            }
                        }
                    }
                }
            }

        }

        return InteractionResult.PASS;
    }

    private void convertVillager(Villager villagerEntity, Player player, Level world) {
        player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        ItemStack itemstack = player.getItemBySlot(EquipmentSlot.MAINHAND);
        GuardEntity guard = GUARD_VILLAGER.create(world, EntitySpawnReason.MOB_SUMMONED);
        if (guard == null)
            return;
        if (player.level().isClientSide()) {
            ParticleOptions particleEffect = ParticleTypes.HAPPY_VILLAGER;
            for (int i = 0; i < 10; ++i) {
                double d0 = villagerEntity.getRandom().nextGaussian() * 0.02D;
                double d1 = villagerEntity.getRandom().nextGaussian() * 0.02D;
                double d2 = villagerEntity.getRandom().nextGaussian() * 0.02D;
                villagerEntity.level().addParticle(particleEffect, villagerEntity.getX() + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getBbWidth() * 2.0F) - (double) villagerEntity.getBbWidth(), villagerEntity.getY() + 0.5D + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getBbWidth()),
                        villagerEntity.getZ() + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getBbWidth() * 2.0F) - (double) villagerEntity.getBbWidth(), d0, d1, d2);
            }
        }
        guard.copyPosition(villagerEntity);
        guard.yHeadRot = villagerEntity.yHeadRot;
        guard.snapTo(villagerEntity.getX(), villagerEntity.getY(), villagerEntity.getZ(), villagerEntity.getYRot(), villagerEntity.getXRot());
        guard.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
        guard.setItemSlot(EquipmentSlot.MAINHAND, itemstack.copy());
        guard.guardInventory.setItem(5, itemstack.copy());

        int i = GuardEntity.getRandomTypeForBiome(guard.level(), guard.blockPosition());
        guard.setGuardVariant(i);
        guard.setPersistenceRequired();
        guard.setCustomName(villagerEntity.getCustomName());
        guard.setCustomNameVisible(villagerEntity.isCustomNameVisible());
        guard.setDropChance(EquipmentSlot.HEAD, 100.0F);
        guard.setDropChance(EquipmentSlot.CHEST, 100.0F);
        guard.setDropChance(EquipmentSlot.FEET, 100.0F);
        guard.setDropChance(EquipmentSlot.LEGS, 100.0F);
        guard.setDropChance(EquipmentSlot.MAINHAND, 100.0F);
        guard.setDropChance(EquipmentSlot.OFFHAND, 100.0F);
        world.addFreshEntity(guard);
        villagerEntity.releasePoi(MemoryModuleType.HOME);
        villagerEntity.releasePoi(MemoryModuleType.JOB_SITE);
        villagerEntity.releasePoi(MemoryModuleType.MEETING_POINT);
        villagerEntity.discard();
    }

    public static boolean hotvChecker(Player player, GuardEntity guard) {
        return player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.giveGuardStuffHotv
                || !GuardVillagersConfig.giveGuardStuffHotv || guard.getPlayerEntityReputation(player) > GuardVillagersConfig.reputationRequirement && !player.level().isClientSide();
    }
}