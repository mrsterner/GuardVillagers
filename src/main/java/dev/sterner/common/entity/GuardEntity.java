package dev.sterner.common.entity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Dynamic;
import dev.sterner.GuardVillagers;
import dev.sterner.GuardVillagersConfig;
import dev.sterner.common.entity.goal.*;
import dev.sterner.common.screenhandler.GuardVillagerScreenHandler;
import io.github.fabricators_of_create.porting_lib.item.ShieldBlockItem;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import io.github.fabricators_of_create.porting_lib.util.ToolActions;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.VillagerGossips;
import net.minecraft.village.VillagerType;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class GuardEntity extends PathAwareEntity implements CrossbowUser, RangedAttackMob, Angerable, InventoryChangedListener, InteractionObserver {
    protected static final TrackedData<Optional<UUID>> OWNER_UNIQUE_ID = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final UUID MODIFIER_UUID = UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E");
    private static final EntityAttributeModifier USE_ITEM_SPEED_PENALTY = new EntityAttributeModifier(MODIFIER_UUID, "Use item speed penalty", -0.25D, EntityAttributeModifier.Operation.ADDITION);
    private static final TrackedData<Optional<BlockPos>> GUARD_POS = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    private static final TrackedData<Boolean> PATROLLING = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> GUARD_VARIANT = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> RUNNING_TO_EAT = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DATA_CHARGING_STATE = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> KICKING = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FOLLOWING = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Map<EntityPose, EntityDimensions> SIZE_BY_POSE = ImmutableMap.<EntityPose, EntityDimensions>builder().put(EntityPose.STANDING, EntityDimensions.changing(0.6F, 1.95F)).put(EntityPose.SLEEPING, SLEEPING_DIMENSIONS).put(EntityPose.FALL_FLYING, EntityDimensions.changing(0.6F, 0.6F)).put(EntityPose.SWIMMING, EntityDimensions.changing(0.6F, 0.6F)).put(EntityPose.SPIN_ATTACK, EntityDimensions.changing(0.6F, 0.6F)).put(EntityPose.CROUCHING, EntityDimensions.changing(0.6F, 1.75F)).put(EntityPose.DYING, EntityDimensions.fixed(0.2F, 0.2F)).build();
    private static final UniformIntProvider angerTime = TimeHelper.betweenSeconds(20, 39);
    private static final Map<EquipmentSlot, Identifier> EQUIPMENT_SLOT_ITEMS = Util.make(Maps.newHashMap(), (slotItems) -> {
        slotItems.put(EquipmentSlot.MAINHAND, GuardEntityLootTables.GUARD_MAIN_HAND);
        slotItems.put(EquipmentSlot.OFFHAND, GuardEntityLootTables.GUARD_OFF_HAND);
        slotItems.put(EquipmentSlot.HEAD, GuardEntityLootTables.GUARD_HELMET);
        slotItems.put(EquipmentSlot.CHEST, GuardEntityLootTables.GUARD_CHEST);
        slotItems.put(EquipmentSlot.LEGS, GuardEntityLootTables.GUARD_LEGGINGS);
        slotItems.put(EquipmentSlot.FEET, GuardEntityLootTables.GUARD_FEET);
    });
    private final VillagerGossips gossips = new VillagerGossips();
    public long lastGossipTime;
    public long lastGossipDecayTime;
    public SimpleInventory guardInventory = new SimpleInventory(6);
    public int kickTicks;
    public int shieldCoolDown;
    public int kickCoolDown;
    public boolean interacting;
    private int remainingPersistentAngerTime;
    private UUID persistentAngerTarget;
    private LazyOptional<?> itemHandler;

    public GuardEntity(EntityType<? extends GuardEntity> type, World world) {
        super(type, world);
        this.guardInventory.addListener(this);
        this.setPersistent();
        if (GuardVillagersConfig.guardEntitysOpenDoors)
            ((MobNavigation) this.getNavigation()).setCanPathThroughDoors(true);
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
    public static int getRandomTypeForBiome(WorldAccess world, BlockPos pos) {
        VillagerType type = VillagerType.forBiome(world.getBiome(pos));
        if (type == VillagerType.SNOW) return 6;
        else if (type == VillagerType.TAIGA) return 5;
        else if (type == VillagerType.JUNGLE) return 4;
        else if (type == VillagerType.SWAMP) return 3;
        else if (type == VillagerType.SAVANNA) return 2;
        else if (type == VillagerType.DESERT) return 1;
        else return 0;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, GuardVillagersConfig.healthModifier)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, GuardVillagersConfig.speedModifier)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, GuardVillagersConfig.followRangeModifier);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData dataTracker, @Nullable NbtCompound entityNbt) {
        this.setPersistent();
        int type = GuardEntity.getRandomTypeForBiome(world, this.getBlockPos());
        if (dataTracker instanceof GuardEntity.GuardEntityData) {
            type = ((GuardEntity.GuardEntityData) dataTracker).variantData;
            dataTracker = new GuardEntity.GuardEntityData(type);
        }
        this.setGuardEntityVariant(type);
        Random random = world.getRandom();
        this.initEquipment(random, difficulty);
        return super.initialize(world, difficulty, spawnReason, dataTracker, entityNbt);
    }

    @Override
    protected void pushAway(Entity entity) {
        if (entity instanceof PathAwareEntity living) {
            boolean attackTargets = living.getTarget() instanceof VillagerEntity || living.getTarget() instanceof IronGolemEntity || living.getTarget() instanceof GuardEntity;
            if (attackTargets) this.setTarget(living);
        }
        super.pushAway(entity);
    }

    @Nullable
    public BlockPos getPatrolPos() {
        return this.dataTracker.get(GUARD_POS).orElse(null);
    }

    @Nullable
    public void setPatrolPos(BlockPos position) {
        this.dataTracker.set(GUARD_POS, Optional.ofNullable(position));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuardVillagers.GUARD_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        if (this.isBlocking()) {
            return SoundEvents.ITEM_SHIELD_BLOCK;
        } else {
            return GuardVillagers.GUARD_HURT;
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GuardVillagers.GUARD_DEATH;
    }

    @Override
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        for (int i = 0; i < this.guardInventory.size(); ++i) {
            ItemStack itemstack = this.guardInventory.getStack(i);
            Random random = getWorld().getRandom();
            if (!itemstack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemstack) && random.nextFloat() < GuardVillagersConfig.chanceToDropEquipment)
                this.dropStack(itemstack);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        UUID uuid = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        if (uuid != null) {
            try {
                this.setOwnerId(uuid);
            } catch (Throwable throwable) {
                this.setOwnerId(null);
            }
        }
        this.setGuardEntityVariant(nbt.getInt("Type"));
        this.kickTicks = nbt.getInt("KickTicks");
        this.setFollowing(nbt.getBoolean("Following"));
        this.interacting = nbt.getBoolean("Interacting");
        this.setPatrolling(nbt.getBoolean("Patrolling"));
        this.shieldCoolDown = nbt.getInt("KickCooldown");
        this.kickCoolDown = nbt.getInt("ShieldCooldown");
        this.lastGossipDecayTime = nbt.getLong("LastGossipDecay");
        this.lastGossipTime = nbt.getLong("LastGossipTime");
        if (nbt.contains("PatrolPosX")) {
            int x = nbt.getInt("PatrolPosX");
            int y = nbt.getInt("PatrolPosY");
            int z = nbt.getInt("PatrolPosZ");
            this.dataTracker.set(GUARD_POS, Optional.ofNullable(new BlockPos(x, y, z)));
        }
        NbtList listtag = nbt.getList("Gossips", 10);
        this.gossips.deserialize(new Dynamic<>(NbtOps.INSTANCE, listtag));
        NbtList listnbt = nbt.getList("Inventory", 9);
        for (int i = 0; i < listnbt.size(); ++i) {
            NbtCompound nbtnbt = listnbt.getCompound(i);
            int j = nbtnbt.getByte("Slot") & 255;
            this.guardInventory.setStack(j, ItemStack.fromNbt(nbtnbt));
        }
        if (nbt.contains("ArmorItems", 9)) {
            NbtList armorItems = nbt.getList("ArmorItems", 10);
            for (int i = 0; i < this.armorItems.size(); ++i) {
                int index = GuardEntity.slotToInventoryIndex(MobEntity.getPreferredEquipmentSlot(ItemStack.fromNbt(armorItems.getCompound(i))));
                this.guardInventory.setStack(index, ItemStack.fromNbt(armorItems.getCompound(i)));
            }
        }
        if (nbt.contains("HandItems", 9)) {
            NbtList handItems = nbt.getList("HandItems", 10);
            for (int i = 0; i < this.handItems.size(); ++i) {
                int handSlot = i == 0 ? 5 : 4;
                this.guardInventory.setStack(handSlot, ItemStack.fromNbt(handItems.getCompound(i)));
            }
        }
        if (!getWorld().isClient) this.readAngerFromNbt(getWorld(), nbt);
    }

    @Override
    protected void consumeItem() {
        if (this.isUsingItem()) {
            Hand hand = this.getActiveHand();
            if (!this.activeItemStack.equals(this.getStackInHand(hand))) {
                this.stopUsingItem();
            } else {
                if (!this.activeItemStack.isEmpty() && this.isUsingItem()) {
                    this.spawnConsumptionEffects(this.activeItemStack, 16);
                    ItemStack itemStack = this.activeItemStack.finishUsing(this.getWorld(), this);
                    if (itemStack != this.activeItemStack) {
                        this.setStackInHand(hand, itemStack);
                    }
                    if (!this.activeItemStack.isFood()) this.activeItemStack.decrement(1);
                    this.stopUsingItem();
                }

            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Type", this.getGuardEntityVariant());
        nbt.putInt("KickTicks", this.kickTicks);
        nbt.putInt("ShieldCooldown", this.shieldCoolDown);
        nbt.putInt("KickCooldown", this.kickCoolDown);
        nbt.putBoolean("Following", this.isFollowing());
        nbt.putBoolean("Interacting", this.interacting);
        nbt.putBoolean("Patrolling", this.isPatrolling());
        nbt.putLong("LastGossipTime", this.lastGossipTime);
        nbt.putLong("LastGossipDecay", this.lastGossipDecayTime);
        if (this.getOwnerId() != null) {
            nbt.putUuid("Owner", this.getOwnerId());
        }
        NbtList listnbt = new NbtList();
        for (int i = 0; i < this.guardInventory.size(); ++i) {
            ItemStack itemstack = this.guardInventory.getStack(i);
            if (!itemstack.isEmpty()) {
                NbtCompound nbtnbt = new NbtCompound();
                nbtnbt.putByte("Slot", (byte) i);
                itemstack.writeNbt(nbtnbt);
                listnbt.add(nbtnbt);
            }
        }
        nbt.put("Inventory", listnbt);
        if (this.getPatrolPos() != null) {
            nbt.putInt("PatrolPosX", this.getPatrolPos().getX());
            nbt.putInt("PatrolPosY", this.getPatrolPos().getY());
            nbt.putInt("PatrolPosZ", this.getPatrolPos().getZ());
        }
        nbt.put("Gossips", this.gossips.serialize(NbtOps.INSTANCE).getValue());
        this.writeAngerToNbt(nbt);
    }

    private void maybeDecayGossip() {
        long i = getWorld().getTime();
        if (this.lastGossipDecayTime == 0L) {
            this.lastGossipDecayTime = i;
        } else if (i >= this.lastGossipDecayTime + 24000L) {
            this.gossips.decay();
            this.lastGossipDecayTime = i;
        }
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        switch (slot) {
            case HEAD:
                return this.guardInventory.getStack(0);
            case CHEST:
                return this.guardInventory.getStack(1);
            case LEGS:
                return this.guardInventory.getStack(2);
            case FEET:
                return this.guardInventory.getStack(3);
            case OFFHAND:
                return this.guardInventory.getStack(4);
            case MAINHAND:
                return this.guardInventory.getStack(5);
        }
        return ItemStack.EMPTY;
    }


    public VillagerGossips getGossips() {
        return this.gossips;
    }

    public int getPlayerEntityReputation(PlayerEntity player) {
        return this.gossips.getReputationFor(player.getUuid(), (gossipType) -> true);
    }

    @Nullable
    public LivingEntity getOwner() {
        try {
            UUID uuid = this.getOwnerId();
            boolean heroOfTheVillage = uuid != null && getWorld().getPlayerByUuid(uuid) != null && getWorld().getPlayerByUuid(uuid).hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE);
            return uuid == null || (getWorld().getPlayerByUuid(uuid) != null && (!heroOfTheVillage && GuardVillagersConfig.followHero) || !GuardVillagersConfig.followHero && getWorld().getPlayerByUuid(uuid) == null) ? null : getWorld().getPlayerByUuid(uuid);
        } catch (IllegalArgumentException illegalargumentexception) {
            return null;
        }
    }

    public boolean isOwner(LivingEntity entityIn) {
        return entityIn == this.getOwner();
    }

    @Nullable
    public UUID getOwnerId() {
        return this.dataTracker.get(OWNER_UNIQUE_ID).orElse(null);
    }

    public void setOwnerId(@Nullable UUID p_184754_1_) {
        this.dataTracker.set(OWNER_UNIQUE_ID, Optional.ofNullable(p_184754_1_));
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (this.isKicking()) {
            ((LivingEntity) target).takeKnockback(1.0F, MathHelper.sin(this.getYaw() * ((float) Math.PI / 180F)), (-MathHelper.cos(this.getYaw() * ((float) Math.PI / 180F))));
            this.kickTicks = 10;
            getWorld().sendEntityStatus(this, (byte) 4);
            this.lookAtEntity(target, 90.0F, 90.0F);
        }
        ItemStack hand = this.getMainHandStack();
        hand.damage(1, this, (entity) -> entity.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        return super.tryAttack(target);
    }

    @Override
    public void handleStatus(byte status) {
        if (status == 4) {
            this.kickTicks = 10;
        } else {
            super.handleStatus(status);
        }
    }

    @Override
    public boolean isImmobile() {
        return this.interacting || super.isImmobile();
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        if ((getWorld().getDifficulty() == Difficulty.NORMAL || getWorld().getDifficulty() == Difficulty.HARD) && damageSource.getAttacker() instanceof ZombieEntity) {
            ZombieVillagerEntity zombieguard = this.convertTo(EntityType.ZOMBIE_VILLAGER, true);
            if (getWorld().getDifficulty() != Difficulty.HARD && this.random.nextBoolean() || zombieguard == null) {
                return;
            }
            zombieguard.initialize((ServerWorldAccess) getWorld(), getWorld().getLocalDifficulty(zombieguard.getBlockPos()), SpawnReason.CONVERSION, new ZombieEntity.ZombieData(false, true), null);
            if (!this.isSilent()) getWorld().syncWorldEvent(null, 1026, this.getBlockPos(), 0);
            this.discard();
        }
        super.onDeath(damageSource);
    }

    @Override
    public ItemStack eatFood(World world, ItemStack stack) {
        if (stack.isFood()) {
            this.heal(stack.getItem().getFoodComponent().getHunger());
        }
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        super.eatFood(world, stack);
        return stack;
    }

    @Override
    public void tickMovement() {
        if (this.kickTicks > 0)
            --this.kickTicks;
        if (this.kickCoolDown > 0)
            --this.kickCoolDown;
        if (this.shieldCoolDown > 0)
            --this.shieldCoolDown;
        if (this.getHealth() < this.getMaxHealth() && this.age % 200 == 0) {
            this.heal(GuardVillagersConfig.amountOfHealthRegenerated);
        }
        if (!getWorld().isClient) this.tickAngerLogic((ServerWorld) getWorld(), true);
        this.tickHandSwing();
        super.tickMovement();
    }

    @Override
    public void tick() {
        this.maybeDecayGossip();
        super.tick();
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return SIZE_BY_POSE.getOrDefault(poseIn, EntityDimensions.changing(0.6F, 1.95F));
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        if (pose == EntityPose.CROUCHING) {
            return 1.40F;
        }
        return super.getActiveEyeHeight(pose, dimensions);
    }


    @Override
    protected void takeShieldHit(LivingEntity entityIn) {
        super.takeShieldHit(entityIn);
        if (entityIn.getMainHandStack().getItem() instanceof ShieldBlockItem) this.disableShield(true);
    }

    @Override
    public void damageShield(float amount) {
        if (this.activeItemStack.getItem().canPerformAction(this.activeItemStack, ToolActions.SHIELD_BLOCK)) {
            if (amount >= 3.0F) {
                int i = 1 + MathHelper.floor(amount);
                Hand hand = this.getActiveHand();
                this.activeItemStack.damage(i, this, (entity) -> entity.sendToolBreakStatus(hand));
                if (this.activeItemStack.isEmpty()) {
                    if (hand == Hand.MAIN_HAND) {
                        this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    } else {
                        this.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    }
                    this.activeItemStack = ItemStack.EMPTY;
                    this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + getWorld().random.nextFloat() * 0.4F);
                }
            }
        }
    }

    @Override
    public void setCurrentHand(Hand hand) {
        super.setCurrentHand(hand);
        ItemStack itemstack = this.getStackInHand(hand);
        if (itemstack.canPerformAction(ToolActions.SHIELD_BLOCK)) {

            EntityAttributeInstance modifiableattributeinstance = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            modifiableattributeinstance.removeModifier(USE_ITEM_SPEED_PENALTY);
            modifiableattributeinstance.addTemporaryModifier(USE_ITEM_SPEED_PENALTY);
        }
    }

    @Override
    public void stopUsingItem() {
        super.stopUsingItem();
        if (this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).hasModifier(USE_ITEM_SPEED_PENALTY))
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).removeModifier(USE_ITEM_SPEED_PENALTY);
    }

    public void disableShield(boolean increase) {
        float chance = 0.25F + (float) EnchantmentHelper.getEfficiency(this) * 0.05F;
        if (increase) chance += 0.75;
        if (this.random.nextFloat() < chance) {
            this.shieldCoolDown = 100;
            this.stopUsingItem();
            getWorld().sendEntityStatus(this, (byte) 30);
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(GUARD_VARIANT, 0);
        this.dataTracker.startTracking(DATA_CHARGING_STATE, false);
        this.dataTracker.startTracking(KICKING, false);
        this.dataTracker.startTracking(OWNER_UNIQUE_ID, Optional.empty());
        this.dataTracker.startTracking(FOLLOWING, false);
        this.dataTracker.startTracking(GUARD_POS, Optional.empty());
        this.dataTracker.startTracking(PATROLLING, false);
        this.dataTracker.startTracking(RUNNING_TO_EAT, false);
    }

    public boolean isCharging() {
        return this.dataTracker.get(DATA_CHARGING_STATE);
    }

    public void setChargingCrossbow(boolean charging) {
        this.dataTracker.set(DATA_CHARGING_STATE, charging);
    }

    public boolean isKicking() {
        return this.dataTracker.get(KICKING);
    }

    public void setKicking(boolean kicking) {
        this.dataTracker.set(KICKING, kicking);
    }

    @Override
    protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        for (EquipmentSlot equipmentslottype : EquipmentSlot.values()) {
            for (ItemStack stack : this.getStacksFromLootTable(equipmentslottype)) {
                this.equipStack(equipmentslottype, stack);
            }
        }
        this.handDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 100.0F;
        this.handDropChances[EquipmentSlot.OFFHAND.getEntitySlotId()] = 100.0F;
    }

    public List<ItemStack> getStacksFromLootTable(EquipmentSlot slot) {
        if (EQUIPMENT_SLOT_ITEMS.containsKey(slot)) {
            LootTable loot = getWorld().getServer().getLootManager().getTable(EQUIPMENT_SLOT_ITEMS.get(slot));
            LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerWorld) this.world)).parameter(LootContextParameters.THIS_ENTITY, this).random(this.getRandom());
            return loot.generateLoot(lootcontext$builder.build(GuardEntityLootTables.SLOT));
        }
        return null;
    }

    public int getGuardEntityVariant() {
        return this.dataTracker.get(GUARD_VARIANT);
    }

    public void setGuardEntityVariant(int typeId) {
        this.dataTracker.set(GUARD_VARIANT, typeId);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(0, new KickGoal(this));
        this.goalSelector.add(0, new GuardEatFoodGoal(this));
        this.goalSelector.add(0, new RaiseShieldGoal(this));
        this.goalSelector.add(1, new GuardRunToEatGoal(this));
        this.goalSelector.add(2, new RangedCrossbowAttackPassiveGoal<>(this, 1.0D, 8.0F));
        this.goalSelector.add(2, new RangedBowAttackPassiveGoal<>(this, 0.5D, 20, 15.0F));
        this.goalSelector.add(2, new GuardEntityMeleeGoal(this, 0.8D, true));
        this.goalSelector.add(3, new GuardEntity.FollowHeroGoal(this));
        if (GuardVillagersConfig.guardEntitysRunFromPolarBears)
            this.goalSelector.add(3, new FleeEntityGoal<>(this, PolarBearEntity.class, 12.0F, 1.0D, 1.2D));
        this.goalSelector.add(3, new WanderAroundPointOfInterestGoal(this, 0.5D, false));
        this.goalSelector.add(3, new IronGolemWanderAroundGoal(this, 0.5D));
        this.goalSelector.add(3, new MoveThroughVillageGoal(this, 0.5D, false, 4, () -> false));
        if (GuardVillagersConfig.guardEntitysOpenDoors) this.goalSelector.add(3, new GuardInteractDoorGoal(this, true));
        if (GuardVillagersConfig.guardEntityFormation) this.goalSelector.add(5, new FollowShieldGuards(this));
        if (GuardVillagersConfig.clericHealing) this.goalSelector.add(6, new RunToClericGoal(this));
        if (GuardVillagersConfig.armorerRepairGuardEntityArmor)
            this.goalSelector.add(6, new ArmorerRepairGuardArmorGoal(this));
        this.goalSelector.add(4, new WalkBackToCheckPointGoal(this, 0.5D));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5D));
        this.goalSelector.add(8, new LookAtEntityGoal(this, MerchantEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new GuardLookAtAndStopMovingWhenBeingTheInteractionTarget(this));
        this.targetSelector.add(5, new GuardEntity.DefendVillageGuardEntityGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, RavagerEntity.class, true));
        this.targetSelector.add(2, (new RevengeGoal(this, GuardEntity.class, IronGolemEntity.class)).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitchEntity.class, true));
        this.targetSelector.add(3, new HeroHurtByTargetGoal(this));
        this.targetSelector.add(3, new HeroHurtTargetGoal(this));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, RaiderEntity.class, true));
        if (GuardVillagersConfig.attackAllMobs)
            this.targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 5, true, true, (mob) -> mob instanceof Monster && !GuardVillagersConfig.mobBlackList.contains(mob.getSavedEntityId())));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, ZombieEntity.class, true));
        this.targetSelector.add(4, new UniversalAngerGoal<>(this, false));
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return false;
    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        this.shieldCoolDown = 8;
        if (this.getMainHandStack().getItem() instanceof CrossbowItem)
            this.shoot(this, 6.0F);
        if (this.getMainHandStack().getItem() instanceof BowItem) {
            ItemStack itemStack = this.getProjectileType(this.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, Items.BOW)));
            ItemStack hand = this.getActiveItem();
            PersistentProjectileEntity persistentProjectileEntity = ProjectileUtil.createArrowProjectile(this, itemStack, pullProgress);
            int powerLevel = EnchantmentHelper.getLevel(Enchantments.POWER, itemStack);
            if (powerLevel > 0) {
                persistentProjectileEntity.setDamage(persistentProjectileEntity.getDamage() + (double) powerLevel * 0.5D + 0.5D);
            }
            int punchLevel = EnchantmentHelper.getLevel(Enchantments.PUNCH, itemStack);
            if (punchLevel > 0) {
                persistentProjectileEntity.setPunch(punchLevel);
            }
            if (EnchantmentHelper.getLevel(Enchantments.FLAME, itemStack) > 0)
                persistentProjectileEntity.setFireTicks(100);
            double d = target.getX() - this.getX();
            double e = target.getBodyY(0.3333333333333333D) - persistentProjectileEntity.getY();
            double f = target.getZ() - this.getZ();
            double g = Math.sqrt(d * d + f * f);
            persistentProjectileEntity.setVelocity(d, e + g * 0.20000000298023224D, f, 1.6F, (float) (14 - this.getWorld().getDifficulty().getId() * 4));
            this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.getWorld().spawnEntity(persistentProjectileEntity);
            hand.damage(1, this, (entity) -> entity.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        }
    }

    @Override
    public void equipStack(EquipmentSlot slotIn, ItemStack stack) {
        super.equipStack(slotIn, stack);
        switch (slotIn) {
            case CHEST:
                if (this.guardInventory.getStack(1).isEmpty())
                    this.guardInventory.setStack(1, this.armorItems.get(slotIn.getEntitySlotId()));
                break;
            case FEET:
                if (this.guardInventory.getStack(3).isEmpty())
                    this.guardInventory.setStack(3, this.armorItems.get(slotIn.getEntitySlotId()));
                break;
            case HEAD:
                if (this.guardInventory.getStack(0).isEmpty())
                    this.guardInventory.setStack(0, this.armorItems.get(slotIn.getEntitySlotId()));
                break;
            case LEGS:
                if (this.guardInventory.getStack(2).isEmpty())
                    this.guardInventory.setStack(2, this.armorItems.get(slotIn.getEntitySlotId()));
                break;
            case MAINHAND:
                this.guardInventory.setStack(5, this.handItems.get(slotIn.getEntitySlotId()));
                break;
            case OFFHAND:
                this.guardInventory.setStack(4, this.handItems.get(slotIn.getEntitySlotId()));
                break;
        }
    }

    public int getGuardVariant() {
        return this.dataTracker.get(GUARD_VARIANT);
    }


    public ItemStack getProjectileType(ItemStack shootable) {
        if (shootable.getItem() instanceof RangedWeaponItem) {
            Predicate<ItemStack> predicate = ((RangedWeaponItem) shootable.getItem()).getHeldProjectiles();
            ItemStack itemstack = RangedWeaponItem.getHeldProjectile(this, predicate);
            return itemstack.isEmpty() ? new ItemStack(Items.ARROW) : itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public int getKickTicks() {
        return this.kickTicks;
    }

    public boolean isFollowing() {
        return this.dataTracker.get(FOLLOWING);
    }

    public void setFollowing(boolean following) {
        this.dataTracker.set(FOLLOWING, following);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return !GuardVillagersConfig.mobBlackList.contains(target.getSavedEntityId()) && !target.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && !this.isOwner(target) && !(target instanceof VillagerEntity) && !(target instanceof IronGolemEntity) && !(target instanceof GuardEntity) && super.canTarget(target);
    }

    @Override
    public void tickRiding() {
        super.tickRiding();
        if (this.getVehicle() instanceof PathAwareEntity creatureentity) {
            this.bodyYaw = creatureentity.bodyYaw;
        }
    }

    @Override
    public double getHeightOffset() {
        return -0.35D;
    }


    @Override
    public void postShoot() {
        this.despawnCounter = 0;
    }

    @Override
    public void setTarget(LivingEntity entity) {
        if (entity instanceof GuardEntity || entity instanceof VillagerEntity || entity instanceof IronGolemEntity)
            return;
        super.setTarget(entity);
    }


    public void gossip(VillagerEntity villager, long gameTime) {
        if ((gameTime < this.lastGossipTime || gameTime >= this.lastGossipTime + 1200L) && (gameTime < villager.gossipStartTime || gameTime >= villager.gossipStartTime + 1200L)) {
            this.gossips.shareGossipFrom(villager.getGossip(), this.random, 10);
            this.lastGossipTime = gameTime;
            villager.gossipStartTime = gameTime;
        }
    }

    @Override
    public void setCharging(boolean charging) {

    }

    @Override
    public void shoot(LivingEntity arg0, ItemStack arg1, ProjectileEntity arg2, float arg3) {
        this.shoot(this, arg0, arg2, arg3, 1.6F);
    }

    @Override
    public void knockback(LivingEntity entityIn) {
        if (this.isKicking()) {
            this.setKicking(false);
        }
        super.knockback(this);
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        boolean configValues = player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.giveGuardStuffHotv || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.setGuardPatrolHotv || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.giveGuardStuffHotv && GuardVillagersConfig.setGuardPatrolHotv || this.getPlayerEntityReputation(player) >= GuardVillagersConfig.reputationRequirement || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && !GuardVillagersConfig.giveGuardStuffHotv && !GuardVillagersConfig.setGuardPatrolHotv || this.getOwnerId() != null && this.getOwnerId().equals(player.getUuid());
        boolean inventoryRequirements = !player.shouldCancelInteraction();
        if (inventoryRequirements) {
            if (this.getTarget() != player && this.canMoveVoluntarily() && configValues) {
                if (player instanceof ServerPlayerEntity) {
                    this.openGui((ServerPlayerEntity) player);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.CONSUME;
        }
        return super.interactMob(player, hand);
    }

    @Override
    public void onInteractionWith(EntityInteraction interaction, Entity entity) {

    }

    @Override
    public void onInventoryChanged(Inventory sender) {

    }


    @Override
    public void damageArmor(DamageSource damageSource, float damage) {
        if (damage >= 0.0F) {
            damage = damage / 4.0F;
            if (damage < 1.0F) {
                damage = 1.0F;
            }
            for (int i = 0; i < this.guardInventory.size(); ++i) {
                ItemStack itemstack = this.guardInventory.getStack(i);
                if ((!damageSource.isFire() || !itemstack.getItem().isFireproof()) && itemstack.getItem() instanceof ArmorItem) {
                    int j = i;
                    itemstack.damage((int) damage, this, (p_214023_1_) -> {
                        p_214023_1_.sendEquipmentBreakStatus(EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, j));
                    });
                }
            }
        }
    }

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        if (world.getDifficulty() != Difficulty.PEACEFUL) {
            WitchEntity witchentity = EntityType.WITCH.create(world);
            if (witchentity == null) return;
            witchentity.copyPositionAndRotation(this);
            witchentity.initialize(world, world.getLocalDifficulty(witchentity.getBlockPos()), SpawnReason.CONVERSION, null, null);
            witchentity.setAiDisabled(this.isAiDisabled());
            witchentity.setCustomName(this.getCustomName());
            witchentity.setCustomNameVisible(this.isCustomNameVisible());
            witchentity.setPersistent();
            world.spawnNewEntityAndPassengers(witchentity);
            this.discard();
        } else {
            super.onStruckByLightning(world, lightning);
        }
    }

    @Override
    public UUID getAngryAt() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setAngryAt(UUID arg0) {
        this.persistentAngerTarget = arg0;
    }

    @Override
    public int getAngerTime() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    public void setAngerTime(int arg0) {
        this.remainingPersistentAngerTime = arg0;
    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerTime(angerTime.get(random));
    }

    public void openGui(ServerPlayerEntity player) {
        this.setOwnerId(player.getUuid());
        if (player.currentScreenHandler != player.playerScreenHandler) {
            player.closeHandledScreen();
        }
        this.interacting = true;
        if (!this.getWorld().isClient()) {
            player.openHandledScreen(new GuardScreenHandlerFactory());
        }
    }

    public void setGuardVariant(int i) {
        this.dataTracker.set(GUARD_VARIANT, i);
    }

    private class GuardScreenHandlerFactory implements ExtendedScreenHandlerFactory {
        private GuardEntity guard() {
            return GuardEntity.this;
        }

        @Override
        public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
            buf.writeVarInt(this.guard().getId());
        }

        @Override
        public Text getDisplayName() {
            return this.guard().getDisplayName();
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
            var guardInv = this.guard().guardInventory;
            return new GuardVillagerScreenHandler(syncId, inv, guardInv, this.guard());
        }
    }

    public boolean isEating() {
        return GuardEatFoodGoal.isConsumable(this.getActiveItem()) && this.isUsingItem();
    }

    public boolean isPatrolling() {
        return this.dataTracker.get(PATROLLING);
    }

    public void setPatrolling(boolean patrolling) {
        this.dataTracker.set(PATROLLING, patrolling);
    }

    @Override
    public boolean canUseRangedWeapon(RangedWeaponItem item) {
        return item instanceof BowItem || item instanceof CrossbowItem || super.canUseRangedWeapon(item);
    }

    public static class GuardEntityData implements EntityData {
        public final int variantData;

        public GuardEntityData(int type) {
            this.variantData = type;
        }
    }

    public static class DefendVillageGuardEntityGoal extends TrackTargetGoal {
        private final GuardEntity guard;
        private LivingEntity villageAggressorTarget;

        public DefendVillageGuardEntityGoal(GuardEntity guardIn) {
            super(guardIn, false, true);
            this.guard = guardIn;
            this.setControls(EnumSet.of(Goal.Control.TARGET, Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            Box box = this.guard.getBoundingBox().expand(10.0D, 8.0D, 10.0D);
            List<VillagerEntity> list = guard.getWorld().getNonSpectatingEntities(VillagerEntity.class, box);
            List<PlayerEntity> list1 = guard.getWorld().getNonSpectatingEntities(PlayerEntity.class, box);
            for (VillagerEntity villager : list) {
                for (PlayerEntity player : list1) {
                    int i = villager.getReputation(player);
                    if (i <= GuardVillagersConfig.reputationRequirementToBeAttacked) {
                        this.villageAggressorTarget = player;
                    }
                }
            }
            return villageAggressorTarget != null && !villageAggressorTarget.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && !this.villageAggressorTarget.isSpectator() && !((PlayerEntity) this.villageAggressorTarget).isCreative();
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
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public void tick() {
            if (guard.getOwner() != null && guard.getOwner().distanceTo(guard) > 3.0D) {
                guard.getNavigation().startMovingTo(guard.getOwner(), 0.7D);
                guard.getLookControl().lookAt(guard.getOwner());
            } else {
                guard.getNavigation().stop();
            }
        }

        @Override
        public boolean shouldContinue() {
            return this.canStart();
        }

        @Override
        public boolean canStart() {
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
        public boolean canStart() {
            return !(this.guard.getMainHandStack().getItem() instanceof CrossbowItem) && this.guard.getTarget() != null && !this.guard.isEating() && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && this.guard.getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = guard.getTarget();
            if (target != null) {
                if (target.distanceTo(guard) <= 3.0D && !guard.isBlocking()) {
                    guard.getMoveControl().strafeTo(-2.0F, 0.0F);
                    guard.lookAtEntity(target, 30.0F, 30.0F);
                }
                if (this.path != null && target.distanceTo(guard) <= 2.0D) guard.getNavigation().stop();
                super.tick();
            }
        }

        @Override
        protected double getSquaredMaxAttackDistance(LivingEntity attackTarget) {
            return super.getSquaredMaxAttackDistance(attackTarget) * 3.55D;
        }

        @Override
        protected void attack(LivingEntity enemy, double distToEnemySqr) {
            double d0 = this.getSquaredMaxAttackDistance(enemy);
            if (distToEnemySqr <= d0 && this.getCooldown() <= 0) {
                this.resetCooldown();
                this.guard.stopUsingItem();
                if (guard.shieldCoolDown == 0) this.guard.shieldCoolDown = 8;
                this.guard.swingHand(Hand.MAIN_HAND);
                this.guard.tryAttack(enemy);
            }
        }
    }
}
