package dev.sterner.guardvillagers;

import dev.sterner.guardvillagers.client.screen.GuardVillagerScreen;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import dev.sterner.guardvillagers.common.entity.goal.AttackEntityDaytimeGoal;
import dev.sterner.guardvillagers.common.entity.goal.HealGolemGoal;
import dev.sterner.guardvillagers.common.entity.goal.HealGuardAndPlayerGoal;
import dev.sterner.guardvillagers.common.event.GuardVillagersEvents;
import dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler;
import eu.midnightdust.lib.config.MidnightConfig;
import io.github.fabricators_of_create.porting_lib.event.common.LivingEntityEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class GuardVillagers implements ModInitializer {
    public static final String MODID = "guardvillagers";
    public static final Identifier ID = new Identifier(GuardVillagers.MODID, "guard_follow");
    public static final Identifier ID_2 = new Identifier(GuardVillagers.MODID, "guard_patroll");

    public static final ScreenHandlerType<GuardVillagerScreenHandler> GUARD_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(GuardVillagerScreenHandler::new);

    public static final EntityType<GuardEntity> GUARD_VILLAGER = Registry.register(Registry.ENTITY_TYPE, new Identifier(GuardVillagers.MODID, "guard"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GuardEntity::new).dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build());

    public static final Item GUARD_SPAWN_EGG = new SpawnEggItem(GUARD_VILLAGER, 5651507, 8412749, new Item.Settings().group(ItemGroup.MISC));

    public static Hand getHandWith(LivingEntity livingEntity, Predicate<Item> itemPredicate) {
        return itemPredicate.test(livingEntity.getMainHandStack().getItem()) ? Hand.MAIN_HAND : Hand.OFF_HAND;
    }

    public static SoundEvent GUARD_AMBIENT = new SoundEvent(new Identifier(MODID, "entity.guard.ambient"));
    public static SoundEvent GUARD_HURT = new SoundEvent(new Identifier(MODID, "entity.guard.hurt"));
    public static SoundEvent GUARD_DEATH = new SoundEvent(new Identifier(MODID, "entity.guard.death"));

    @Override
    public void onInitialize() {
        MidnightConfig.init(MODID, GuardVillagersConfig.class);
        FabricDefaultAttributeRegistry.register(GUARD_VILLAGER, GuardEntity.createAttributes());

        Registry.register(Registry.ITEM, new Identifier(MODID, "guard_spawn_egg"), GUARD_SPAWN_EGG);
        Registry.register(Registry.SCREEN_HANDLER, new Identifier("guard_screen"), GUARD_SCREEN_HANDLER);
        Registry.register(Registry.SOUND_EVENT, new Identifier(MODID, "entity.guard.ambient"), GUARD_AMBIENT);
        Registry.register(Registry.SOUND_EVENT, new Identifier(MODID, "entity.guard.hurt"), GUARD_HURT);
        Registry.register(Registry.SOUND_EVENT, new Identifier(MODID, "entity.guard.death"), GUARD_DEATH);

        ServerPlayNetworking.registerGlobalReceiver(ID, ((server, player, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            boolean pressed = buf.readBoolean();
            server.execute(() -> {
                Entity entity = player.world.getEntityById(entityId);
                if(entity instanceof GuardEntity guardEntity){
                    guardEntity.setFollowing(!guardEntity.isFollowing());
                    guardEntity.setOwnerId(player.getUuid());
                    guardEntity.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1,1);
                }

            });
        }));
        ServerPlayNetworking.registerGlobalReceiver(ID_2, ((server, player, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            boolean pressed = buf.readBoolean();
            server.execute(() -> {
                Entity entity = player.world.getEntityById(entityId);
                if(entity instanceof GuardEntity guardEntity){
                    BlockPos pos = guardEntity.getBlockPos();
                    if (guardEntity.getBlockPos() != null){
                        guardEntity.setPatrolPos(pos);
                    }
                    guardEntity.setPatrolling(pressed);
                }

            });
        }));

        //LivingEntityEvents.NATURAL_SPAWN.register(this::addGoals);
        //LivingEntityEvents.SET_TARGET.register(this::target);
        GuardVillagersEvents.ON_TARGET_EVENT.register(this::target);
        GuardVillagersEvents.ON_SPAWNED_ENTITY_EVENT.register(this::addGoals);
        LivingEntityEvents.ACTUALLY_HURT.register(this::onDamage);
        UseEntityCallback.EVENT.register(this::villagerConvert);
    }

    private void target(LivingEntity mob, LivingEntity target) {
        if (target == null || mob instanceof GuardEntity) {
            return;
        }
        boolean isVillager = target.getType() == EntityType.VILLAGER || target instanceof GuardEntity;
        if (isVillager) {
            List<MobEntity> list = mob.getWorld().getNonSpectatingEntities(MobEntity.class, mob.getBoundingBox().expand(GuardVillagersConfig.guardVillagerHelpRange, 5.0D, GuardVillagersConfig.guardVillagerHelpRange));
            for (MobEntity mobEntity : list) {
                if ((mobEntity instanceof GuardEntity || mob.getType() == EntityType.IRON_GOLEM) && mobEntity.getTarget() == null) {
                    mobEntity.setTarget(mob);
                }
            }
        }

        if (mob instanceof IronGolemEntity golem && target instanceof GuardEntity) {
            golem.setTarget(null);
        }
    }

    private void addGoals(ServerWorld serverWorld, Entity entity) {
        if (GuardVillagersConfig.raidAnimals) {
            if (entity instanceof RaiderEntity raiderEntity)
                if (raiderEntity.hasActiveRaid()) {
                    raiderEntity.targetSelector.add(5, new ActiveTargetGoal<>(raiderEntity, AnimalEntity.class, false));
                }
        }

        if (GuardVillagersConfig.attackAllMobs) {
            if (entity instanceof HostileEntity && !(entity instanceof SpiderEntity)) {
                MobEntity mob = (MobEntity) entity;
                mob.targetSelector.add(2, new ActiveTargetGoal<>(mob, GuardEntity.class, false));
            }
            if (entity instanceof SpiderEntity spider) {
                spider.targetSelector.add(3, new AttackEntityDaytimeGoal<>(spider, GuardEntity.class));
            }
        }


        if (entity instanceof IllagerEntity illager) {
            if (GuardVillagersConfig.illagersRunFromPolarBears) {
                illager.goalSelector.add(2, new FleeEntityGoal<>(illager, PolarBearEntity.class, 6.0F, 1.0D, 1.2D));
            }

            illager.targetSelector.add(2, new ActiveTargetGoal<>(illager, GuardEntity.class, false));
        }

        if (entity instanceof VillagerEntity villagerEntity) {
            if (GuardVillagersConfig.villagersRunFromPolarBears)
                villagerEntity.goalSelector.add(2, new FleeEntityGoal<>(villagerEntity, PolarBearEntity.class, 6.0F, 1.0D, 1.2D));
            if (GuardVillagersConfig.witchesVillager)
                villagerEntity.goalSelector.add(2, new FleeEntityGoal<>(villagerEntity, WitchEntity.class, 6.0F, 1.0D, 1.2D));
        }

        if (entity instanceof VillagerEntity villagerEntity) {
            if (GuardVillagersConfig.blackSmithHealing)
                villagerEntity.goalSelector.add(1, new HealGolemGoal(villagerEntity));
            if (GuardVillagersConfig.clericHealing)
                villagerEntity.goalSelector.add(1, new HealGuardAndPlayerGoal(villagerEntity, 1.0D, 100, 0, 10.0F));
        }

        if (entity instanceof IronGolemEntity golem) {

            RevengeGoal tolerateFriendlyFire = new RevengeGoal(golem, GuardEntity.class).setGroupRevenge();
            golem.targetSelector.getGoals().stream().map(PrioritizedGoal::getGoal).filter(it -> it instanceof RevengeGoal).findFirst().ifPresent(angerGoal -> {
                golem.targetSelector.remove(angerGoal);
                golem.targetSelector.add(2, tolerateFriendlyFire);
            });
        }

        if (entity instanceof ZombieEntity zombie) {
            zombie.targetSelector.add(3, new ActiveTargetGoal<>(zombie, GuardEntity.class, false));
        }

        if (entity instanceof RavagerEntity ravager) {
            ravager.targetSelector.add(2, new ActiveTargetGoal<>(ravager, GuardEntity.class, false));
        }

        if (entity instanceof WitchEntity witch) {
            if (GuardVillagersConfig.witchesVillager) {
                witch.targetSelector.add(3, new ActiveTargetGoal<>(witch, VillagerEntity.class, true));
                witch.targetSelector.add(3, new ActiveTargetGoal<>(witch, IronGolemEntity.class, true));
                witch.targetSelector.add(3, new ActiveTargetGoal<>(witch, GuardEntity.class, true));
            }
        }

        if (entity instanceof CatEntity cat) {
            cat.goalSelector.add(1, new FleeEntityGoal<>(cat, IllagerEntity.class, 12.0F, 1.0D, 1.2D));
        }
    }

    private float onDamage(DamageSource damageSource, LivingEntity entity, float damageAmount) {
        Entity trueSource = damageSource.getAttacker();
        if (entity == null || trueSource == null)
            return damageAmount;

        boolean isVillager = entity.getType() == EntityType.VILLAGER || entity.getType() == GuardVillagers.GUARD_VILLAGER;
        boolean isGolem = isVillager || entity.getType() == EntityType.IRON_GOLEM;
        if (isGolem && trueSource.getType() == GuardVillagers.GUARD_VILLAGER && !GuardVillagersConfig.guardArrowsHurtVillagers) {
            damageAmount = 0;
            return damageAmount;
        }
        if (isVillager && damageSource.getAttacker() instanceof MobEntity) {
            List<MobEntity> list = trueSource.getWorld().getNonSpectatingEntities(MobEntity.class, trueSource.getBoundingBox().expand(GuardVillagersConfig.guardVillagerHelpRange, 5.0D, GuardVillagersConfig.guardVillagerHelpRange));
            for (MobEntity mob : list) {
                boolean type = mob.getType() == GUARD_VILLAGER || mob.getType() == EntityType.IRON_GOLEM;
                boolean trueSourceGolem = trueSource.getType() == GUARD_VILLAGER || trueSource.getType() == EntityType.IRON_GOLEM;
                if (!trueSourceGolem && type && mob.getTarget() == null)
                    mob.setTarget((MobEntity) damageSource.getAttacker());
            }
        }
        return damageAmount;
    }

    private ActionResult villagerConvert(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult entityHitResult) {
        ItemStack itemStack = player.getStackInHand(hand);
        if ((itemStack.getItem() instanceof SwordItem || itemStack.getItem() instanceof CrossbowItem) && player.isSneaking()) {
            if (entityHitResult != null) {
                Entity target = entityHitResult.getEntity();
                if (target instanceof VillagerEntity villagerEntity) {
                    if (!villagerEntity.isBaby()) {
                        if (villagerEntity.getVillagerData().getProfession() == VillagerProfession.NONE || villagerEntity.getVillagerData().getProfession() == VillagerProfession.NITWIT) {
                            if (!GuardVillagersConfig.convertVillagerIfHaveHotv || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.convertVillagerIfHaveHotv) {
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
    }

    private void convertVillager(VillagerEntity villagerEntity, PlayerEntity player, World world) {
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

    public static boolean hotvChecker(PlayerEntity player, GuardEntity guard) {
        return player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.giveGuardStuffHotv
                || !GuardVillagersConfig.giveGuardStuffHotv || guard.getPlayerEntityReputation(player) > GuardVillagersConfig.reputationRequirement && !player.getWorld().isClient();
    }
}