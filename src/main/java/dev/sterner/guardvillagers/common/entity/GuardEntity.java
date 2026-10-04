package dev.sterner.guardvillagers.common.entity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.network.GuardData;
import dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler;
import dev.sterner.guardvillagers.common.entity.goal.*;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class GuardEntity extends PathfinderMob implements CrossbowAttackMob, RangedAttackMob, ReputationEventHandler {
    protected static final EntityDataAccessor<String> OWNER_UNIQUE_ID = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.STRING);
    private static final AttributeModifier USE_ITEM_SPEED_PENALTY = new AttributeModifier(GuardVillagers.id("speed_penalty"), -0.25D, AttributeModifier.Operation.ADD_VALUE);
    private static final EntityDataAccessor<Optional<BlockPos>> GUARD_POS = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> PATROLLING = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> GUARD_VARIANT = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RUNNING_TO_EAT = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING_STATE = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> KICKING = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FOLLOWING = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Map<Pose, EntityDimensions> SIZE_BY_POSE = ImmutableMap.<Pose, EntityDimensions>builder().put(Pose.STANDING, EntityDimensions.scalable(0.6F, 1.95F)).put(Pose.SLEEPING, SLEEPING_DIMENSIONS).put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.CROUCHING, EntityDimensions.scalable(0.6F, 1.75F)).put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F)).build();
    private static final UniformInt angerTime = TimeUtil.rangeOfSeconds(20, 39);
    public static final Map<EquipmentSlot, ResourceKey<LootTable>> EQUIPMENT_SLOT_ITEMS = Util.make(Maps.newHashMap(), (slotItems) -> {
        slotItems.put(EquipmentSlot.MAINHAND, GuardEntityLootTables.GUARD_MAIN_HAND);
        slotItems.put(EquipmentSlot.OFFHAND, GuardEntityLootTables.GUARD_OFF_HAND);
        slotItems.put(EquipmentSlot.HEAD, GuardEntityLootTables.GUARD_HELMET);
        slotItems.put(EquipmentSlot.CHEST, GuardEntityLootTables.GUARD_CHEST);
        slotItems.put(EquipmentSlot.LEGS, GuardEntityLootTables.GUARD_LEGGINGS);
        slotItems.put(EquipmentSlot.FEET, GuardEntityLootTables.GUARD_FEET);
    });
    private final GossipContainer gossips = new GossipContainer();
    public long lastGossipTime;
    public long lastGossipDecayTime;
    public SimpleContainer guardInventory = new SimpleContainer(6);
    public int kickTicks;
    public int shieldCoolDown;
    public int kickCoolDown;
    public boolean interacting;
    public boolean spawnWithArmor;
    private int remainingPersistentAngerTime;
    private UUID persistentAngerTarget;

    public GuardEntity(EntityType<? extends GuardEntity> type, Level world) {
        super(type, world);
        this.setPersistenceRequired();
        if (GuardVillagersConfig.guardEntitysOpenDoors)
            ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
    }

    public static int slotToInventoryIndex(EquipmentSlot slot) {
        return switch (slot) {
            case CHEST -> 1;
            case FEET -> 3;
            case LEGS -> 2;
            default -> 0;
        };
    }

    /**
     * Credit - SmellyModder for Biome Specific Textures
     */
    public static int getRandomTypeForBiome(LevelAccessor world, BlockPos pos) {
        ResourceKey<VillagerType> type = VillagerType.byBiome(world.getBiome(pos));
        if (type == VillagerType.SNOW) return 6;
        else if (type == VillagerType.TAIGA) return 5;
        else if (type == VillagerType.JUNGLE) return 4;
        else if (type == VillagerType.SWAMP) return 3;
        else if (type == VillagerType.SAVANNA) return 2;
        else if (type == VillagerType.DESERT) return 1;
        else return 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, GuardVillagersConfig.healthModifier)
                .add(Attributes.MOVEMENT_SPEED, GuardVillagersConfig.speedModifier)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, GuardVillagersConfig.followRangeModifier);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData entityData) {
        this.setPersistenceRequired();
        int type = GuardEntity.getRandomTypeForBiome(world, this.blockPosition());
        if (entityData instanceof GuardEntityData) {
            type = ((GuardEntityData) entityData).variantData;
            entityData = new GuardEntityData(type);
        }
        this.setGuardEntityVariant(type);
        RandomSource random = world.getRandom();
        this.populateDefaultEquipmentSlots(random, difficulty);
        return super.finalizeSpawn(world, difficulty, spawnReason, entityData);
    }

    @Override
    protected void doPush(Entity entity) {
        if (entity instanceof PathfinderMob living) {
            boolean attackTargets = living.getTarget() instanceof Villager || living.getTarget() instanceof IronGolem || living.getTarget() instanceof GuardEntity;
            if (attackTargets) this.setTarget(living);
        }
        super.doPush(entity);
    }

    @Nullable
    public BlockPos getPatrolPos() {
        return this.entityData.get(GUARD_POS).orElse(null);
    }

    @Nullable
    public void setPatrolPos(BlockPos position) {
        this.entityData.set(GUARD_POS, Optional.ofNullable(position));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuardVillagers.GUARD_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        if (this.isBlocking()) {
            return SoundEvents.SHIELD_BLOCK.value();
        } else {
            return GuardVillagers.GUARD_HURT;
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GuardVillagers.GUARD_DEATH;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel world, DamageSource source, boolean causedByPlayer) {
        for (int i = 0; i < this.guardInventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.guardInventory.getItem(i);
            RandomSource random = level().getRandom();
            if (!itemstack.isEmpty() && !EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP) && random.nextFloat() < GuardVillagersConfig.chanceToDropEquipment)
                this.spawnAtLocation(world, itemstack);
        }
    }

    @Override
    public void load(ValueInput view) {
        super.load(view);

    }

    @Override
    public void saveWithoutId(ValueOutput view) {
        super.saveWithoutId(view);
    }

    @Override
    public void readAdditionalSaveData(ValueInput nbt) {
        super.readAdditionalSaveData(nbt);
        nbt.read("Owner", UUIDUtil.LENIENT_CODEC).ifPresent(this::setOwnerId);
        this.setGuardEntityVariant(nbt.getIntOr("Type", 0));
        this.kickTicks = nbt.getIntOr("KickTicks", 0);
        this.setFollowing(nbt.getBooleanOr("Following", false));
        this.interacting = nbt.getBooleanOr("Interacting", false);
        this.setPatrolling(nbt.getBooleanOr("Patrolling", false));
        this.shieldCoolDown = nbt.getIntOr("KickCooldown", 0);
        this.kickCoolDown = nbt.getIntOr("ShieldCooldown", 0);
        this.lastGossipDecayTime = nbt.getLongOr("LastGossipDecay", 0);
        this.lastGossipTime = nbt.getLongOr("LastGossipTime", 0);
        this.spawnWithArmor = nbt.getBooleanOr("SpawnWithArmor", false);
        if (nbt.contains("PatrolPosX")) {
            int x = nbt.getIntOr("PatrolPosX", 0);
            int y = nbt.getIntOr("PatrolPosY", 0);
            int z = nbt.getIntOr("PatrolPosZ", 0);
            this.entityData.set(GUARD_POS, Optional.ofNullable(new BlockPos(x, y, z)));
        }
        nbt.getInt("PatrolPosX").ifPresent((x) -> {
            int y = nbt.getIntOr("PatrolPosY", 0);
            int z = nbt.getIntOr("PatrolPosZ", 0);
            this.entityData.set(GUARD_POS, Optional.of(new BlockPos(x, y, z)));
        });
        nbt.read("Gossips", GossipContainer.CODEC).ifPresent((decoded) -> {
            this.gossips.clear();
            this.gossips.putAll(decoded);
        });
        nbt.childrenList("Inventory").ifPresent((list) -> {
            for(ValueInput entry : list) {
                int slot = Byte.toUnsignedInt(entry.getByteOr("Slot", (byte)-1));
                if (slot < this.guardInventory.getContainerSize()) {
                    entry.read(ItemStack.MAP_CODEC).ifPresent((stack) -> {
                        if (!stack.isEmpty()) {
                            this.guardInventory.setItem(slot, stack);
                        }

                    });
                }
            }

        });
        nbt.list("ArmorItems", ItemStack.CODEC).ifPresent((armorList) -> {
            for(ItemStack stack : armorList) {
                if (stack != null && !stack.isEmpty()) {
                    Equippable eq = (Equippable)stack.get(DataComponents.EQUIPPABLE);
                    EquipmentSlot slot = eq != null ? eq.slot() : this.getEquipmentSlotForItem(stack);
                    int index = slotToInventoryIndex(slot);
                    this.guardInventory.setItem(index, stack);
                }
            }

        });
        nbt.list("HandItems", ItemStack.CODEC).ifPresent((handList) -> {
            int i = 0;

            for(ItemStack stack : handList) {
                if (i >= 2) {
                    break;
                }

                if (stack != null && !stack.isEmpty()) {
                    int handSlot = i == 0 ? 5 : 4;
                    this.guardInventory.setItem(handSlot, stack);
                    ++i;
                } else {
                    ++i;
                }
            }
        });
    }

    @Override
    protected void completeUsingItem() {
        if (this.isUsingItem()) {
            InteractionHand hand = this.getUsedItemHand();
            if (!this.useItem.equals(this.getItemInHand(hand))) {
                this.releaseUsingItem();
            } else {
                if (!this.useItem.isEmpty() && this.isUsingItem()) {
                    this.level().levelEvent(2003, this.blockPosition(), Item.getId(this.useItem.getItem()));
                    ItemStack itemStack = this.useItem.finishUsingItem(this.level(), this);
                    if (itemStack != this.useItem) {
                        this.setItemInHand(hand, itemStack);
                    }
                    if (!(this.useItem.getUseAnimation() == ItemUseAnimation.EAT)) this.useItem.shrink(1);
                    this.releaseUsingItem();
                }

            }
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Type", this.getGuardEntityVariant());
        nbt.putInt("KickTicks", this.kickTicks);
        nbt.putInt("ShieldCooldown", this.shieldCoolDown);
        nbt.putInt("KickCooldown", this.kickCoolDown);
        nbt.putBoolean("Following", this.isFollowing());
        nbt.putBoolean("Interacting", this.interacting);
        nbt.putBoolean("Patrolling", this.isPatrolling());
        nbt.putBoolean("SpawnWithArmor", this.spawnWithArmor);
        nbt.putLong("LastGossipTime", this.lastGossipTime);
        nbt.putLong("LastGossipDecay", this.lastGossipDecayTime);
        UUID ownerId = this.getOwnerId();
        if (ownerId != null) {
            nbt.store("Owner", UUIDUtil.STRING_CODEC, ownerId);
        }

        int invSize = this.guardInventory.getContainerSize();
        List<ItemStackWithSlot> invOut = new ArrayList();

        for(int slot = 0; slot < invSize; ++slot) {
            ItemStack stack = this.guardInventory.getItem(slot);
            if (!stack.isEmpty()) {
                invOut.add(new ItemStackWithSlot(slot, stack.copy()));
            }
        }

        if (!invOut.isEmpty()) {
            nbt.store("Inventory", ItemStackWithSlot.CODEC.listOf(), invOut);
        }

        if (this.getPatrolPos() != null) {
            BlockPos p = this.getPatrolPos();
            nbt.putInt("PatrolPosX", p.getX());
            nbt.putInt("PatrolPosY", p.getY());
            nbt.putInt("PatrolPosZ", p.getZ());
        }

        nbt.store("Gossips", GossipContainer.CODEC, this.gossips);
    }

    private void maybeDecayGossip() {
        long i = level().getGameTime();
        if (this.lastGossipDecayTime == 0L) {
            this.lastGossipDecayTime = i;
        } else if (i >= this.lastGossipDecayTime + 24000L) {
            this.gossips.decay();
            this.lastGossipDecayTime = i;
        }
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (this.guardInventory != null) {
            switch (slot) {
                case HEAD:
                    return this.guardInventory.getItem(0);
                case CHEST:
                    return this.guardInventory.getItem(1);
                case LEGS:
                    return this.guardInventory.getItem(2);
                case FEET:
                    return this.guardInventory.getItem(3);
                case OFFHAND:
                    return this.guardInventory.getItem(4);
                case MAINHAND:
                    return this.guardInventory.getItem(5);
            }
        }

        return ItemStack.EMPTY;
    }


    public GossipContainer getGossips() {
        return this.gossips;
    }

    public int getPlayerEntityReputation(Player player) {
        return this.gossips.getReputation(player.getUUID(), (gossipType) -> true);
    }

    @Nullable
    public LivingEntity getOwner() {
        try {
            UUID uuid = this.getOwnerId();
            boolean heroOfTheVillage = uuid != null && level().getPlayerByUUID(uuid) != null && level().getPlayerByUUID(uuid).hasEffect(MobEffects.HERO_OF_THE_VILLAGE);
            return uuid == null || (level().getPlayerByUUID(uuid) != null && (!heroOfTheVillage && GuardVillagersConfig.followHero) || !GuardVillagersConfig.followHero && level().getPlayerByUUID(uuid) == null) ? null : level().getPlayerByUUID(uuid);
        } catch (IllegalArgumentException illegalargumentexception) {
            return null;
        }
    }

    public boolean isOwner(LivingEntity entityIn) {
        return entityIn == this.getOwner();
    }

    @Nullable
    public UUID getOwnerId() {
        String s = this.entityData.get(OWNER_UNIQUE_ID);
        if (s != null && !s.isEmpty()) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException var3) {
                return null;
            }
        } else {
            return null;
        }
    }

    public void setOwnerId(@Nullable UUID p_184754_1_) {
        this.entityData.set(OWNER_UNIQUE_ID, p_184754_1_ != null ? p_184754_1_.toString() : "");
    }

    @Override
    public boolean doHurtTarget(ServerLevel world, Entity target) {
        if (this.isKicking()) {
            DamageSource source = world.damageSources().mobAttack(this);
            ((LivingEntity) target).knockback(1.0, Mth.sin(this.getYRot() * ((float) Math.PI / 180)), (-Mth.cos(this.getYRot() * ((float) Math.PI / 180))), source, 0f);
            this.kickTicks = 10;
            level().broadcastEntityEvent(this, (byte) 4);
            this.lookAt(target, 90.0F, 90.0F);
        }
        ItemStack hand = this.getMainHandItem();
        hand.hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
        return super.doHurtTarget(world, target);
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 4) {
            this.kickTicks = 10;
        } else {
            super.handleEntityEvent(status);
        }
    }

    @Override
    public boolean isImmobile() {
        return this.interacting || super.isImmobile();
    }

    @Override
    public void die(DamageSource damageSource) {
        if ((level().getDifficulty() == Difficulty.NORMAL || level().getDifficulty() == Difficulty.HARD) && damageSource.getEntity() instanceof Zombie) {
            ZombieVillager zombieguard = (ZombieVillager)this.convertTo(EntityTypes.ZOMBIE_VILLAGER, ConversionParams.single(this, true, false), (zv) -> {
            });
            if (this.level().getDifficulty() != Difficulty.HARD && this.random.nextBoolean() || zombieguard == null) {
                return;
            }
            if (level() instanceof ServerLevel serverWorld){
                zombieguard.finalizeSpawn((ServerLevelAccessor) level(), serverWorld.getCurrentDifficultyAt(zombieguard.blockPosition()), EntitySpawnReason.CONVERSION, new Zombie.ZombieGroupData(false, true));

            }
             if (!this.isSilent()) level().levelEvent(null, 1026, this.blockPosition(), 0);
            this.discard();
        }
        super.die(damageSource);
    }

    @Override
    public void aiStep() {
        if (this.kickTicks > 0)
            --this.kickTicks;
        if (this.kickCoolDown > 0)
            --this.kickCoolDown;
        if (this.shieldCoolDown > 0)
            --this.shieldCoolDown;
        if (this.getHealth() < this.getMaxHealth() && this.tickCount % 200 == 0) {
            this.heal(GuardVillagersConfig.amountOfHealthRegenerated);
        }
        if (spawnWithArmor && this.level() instanceof ServerLevel serverWorld) {
            for (EquipmentSlot equipmentslottype : EquipmentSlot.values()) {
                for (ItemStack stack : this.getStacksFromLootTable(equipmentslottype, serverWorld)) {
                    this.setItemSlot(equipmentslottype, stack);
                }
            }
            this.spawnWithArmor = false;
        }

        super.aiStep();
    }

    @Override
    public void tick() {
        this.maybeDecayGossip();
        super.tick();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return SIZE_BY_POSE.getOrDefault(pose, EntityDimensions.scalable(0.6F, 1.95F));
    }

    @Override
    protected void blockUsingItem(ServerLevel level, LivingEntity attacker, DamageSource source, float damage, boolean fullyBlocked) {
        super.blockUsingItem(level, attacker, source, damage, fullyBlocked);
        if (attacker.getMainHandItem().is(ItemTags.AXES)) this.disableShield(true, attacker.getMainHandItem().getItem());
    }

    @Override
    public void startUsingItem(InteractionHand hand) {
        super.startUsingItem(hand);
        ItemStack itemstack = this.getItemInHand(hand);
        if (itemstack.getItem() == Items.SHIELD) { // See above

            AttributeInstance modifiableattributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            modifiableattributeinstance.removeModifier(USE_ITEM_SPEED_PENALTY);
            modifiableattributeinstance.addTransientModifier(USE_ITEM_SPEED_PENALTY);
        }
    }

    @Override
    public void releaseUsingItem() {
        super.releaseUsingItem();
        if (this.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(USE_ITEM_SPEED_PENALTY.id()))
            this.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(USE_ITEM_SPEED_PENALTY);
    }

    public void disableShield(boolean increase, Item item) {
        float chance = 0.25F;
        if (increase) chance += 0.75;
        if (this.random.nextFloat() < chance) {
            this.shieldCoolDown = 100;
            this.releaseUsingItem();
            level().broadcastEntityEvent(this, (byte) 30);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(GUARD_VARIANT, 0);
        builder.define(DATA_CHARGING_STATE, false);
        builder.define(KICKING, false);
        builder.define(OWNER_UNIQUE_ID, "");
        builder.define(FOLLOWING, false);
        builder.define(GUARD_POS, Optional.empty());
        builder.define(PATROLLING, false);
        builder.define(RUNNING_TO_EAT, false);

        super.defineSynchedData(builder);
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_CHARGING_STATE);
    }

    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(DATA_CHARGING_STATE, charging);
    }

    public boolean isKicking() {
        return this.entityData.get(KICKING);
    }

    public void setKicking(boolean kicking) {
        this.entityData.set(KICKING, kicking);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance localDifficulty) {
        this.setDropChance(EquipmentSlot.MAINHAND, 100.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 100.0F);
        this.spawnWithArmor = true;
    }

    public List<ItemStack> getStacksFromLootTable(EquipmentSlot slot, ServerLevel serverWorld) {
        if (EQUIPMENT_SLOT_ITEMS.containsKey(slot)) {

            LootTable loot = serverWorld.getServer().reloadableRegistries().getLootTable(EQUIPMENT_SLOT_ITEMS.get(slot));

            LootParams.Builder worldCtx = (new LootParams.Builder((ServerLevel)this.level())).withParameter(LootContextParams.THIS_ENTITY, this);
            var origin = this.position();
            var ds = this.getLastDamageSource();
            if (ds == null) {
                ds = serverWorld.damageSources().generic();
            }

            worldCtx.withParameter(LootContextParams.ORIGIN, origin);
            worldCtx.withParameter(LootContextParams.DAMAGE_SOURCE, ds);
            LootParams ctx = worldCtx.create(LootContextParamSets.ENTITY);
            return loot.getRandomItems(ctx);
        } else {
            return List.of();
        }
    }

    public int getGuardEntityVariant() {
        return this.entityData.get(GUARD_VARIANT);
    }

    public void setGuardEntityVariant(int typeId) {
        this.entityData.set(GUARD_VARIANT, typeId);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new KickGoal(this));
        this.goalSelector.addGoal(0, new GuardEatFoodGoal(this));
        this.goalSelector.addGoal(0, new RaiseShieldGoal(this));
        this.goalSelector.addGoal(1, new GuardRunToEatGoal(this));
        this.goalSelector.addGoal(2, new RangedCrossbowAttackPassiveGoal<>(this, 1.0D, 8.0F));
        this.goalSelector.addGoal(3, new RangedBowAttackPassiveGoal<GuardEntity>(this, 0.5D, 20, 15.0F) {
            @Override
            public boolean canUse() {
                return GuardEntity.this.getTarget() != null && this.isBowInMainhand() && !GuardEntity.this.isEating() && !GuardEntity.this.isBlocking();
            }

            protected boolean isBowInMainhand() {
                return GuardEntity.this.getMainHandItem().getItem() instanceof BowItem;
            }

            @Override
            public void tick() {
                super.tick();
                if (GuardEntity.this.isPatrolling()) {
                    GuardEntity.this.getNavigation().stop();
                    GuardEntity.this.getMoveControl().strafe(0.0F, 0.0F);
                }
            }

            @Override
            public boolean canContinueToUse() {
                return (this.canUse() || !GuardEntity.this.getNavigation().isDone()) && this.isBowInMainhand();
            }
        });
        this.goalSelector.addGoal(2, new GuardEntityMeleeGoal(this, 0.8D, true));
        this.goalSelector.addGoal(3, new FollowHeroGoal(this));
        if (GuardVillagersConfig.guardEntitysRunFromPolarBears)
            this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, PolarBear.class, 12.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(3, new MoveBackToVillageGoal(this, 0.5D, false));
        this.goalSelector.addGoal(3, new GolemRandomStrollInVillageGoal(this, 0.5D));
        this.goalSelector.addGoal(3, new MoveThroughVillageGoal(this, 0.5D, false, 4, () -> false));
        if (GuardVillagersConfig.guardEntitysOpenDoors) this.goalSelector.addGoal(3, new GuardInteractDoorGoal(this, true));
        if (GuardVillagersConfig.guardEntityFormation) this.goalSelector.addGoal(5, new FollowShieldGuards(this));
        if (GuardVillagersConfig.clericHealing) this.goalSelector.addGoal(6, new RunToClericGoal(this));
        if (GuardVillagersConfig.armorerRepairGuardEntityArmor)
            this.goalSelector.addGoal(6, new ArmorerRepairGuardArmorGoal(this));
        this.goalSelector.addGoal(4, new WalkBackToCheckPointGoal(this, 0.5D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, AbstractVillager.class, 8.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new GuardLookAtAndStopMovingWhenBeingTheInteractionTarget(this));
        this.targetSelector.addGoal(5, new DefendVillageGuardEntityGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Ravager.class, true));
        this.targetSelector.addGoal(2, (new HurtByTargetGoal(this, GuardEntity.class, IronGolem.class)).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Witch.class, true));
        this.targetSelector.addGoal(3, new HeroHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new HeroHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Raider.class, true));
        if (GuardVillagersConfig.attackAllMobs)
            this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                    this,
                    Mob.class,
                    5,
                    true,
                    true, (mob, owner) -> {
                Identifier id = EntityType.getKey(mob.getType());
                return mob instanceof Enemy && !GuardVillagersConfig.mobBlackList.contains(id.toString());
            }));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }


    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        this.shieldCoolDown = 8;
        if (this.getMainHandItem().getItem() instanceof CrossbowItem)
            this.performCrossbowAttack(this, 6.0F);
        if (this.getMainHandItem().getItem() instanceof BowItem) {
            ItemStack itemStack = this.getProjectile(this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW)));
            ItemStack hand = this.getUseItem();
            ItemEnchantments itemEnchantmentsComponent = EnchantmentHelper.getEnchantmentsForCrafting(itemStack);
            AbstractArrow persistentProjectileEntity = ProjectileUtil.getMobArrow(this, itemStack, pullProgress, hand);
            HolderLookup.RegistryLookup<Enchantment> impl = this.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

            itemEnchantmentsComponent.getLevel(impl.getOrThrow(Enchantments.POWER));
            int powerLevel = itemEnchantmentsComponent.getLevel(impl.getOrThrow(Enchantments.POWER));

            if (powerLevel > 0) {
                persistentProjectileEntity.setBaseDamageFromMob((float) powerLevel * 0.5f + 0.5f);
            }
            int punchLevel = itemEnchantmentsComponent.getLevel(impl.getOrThrow(Enchantments.PUNCH));
            if (punchLevel > 0) {
                //TODO persistentProjectileEntity.getKnockback().setPunch(punchLevel);
            }
            if (itemEnchantmentsComponent.getLevel(impl.getOrThrow(Enchantments.FLAME)) > 0)
                persistentProjectileEntity.setRemainingFireTicks(100);
            double d = target.getX() - this.getX();
            double e = target.getY(0.3333333333333333D) - persistentProjectileEntity.getY();
            double f = target.getZ() - this.getZ();
            double g = Math.sqrt(d * d + f * f);
            persistentProjectileEntity.shoot(d, e + g * 0.20000000298023224D, f, 1.6F, (float) (14 - this.level().getDifficulty().getId() * 4));
            this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.level().addFreshEntity(persistentProjectileEntity);
            hand.hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slotIn, ItemStack stack) {
        super.setItemSlot(slotIn, stack);
        switch (slotIn) {
            case CHEST:
                if (this.guardInventory.getItem(1).isEmpty())
                    this.guardInventory.setItem(1, stack.copy());
                break;
            case FEET:
                if (this.guardInventory.getItem(3).isEmpty())
                    this.guardInventory.setItem(3, stack.copy());
                break;
            case HEAD:
                if (this.guardInventory.getItem(0).isEmpty())
                    this.guardInventory.setItem(0, stack.copy());
                break;
            case LEGS:
                if (this.guardInventory.getItem(2).isEmpty())
                    this.guardInventory.setItem(2, stack.copy());
                break;
            case MAINHAND:
                this.guardInventory.setItem(5, stack.copy());
                break;
            case OFFHAND:
                this.guardInventory.setItem(4, stack.copy());
                break;
        }
    }

    public int getGuardVariant() {
        return this.entityData.get(GUARD_VARIANT);
    }


    @Override
    public ItemStack getProjectile(ItemStack shootable) {
        if (shootable.getItem() instanceof ProjectileWeaponItem) {
            Predicate<ItemStack> predicate = ((ProjectileWeaponItem) shootable.getItem()).getSupportedHeldProjectiles();
            ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
            return itemstack.isEmpty() ? new ItemStack(Items.ARROW) : itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public int getKickTicks() {
        return this.kickTicks;
    }

    public boolean isFollowing() {
        return this.entityData.get(FOLLOWING);
    }

    public void setFollowing(boolean following) {
        this.entityData.set(FOLLOWING, following);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        Identifier id = EntityType.getKey(target.getType());
        return !GuardVillagersConfig.mobBlackList.contains(id.toString()) && !target.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && !this.isOwner(target) && !(target instanceof Villager) && !(target instanceof IronGolem) && !(target instanceof GuardEntity) && super.canAttack(target);
    }

    @Override
    public void rideTick() {
        super.rideTick();
        if (this.getVehicle() instanceof PathfinderMob creatureentity) {
            this.yBodyRot = creatureentity.yBodyRot;
        }
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public void setTarget(LivingEntity entity) {
        if (entity instanceof GuardEntity || entity instanceof Villager || entity instanceof IronGolem)
            return;
        super.setTarget(entity);
    }


    public void gossip(Villager villager, long gameTime) {
        if ((gameTime < this.lastGossipTime || gameTime >= this.lastGossipTime + 1200L) && (gameTime < villager.lastGossipTime || gameTime >= villager.lastGossipTime + 1200L)) {
            this.gossips.transferFrom(villager.getGossips(), this.random, 10);
            this.lastGossipTime = gameTime;
            villager.lastGossipTime = gameTime;
        }
    }

    @Override
    protected void blockedByItem(LivingEntity defender, DamageSource source, float damage, boolean fullyBlocked) {
        if (this.isKicking()) {
            this.setKicking(false);
        }
        super.blockedByItem(defender, source, damage, fullyBlocked);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean configValues = player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.giveGuardStuffHotv || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.setGuardPatrolHotv || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.giveGuardStuffHotv && GuardVillagersConfig.setGuardPatrolHotv || this.getPlayerEntityReputation(player) >= GuardVillagersConfig.reputationRequirement || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && !GuardVillagersConfig.giveGuardStuffHotv && !GuardVillagersConfig.setGuardPatrolHotv || this.getOwnerId() != null && this.getOwnerId().equals(player.getUUID());
        boolean inventoryRequirements = !player.isSecondaryUseActive();
        if (inventoryRequirements) {
            if (this.getTarget() != player && this.canSimulateMovement() && configValues) {
                if (player instanceof ServerPlayer) {
                    this.openGui((ServerPlayer) player);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void onReputationEventFrom(ReputationEventType interaction, Entity entity) {

    }

    @Override
    public void hurtArmor(DamageSource damageSource, float damage) {
        if (damage >= 0.0F) {
            damage = damage / 4.0F;
            if (damage < 1.0F) {
                damage = 1.0F;
            }
            for (int i = 0; i < this.guardInventory.getContainerSize(); ++i) {
                ItemStack itemstack = this.guardInventory.getItem(i);

                if (itemstack.has(DataComponents.EQUIPPABLE)) {
                    DamageResistant resist = (DamageResistant)itemstack.get(DataComponents.DAMAGE_RESISTANT);
                    if (resist != null && resist.isResistantTo(damageSource)) {
                        return;
                    }

                    int j = i;
                    var list = Arrays.stream(EquipmentSlot.values()).filter(EquipmentSlot::isArmor).toList();

                    itemstack.hurtAndBreak((int) damage, this, list.get(j));
                }
            }
        }
    }

    @Override
    public void thunderHit(ServerLevel world, LightningBolt lightning) {
        if (world.getDifficulty() != Difficulty.PEACEFUL) {
            Witch witchentity = EntityTypes.WITCH.create(world, EntitySpawnReason.CONVERSION);
            if (witchentity == null) return;
            witchentity.copyPosition(this);
            witchentity.finalizeSpawn(world, world.getCurrentDifficultyAt(witchentity.blockPosition()), EntitySpawnReason.CONVERSION, null);
            witchentity.setNoAi(this.isNoAi());
            witchentity.setCustomName(this.getCustomName());
            witchentity.setCustomNameVisible(this.isCustomNameVisible());
            witchentity.setPersistenceRequired();
            world.tryAddFreshEntityWithPassengers(witchentity);
            this.discard();
        } else {
            super.thunderHit(world, lightning);
        }
    }


    public @Nullable UUID getAngryAtTarget() {
        return this.persistentAngerTarget;
    }

    public void setAngryAtTarget(@Nullable UUID target) {
        this.persistentAngerTarget = target;
    }

    public int getAngerTime() {
        return this.remainingPersistentAngerTime;
    }

    public void setAngerTime(int ticks) {
        this.remainingPersistentAngerTime = ticks;
    }

    public void openGui(ServerPlayer player) {
        this.setOwnerId(player.getUUID());
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        this.interacting = true;
        if (!this.level().isClientSide()) {
            player.openMenu(new GuardScreenHandlerFactory());
        }
    }

    public void setGuardVariant(int i) {
        this.entityData.set(GUARD_VARIANT, i);
    }

    private class GuardScreenHandlerFactory implements ExtendedMenuProvider {
        private GuardEntity guard() {
            return GuardEntity.this;
        }

        @Override
        public Component getDisplayName() {
            return this.guard().getDisplayName();
        }

        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
            var guardInv = this.guard().guardInventory;
            return new GuardVillagerScreenHandler(syncId, inv, guardInv, this.guard());
        }

        @Override
        public Object getScreenOpeningData(ServerPlayer player) {
            return new GuardData(guard().getId());
        }
    }

    public boolean isEating() {
        return GuardEatFoodGoal.isConsumable(this.getUseItem()) && this.isUsingItem();
    }

    public boolean isPatrolling() {
        return this.entityData.get(PATROLLING);
    }

    public void setPatrolling(boolean patrolling) {
        this.entityData.set(PATROLLING, patrolling);
    }

    @Override
    public boolean canUseNonMeleeWeapon(ItemStack item) {
        return item.getItem() instanceof BowItem || item.getItem() instanceof CrossbowItem || super.canUseNonMeleeWeapon(item);
    }

    public static class GuardEntityData implements SpawnGroupData {
        public final int variantData;

        public GuardEntityData(int type) {
            this.variantData = type;
        }
    }

    public static class DefendVillageGuardEntityGoal extends TargetGoal {
        private final GuardEntity guard;
        private LivingEntity villageAggressorTarget;

        public DefendVillageGuardEntityGoal(GuardEntity guardIn) {
            super(guardIn, false, true);
            this.guard = guardIn;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            AABB box = this.guard.getBoundingBox().inflate(10.0D, 8.0D, 10.0D);
            List<Villager> list = guard.level().getEntitiesOfClass(Villager.class, box);
            List<Player> list1 = guard.level().getEntitiesOfClass(Player.class, box);
            for (Villager villager : list) {
                for (Player player : list1) {
                    int i = villager.getPlayerReputation(player);
                    if (i <= GuardVillagersConfig.reputationRequirementToBeAttacked) {
                        this.villageAggressorTarget = player;
                    }
                }
            }
            return villageAggressorTarget != null && !villageAggressorTarget.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && !this.villageAggressorTarget.isSpectator() && !((Player) this.villageAggressorTarget).isCreative();
        }

        @Override
        public void start() {
            this.guard.setTarget(this.villageAggressorTarget);
            super.start();
        }
    }

    public static class FollowHeroGoal extends Goal {
        public final GuardEntity guard;

        public FollowHeroGoal(GuardEntity mob) {
            this.guard = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public void tick() {
            if (guard.getOwner() != null && guard.getOwner().distanceTo(guard) > 3.0D) {
                guard.getNavigation().moveTo(guard.getOwner(), 0.7D);
                guard.getLookControl().setLookAt(guard.getOwner());
            } else {
                guard.getNavigation().stop();
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public boolean canUse() {
            return guard.isFollowing() && guard.getOwner() != null;
        }

        @Override
        public void stop() {
            this.guard.getNavigation().stop();
        }
    }

    public static class GuardEntityMeleeGoal extends MeleeAttackGoal {
        public final GuardEntity guard;

        public GuardEntityMeleeGoal(GuardEntity guard, double speedIn, boolean useLongMemory) {
            super(guard, speedIn, useLongMemory);
            this.guard = guard;
        }

        @Override
        public boolean canUse() {
            return !(this.guard.getMainHandItem().getItem() instanceof CrossbowItem) && this.guard.getTarget() != null && !this.guard.isEating() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.guard.getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = guard.getTarget();
            if (target != null) {
                if (target.distanceTo(guard) <= 3.0D && !guard.isBlocking()) {
                    guard.getMoveControl().strafe(-2.0F, 0.0F);
                    guard.lookAt(target, 30.0F, 30.0F);
                }
                if (this.path != null && target.distanceTo(guard) <= 2.0D) guard.getNavigation().stop();
                super.tick();
            }
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (guard.isWithinMeleeAttackRange(target) && this.getTicksUntilNextAttack() <= 0) {
                this.resetAttackCooldown();
                this.guard.releaseUsingItem();
                if (guard.shieldCoolDown == 0) this.guard.shieldCoolDown = 8;
                this.guard.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
                if (guard.level() instanceof  ServerLevel serverWorld) {
                    this.guard.doHurtTarget(serverWorld, target);
                }
            }
        }
    }
}
