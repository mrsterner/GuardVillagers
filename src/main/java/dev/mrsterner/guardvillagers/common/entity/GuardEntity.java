package dev.mrsterner.guardvillagers.common.entity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.GuardVillagersConfig;
import dev.mrsterner.guardvillagers.common.ToolAction;
import dev.mrsterner.guardvillagers.client.screen.GuardVillagerScreenHandler;
import dev.mrsterner.guardvillagers.common.GuardLootTables;
import dev.mrsterner.guardvillagers.common.entity.ai.goals.*;
import dev.mrsterner.guardvillagers.common.events.GuardVillagersEvents;
import dev.mrsterner.guardvillagers.mixin.MeleeAttackGoalAccessor;
import dev.mrsterner.guardvillagers.mixin.MobEntityAccessor;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.impl.blockrenderlayer.BlockRenderLayerMapImpl;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
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
import net.minecraft.entity.passive.*;
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
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.village.VillagerType;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.function.Predicate;

public class GuardEntity extends PathAwareEntity implements CrossbowUser, RangedAttackMob, Angerable, InventoryChangedListener {
    private static final UUID MODIFIER_UUID = UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E");
    private static final EntityAttributeModifier USE_ITEM_SPEED_PENALTY = new EntityAttributeModifier(MODIFIER_UUID, "Use item speed penalty", -0.25D, EntityAttributeModifier.Operation.ADDITION);
    private static final TrackedData<Optional<BlockPos>> GUARD_POS = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    private static final TrackedData<Boolean> PATROLLING = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> GUARD_VARIANT = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> RUNNING_TO_EAT = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DATA_CHARGING_STATE = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> EATING = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> KICKING = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FOLLOWING = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    protected static final TrackedData<Optional<UUID>> OWNER_UNIQUE_ID = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final Map<EntityPose, EntityDimensions> SIZE_BY_POSE = ImmutableMap.<EntityPose, EntityDimensions>builder()
    .put(EntityPose.STANDING, EntityDimensions.changing(0.6F, 1.95F)).put(EntityPose.SLEEPING, SLEEPING_DIMENSIONS)
    .put(EntityPose.FALL_FLYING, EntityDimensions.changing(0.6F, 0.6F))
    .put(EntityPose.SWIMMING, EntityDimensions.changing(0.6F, 0.6F))
    .put(EntityPose.SPIN_ATTACK, EntityDimensions.changing(0.6F, 0.6F))
    .put(EntityPose.CROUCHING, EntityDimensions.changing(0.6F, 1.75F))
    .put(EntityPose.DYING, EntityDimensions.fixed(0.2F, 0.2F)).build();
    public SimpleInventory guardInventory = new SimpleInventory(6);
    public int kickTicks;
    public int shieldCoolDown;
    public int kickCoolDown;
    public boolean interacting;
    private int remainingPersistentAngerTime;
    private static final UniformIntProvider angerTime = TimeHelper.betweenSeconds(20, 39);
    private UUID persistentAngerTarget;
    private static final Map<EquipmentSlot, Identifier> EQUIPMENT_SLOT_ITEMS = Util.make(Maps.newHashMap(),
    (slotItems) -> {
        slotItems.put(EquipmentSlot.MAINHAND, GuardLootTables.GUARD_MAIN_HAND);
        slotItems.put(EquipmentSlot.OFFHAND, GuardLootTables.GUARD_OFF_HAND);
        slotItems.put(EquipmentSlot.HEAD, GuardLootTables.GUARD_HELMET);
        slotItems.put(EquipmentSlot.CHEST, GuardLootTables.GUARD_CHEST);
        slotItems.put(EquipmentSlot.LEGS, GuardLootTables.GUARD_LEGGINGS);
        slotItems.put(EquipmentSlot.FEET, GuardLootTables.GUARD_FEET);
    });

    public GuardEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.guardInventory.addListener(this);
        this.setPersistent();
    }

    @Override
    public void equipStack(EquipmentSlot slotIn, ItemStack stack) {
        super.equipStack(slotIn, stack);
        switch (slotIn) {
            case CHEST:
                if (this.guardInventory.getStack(1).isEmpty())
                    this.guardInventory.setStack(1, ((MobEntityAccessor)this).armorItems().get(slotIn.getEntitySlotId()));
                break;
            case FEET:
                if (this.guardInventory.getStack(3).isEmpty())
                    this.guardInventory.setStack(3, ((MobEntityAccessor)this).armorItems().get(slotIn.getEntitySlotId()));
                break;
            case HEAD:
                if (this.guardInventory.getStack(0).isEmpty())
                    this.guardInventory.setStack(0, ((MobEntityAccessor)this).armorItems().get(slotIn.getEntitySlotId()));
                break;
            case LEGS:
                if (this.guardInventory.getStack(2).isEmpty())
                    this.guardInventory.setStack(2, ((MobEntityAccessor)this).armorItems().get(slotIn.getEntitySlotId()));
                break;
            case MAINHAND:
                if (this.guardInventory.getStack(5).isEmpty())
                    this.guardInventory.setStack(5, ((MobEntityAccessor)this).armorItems().get(slotIn.getEntitySlotId()));
                break;
            case OFFHAND:
                if (this.guardInventory.getStack(4).isEmpty())
                    this.guardInventory.setStack(4, ((MobEntityAccessor)this).armorItems().get(slotIn.getEntitySlotId()));
                break;
        }
    }

    @Override
    public void handleStatus(byte id) {
        if (id == 4) {
            this.kickTicks = 10;
        } else {
            super.handleStatus(id);
        }
    }

    @Override
    public boolean isImmobile() {
        return this.interacting || super.isImmobile();
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        this.setPersistent();
        int type = GuardEntity.getRandomTypeForBiome(world, this.getBlockPos());
        if (entityData instanceof GuardEntity.GuardData) {
            type = ((GuardEntity.GuardData) entityData).variantData;
            entityData = new GuardEntity.GuardData(type);
        }
        this.setGuardVariant(type);
        this.initEquipment(difficulty);
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    protected void initEquipment(LocalDifficulty difficulty) {
        for (EquipmentSlot equipmentslottype : EquipmentSlot.values()) {
            for (ItemStack stack : this.getItemsFromLootTable(equipmentslottype)) {
                this.equipStack(equipmentslottype, stack);
            }
        }
        this.handDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 100.0F;
        this.handDropChances[EquipmentSlot.OFFHAND.getEntitySlotId()] = 100.0F;
    }

    @Override
    protected void pushAway(Entity entity) {
        if (entity instanceof PathAwareEntity) {
            PathAwareEntity living = (PathAwareEntity) entity;
            boolean attackTargets = living.getTarget() instanceof VillagerEntity || living.getTarget() instanceof IronGolemEntity || living.getTarget() instanceof GuardEntity;
            if (attackTargets)
                this.setTarget(living);
        }
        super.pushAway(entity);
    }

    @Nullable
    public void setPatrolPos(BlockPos position) {
        this.dataTracker.set(GUARD_POS, Optional.ofNullable(position));
    }

    @Nullable
    public BlockPos getPatrolPos() {
        return this.dataTracker.get(GUARD_POS).orElse((BlockPos) null);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        if (this.isBlocking()) {
            return SoundEvents.ITEM_SHIELD_BLOCK;
        } else {
            return SoundEvents.ENTITY_VILLAGER_HURT;
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }


    @Override
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        for (int i = 0; i < this.guardInventory.size(); ++i) {
            ItemStack itemstack = this.guardInventory.getStack(i);
            if (!itemstack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemstack))
                this.dropStack(itemstack);
        }
    }

    public static int slotToInventoryIndex(EquipmentSlot slot) {
        switch (slot) {
            case CHEST:
                return 1;
            case FEET:
                return 3;
            case HEAD:
                return 0;
            case LEGS:
                return 2;
            default:
                break;
        }
        return 0;
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
        this.setGuardVariant(nbt.getInt("Type"));
        this.kickTicks = nbt.getInt("KickTicks");
        this.setFollowing(nbt.getBoolean("Following"));
        this.interacting = nbt.getBoolean("Interacting");
        this.setEating(nbt.getBoolean("Eating"));
        this.setPatrolling(nbt.getBoolean("Patrolling"));
        this.setRunningToEat(nbt.getBoolean("RunningToEat"));
        this.shieldCoolDown = nbt.getInt("KickCooldown");
        this.kickCoolDown = nbt.getInt("ShieldCooldown");
        if (nbt.contains("PatrolPosX")) {
            int x = nbt.getInt("PatrolPosX");
            int y = nbt.getInt("PatrolPosY");
            int z = nbt.getInt("PatrolPosZ");
            this.dataTracker.set(GUARD_POS, Optional.ofNullable(new BlockPos(x, y, z)));
        }
        NbtList listnbt = nbt.getList("Inventory", 10);
        for (int i = 0; i < listnbt.size(); ++i) {
            NbtCompound compoundnbt = listnbt.getCompound(i);
            int j = compoundnbt.getByte("Slot") & 255;
            this.guardInventory.setStack(j, ItemStack.fromNbt(compoundnbt));
        }

        if (nbt.contains("ArmorItems", 9)) {
            NbtList armorItems = nbt.getList("ArmorItems", 10);
            for (int i = 0; i < ((MobEntityAccessor)this).armorItems().size(); ++i) {
                int index = GuardEntity.slotToInventoryIndex(MobEntity.getPreferredEquipmentSlot(ItemStack.fromNbt(armorItems.getCompound(i))));
                this.guardInventory.setStack(index, ItemStack.fromNbt(armorItems.getCompound(i)));
            }
        }
        if (nbt.contains("HandItems", 9)) {
            NbtList handItems = nbt.getList("HandItems", 10);
            for (int i = 0; i < ((MobEntityAccessor)this).handItems().size(); ++i) {
                int handSlot = i == 0 ? 5 : 4;
                this.guardInventory.setStack(handSlot, ItemStack.fromNbt(handItems.getCompound(i)));
            }
        }

        if (!world.isClient())
            this.readAngerFromNbt(this.world, nbt);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Type", this.getGuardVariant());
        nbt.putInt("KickTicks", this.kickTicks);
        nbt.putInt("ShieldCooldown", this.shieldCoolDown);
        nbt.putInt("KickCooldown", this.kickCoolDown);
        nbt.putBoolean("Following", this.isFollowing());
        nbt.putBoolean("Interacting", this.interacting);
        nbt.putBoolean("Eating", this.isEating());
        nbt.putBoolean("Patrolling", this.isPatrolling());
        nbt.putBoolean("RunningToEat", this.isRunningToEat());
        if (this.getOwnerId() != null) {
            nbt.putUuid("Owner", this.getOwnerId());
        }
        NbtList listnbt = new NbtList();
        for (int i = 0; i < this.guardInventory.size(); ++i) {
            ItemStack itemstack = this.guardInventory.getStack(i);
            NbtCompound compoundnbt = new NbtCompound();
            compoundnbt.putByte("Slot", (byte) i);
            itemstack.writeNbt(compoundnbt);
            listnbt.add(compoundnbt);

        }
        nbt.put("Inventory", listnbt);
        if (this.getPatrolPos() != null) {
            nbt.putInt("PatrolPosX", this.getPatrolPos().getX());
            nbt.putInt("PatrolPosY", this.getPatrolPos().getY());
            nbt.putInt("PatrolPosZ", this.getPatrolPos().getZ());
        }
        this.readAngerFromNbt(this.world, nbt);
    }

    public void setOwnerId(@Nullable UUID p_184754_1_) {
        this.dataTracker.set(OWNER_UNIQUE_ID, Optional.ofNullable(p_184754_1_));
    }

    public void setFollowing(boolean following) {
        this.dataTracker.set(FOLLOWING, following);
    }

    public void setEating(boolean eating) {
        this.dataTracker.set(EATING, eating);
    }

    public void setPatrolling(boolean patrolling) {
        this.dataTracker.set(PATROLLING, patrolling);
    }

    public boolean isRunningToEat() {
        return this.dataTracker.get(RUNNING_TO_EAT);
    }

    public void setRunningToEat(boolean running) {
        this.dataTracker.set(RUNNING_TO_EAT, running);
    }

    public UUID getOwnerId() {
        return this.dataTracker.get(OWNER_UNIQUE_ID).orElse(null);
    }

    public int getGuardVariant() {
        return this.dataTracker.get(GUARD_VARIANT);
    }


    public int getKickTicks() {
        return this.kickTicks;
    }

    public void setKicking(boolean kicking) {
        this.dataTracker.set(KICKING, kicking);
    }

    public boolean isFollowing() {
        return this.dataTracker.get(FOLLOWING);
    }

    public boolean isEating() {
        return this.dataTracker.get(EATING);
    }

    public boolean isPatrolling() {
        return this.dataTracker.get(PATROLLING);
    }

    public ItemStack getPickedResult(HitResult target) {
        return new ItemStack(GuardVillagers.GUARD_SPAWN_EGG.asItem());
    }



    @Nullable
    public LivingEntity getOwner() {
        try {
            UUID uuid = this.getOwnerId();
            return (uuid == null || uuid != null && this.world.getPlayerByUuid(uuid) != null
            && !this.world.getPlayerByUuid(uuid).hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) ? null
            : this.world.getPlayerByUuid(uuid);
        } catch (IllegalArgumentException illegalargumentexception) {
            return null;
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (this.isKicking()) {
            ((LivingEntity) target).takeKnockback(1.0F, Math.sin(this.getYaw() * ((float) Math.PI / 180F)), (-Math.cos(this.getYaw() * ((float) Math.PI / 180F))));
            this.kickTicks = 10;
            this.world.sendEntityStatus(this, (byte) 4);
            this.lookAtEntity(target, 90.0F, 90.0F);
        }
        ItemStack hand = this.getMainHandStack();
        hand.damage(1, this, (entity) -> entity.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        return super.tryAttack(target);
    }

    public boolean isKicking() {
        return this.dataTracker.get(KICKING);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(0, new KickGoal(this));
        this.goalSelector.add(0, new GuardEatFoodGoal(this));
        this.goalSelector.add(0, new RaiseShieldGoal(this));
        this.goalSelector.add(1, new GuardRunToEatGoal(this));
        this.goalSelector.add(1, new GuardSetRunningToEatGoal(this, 1.0D));
        this.goalSelector.add(2, new RangedCrossbowAttackPassiveGoal<>(this, 1.0D, 8.0F));
        this.goalSelector.add(2, new RangedBowAttackPassiveGoal<>(this, 0.5D, 20, 15.0F));
        this.goalSelector.add(2, new GuardMeleeGoal(this, 0.8D, true));
        this.goalSelector.add(3, new GuardEntity.FollowHeroGoal(this));
        if (GuardVillagersConfig.get().GuardsRunFromPolarBears)
            this.goalSelector.add(3, new FleeEntityGoal<>(this, PolarBearEntity.class, 12.0F, 1.0D, 1.2D));
        this.goalSelector.add(3, new WanderAroundPointOfInterestGoal(this, 0.5D, false));
        this.goalSelector.add(3, new IronGolemWanderAroundGoal(this, 0.5D));
        this.goalSelector.add(3, new MoveThroughVillageGoal(this, 0.5D, false, 4, () -> false));
        if (GuardVillagersConfig.get().GuardsOpenDoors)
            this.goalSelector.add(3, new LongDoorInteractGoal(this, true) {
                @Override
                public void start() {
                    this.mob.swingHand(Hand.MAIN_HAND);
                    super.start();
                }
            });
        if (GuardVillagersConfig.get().GuardFormation)
            this.goalSelector.add(5, new FollowShieldGuards(this)); // phalanx
        if (GuardVillagersConfig.get().ClericHealing)
            this.goalSelector.add(6, new RunToClericGoal(this));
        if (GuardVillagersConfig.get().armorerRepairGuardArmor)
            this.goalSelector.add(6, new ArmorerRepairGuardArmorGoal(this));
        this.goalSelector.add(4, new WalkBackToCheckPointGoal(this, 0.5D));
        this.goalSelector.add(8, new LookAtEntityGoal(this, MerchantEntity.class, 8.0F));
        this.goalSelector.add(8, new WanderAroundGoal(this, 0.5D));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.targetSelector.add(5, new GuardEntity.DefendVillageGuardGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, RavagerEntity.class, true));
        this.targetSelector.add(2, (new RevengeGoal(this, GuardEntity.class, IronGolemEntity.class)).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitchEntity.class, true));
        this.targetSelector.add(3, new HeroHurtByTargetGoal(this));
        this.targetSelector.add(3, new HeroHurtTargetGoal(this));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IllagerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, RaiderEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IllusionerEntity.class, true));
        if (GuardVillagersConfig.get().AttackAllMobs) {
            this.targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 5, true, true, (mob) -> mob instanceof Monster && !GuardVillagersConfig.get().MobBlackList.contains(mob.getEntityName())));
        }
        this.targetSelector.add(3,
        new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, ZombieEntity.class, true));
        this.targetSelector.add(4, new UniversalAngerGoal<>(this, false));
    }

    @Override
    public void onDeath(DamageSource source) {
        if ((this.world.getDifficulty() == Difficulty.NORMAL || this.world.getDifficulty() == Difficulty.HARD)
        && source.getSource() instanceof ZombieEntity) {
            if (this.world.getDifficulty() != Difficulty.HARD && this.random.nextBoolean()) {
                return;
            }
            ZombieVillagerEntity zombieguard = this.convertTo(EntityType.ZOMBIE_VILLAGER, true);
            zombieguard.initialize((ServerWorldAccess) this.world,
            this.world.getLocalDifficulty(zombieguard.getBlockPos()), SpawnReason.CONVERSION,
            new ZombieEntity.ZombieData(false, true), (NbtCompound) null);
            if (!this.isSilent())
                this.world.syncWorldEvent((PlayerEntity) null, 1026, this.getBlockPos(), 0);
            this.discard();
        }
        super.onDeath(source);
    }



    @Override
    protected void consumeItem() {
        Hand interactionhand = this.getActiveHand();
        if (!this.activeItemStack.equals(this.getStackInHand(interactionhand))) {
            this.stopUsingItem();
        } else {
            if (!this.activeItemStack.isEmpty() && this.isUsingItem()) {
                this.spawnConsumptionEffects(this.activeItemStack, 16);
                ItemStack copy = this.activeItemStack.copy();
                ItemStack itemStack = GuardVillagersEvents.ON_CONSUMED_EVENT.invoker().onConsumed(this, copy, getItemUseTimeLeft(), this.activeItemStack.finishUsing(this.world,this));
                if (itemStack != this.activeItemStack) {
                    this.setStackInHand(interactionhand, itemStack);
                }
                if (!this.activeItemStack.isFood())
                    this.activeItemStack.decrement(1);
                this.stopUsingItem();
            }
        }
    }

    @Override
    public ItemStack eatFood(World world, ItemStack stack) {
        if (stack.isFood()) {
            this.heal(stack.getItem().getFoodComponent().getHunger());
        }
        super.eatFood(world, stack);
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5F,
        world.random.nextFloat() * 0.1F + 0.9F);
        this.setEating(false);
        return stack;
    }

    @Override
    public void tickMovement() {
        if (this.kickTicks > 0) {
            --this.kickTicks;
        }
        if (this.kickCoolDown > 0) {
            --this.kickCoolDown;
        }
        if (this.shieldCoolDown > 0) {
            --this.shieldCoolDown;
        }
        if (this.getHealth() < this.getMaxHealth() && this.age % 200 == 0) {
            this.heal(GuardVillagersConfig.get().amountOfHealthRegenerated);
        }
        if (!this.world.isClient())
            this.tickAngerLogic((ServerWorld) this.world, true);
        this.tickHandSwing();
        super.tickMovement();
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return SIZE_BY_POSE.getOrDefault(poseIn, EntityDimensions.changing(0.6F, 1.95F));
    }

    @Override
    public float getActiveEyeHeight(EntityPose poseIn, EntityDimensions sizeIn) {
        if (poseIn == EntityPose.CROUCHING) {
            return 1.40F;
        }
        return super.getActiveEyeHeight(poseIn, sizeIn);
    }

    @Override
    protected void takeShieldHit(LivingEntity entityIn) {
        super.takeShieldHit(entityIn);
        if (entityIn.getMainHandStack().getItem() instanceof AxeItem)
            this.disableShield(true);
    }

    @Override
    protected void damageShield(float damage) {
        if (canPerformAction(this.activeItemStack,ToolActions.SHIELD_BLOCK)) {
            if (damage >= 3.0F) {
                int i = 1 + MathHelper.floor(damage);
                Hand hand = this.getActiveHand();
                this.activeItemStack.damage(i, this, (entity) -> entity.sendToolBreakStatus(hand));
                if (this.activeItemStack.isEmpty()) {
                    if (hand == Hand.MAIN_HAND) {
                        this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    } else {
                        this.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    }
                    this.activeItemStack = ItemStack.EMPTY;
                    this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + this.world.random.nextFloat() * 0.4F);
                }
            }
        }
    }

    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return ToolActions.DEFAULT_SHIELD_ACTIONS.contains(toolAction);
    }

    @Override
    public void setCurrentHand(Hand hand) {
        ItemStack itemstack = this.getStackInHand(hand);
        if (canPerformAction(itemstack,ToolActions.SHIELD_BLOCK)) {
            EntityAttributeInstance modifiableattributeinstance = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            modifiableattributeinstance.removeModifier(USE_ITEM_SPEED_PENALTY);
            modifiableattributeinstance.addTemporaryModifier(USE_ITEM_SPEED_PENALTY);
        }
        super.setCurrentHand(hand);
    }

    @Override
    public void stopUsingItem() {
        if (this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).hasModifier(USE_ITEM_SPEED_PENALTY))
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).removeModifier(USE_ITEM_SPEED_PENALTY);
        super.stopUsingItem();
    }

    public void disableShield(boolean increase) {
        float chance = 0.25F + (float) EnchantmentHelper.getEfficiency(this) * 0.05F;
        if (increase)
            chance += 0.75;
        if (this.random.nextFloat() < chance) {
            this.shieldCoolDown = 100;
            this.stopUsingItem();
            this.world.sendEntityStatus(this, (byte) 30);
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(GUARD_VARIANT, 0);
        this.dataTracker.startTracking(DATA_CHARGING_STATE, false);
        this.dataTracker.startTracking(KICKING, false);
        this.dataTracker.startTracking(OWNER_UNIQUE_ID, Optional.empty());
        this.dataTracker.startTracking(EATING, false);
        this.dataTracker.startTracking(FOLLOWING, false);
        this.dataTracker.startTracking(GUARD_POS, Optional.empty());
        this.dataTracker.startTracking(PATROLLING, false);
        this.dataTracker.startTracking(RUNNING_TO_EAT, false);
    }



    public static int getRandomTypeForBiome(WorldAccess world, BlockPos pos) {
        VillagerType type = VillagerType.forBiome(world.getBiomeKey(pos));
        if (type == VillagerType.SNOW)
            return 6;
        else if (type == VillagerType.TAIGA)
            return 5;
        else if (type == VillagerType.JUNGLE)
            return 4;
        else if (type == VillagerType.SWAMP)
            return 3;
        else if (type == VillagerType.SAVANNA)
            return 2;
        if (type == VillagerType.DESERT)
            return 1;
        else return 0;
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return false;
    }

    public List<ItemStack> getItemsFromLootTable(EquipmentSlot slot) {
        if (EQUIPMENT_SLOT_ITEMS.containsKey(slot)) {
            LootTable loot = this.world.getServer().getLootManager().getTable(EQUIPMENT_SLOT_ITEMS.get(slot));
            LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerWorld) this.world)).parameter(LootContextParameters.THIS_ENTITY, this).random(this.getRandom());
            return loot.generateLoot(lootcontext$builder.build(GuardLootTables.SLOT));
        }
        return null;
    }

    @Override
    public void setCharging(boolean charging) {

    }

    @Override
    public void shoot(LivingEntity target, ItemStack crossbow, ProjectileEntity projectile, float multiShotSpray) {
        this.shoot(this, target, projectile, multiShotSpray, 1.6F);
    }

    @Override
    public void setTarget(LivingEntity entity) {
        if (entity instanceof GuardEntity || entity instanceof VillagerEntity || entity instanceof IronGolemEntity)
            return;
        super.setTarget(entity);
    }


    @Override
    protected void knockback(LivingEntity entityIn) {
        if (this.isKicking()) {
            this.setKicking(false);
        }
        super.knockback(this);
    }



    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        boolean configValues = !GuardVillagersConfig.get().giveGuardStuffHOTV || !GuardVillagersConfig.get().setGuardPatrolHotv
        || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.get().giveGuardStuffHOTV
        || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.get().setGuardPatrolHotv
        || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.get().giveGuardStuffHOTV
        && GuardVillagersConfig.get().setGuardPatrolHotv;
        boolean inventoryRequirements = !player.shouldCancelInteraction() && this.onGround;
        if (configValues && inventoryRequirements) {
            if (this.getTarget() != player && this.canMoveVoluntarily()) {
                    this.openGui(player);
                    return ActionResult.success(this.world.isClient());
            }
        }
        return super.interactMob(player, hand);
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

    public void openGui(PlayerEntity player) {
        if (player.world != null && !this.world.isClient()) {
            this.interacting = true;
            player.openHandledScreen(new GuardScreenHandlerFactory());
        }
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
        .add(EntityAttributes.GENERIC_MAX_HEALTH, 20)
        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5)
        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
        .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20);
    }



    @Override
    public void attack(LivingEntity target, float pullProgress) {
        this.shieldCoolDown = 8;
        if (this.getMainHandStack().getItem() instanceof CrossbowItem)
            this.shoot(this, 6.0F);
        if (this.getMainHandStack().getItem() instanceof BowItem) {
            ItemStack itemStack = this.getArrowType(this.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, Items.BOW)));
            ItemStack hand = this.getActiveItem();
            PersistentProjectileEntity persistentProjectileEntity = ProjectileUtil.createArrowProjectile(this, itemStack, pullProgress);
            int powerLevel = EnchantmentHelper.getLevel(Enchantments.POWER, itemStack);
            if (powerLevel > 0){
                persistentProjectileEntity.setDamage(persistentProjectileEntity.getDamage() + (double) powerLevel * 0.5D + 0.5D);
            }
            int punchLevel = EnchantmentHelper.getLevel(Enchantments.PUNCH, itemStack);
            if (punchLevel > 0){
                persistentProjectileEntity.setPunch(punchLevel);
            }
            if (EnchantmentHelper.getLevel(Enchantments.FLAME, itemStack) > 0)
                persistentProjectileEntity.setFireTicks(100);
            double d = target.getX() - this.getX();
            double e = target.getBodyY(0.3333333333333333D) - persistentProjectileEntity.getY();
            double f = target.getZ() - this.getZ();
            double g = Math.sqrt(d * d + f * f);
            persistentProjectileEntity.setVelocity(d, e + g * 0.20000000298023224D, f, 1.6F, (float)(14 - this.world.getDifficulty().getId() * 4));
            this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.world.spawnEntity(persistentProjectileEntity);
            hand.damage(1, this, (entity) -> entity.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        }
    }

    @Override
    public ItemStack getArrowType(ItemStack shootable) {
        if (shootable.getItem() instanceof RangedWeaponItem) {
            Predicate<ItemStack> predicate = ((RangedWeaponItem) shootable.getItem()).getHeldProjectiles();
            ItemStack itemstack = RangedWeaponItem.getHeldProjectile(this, predicate);
            return itemstack.isEmpty() ? new ItemStack(Items.ARROW) : itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return !GuardVillagersConfig.get().MobBlackList.contains(target.getEntityName())
        && !target.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && !this.isOwner(target)
        && !(target instanceof VillagerEntity) && !(target instanceof IronGolemEntity) && !(target instanceof GuardEntity)
        && super.canTarget(target);
    }

    public boolean isOwner(LivingEntity entityIn) {
        return entityIn == this.getOwner();
    }


    @Override
    public void tickRiding() {
        super.tickRiding();
        if (this.getVehicle() instanceof PathAwareEntity) {
            PathAwareEntity creatureentity = (PathAwareEntity) this.getVehicle();
            this.bodyYaw = creatureentity.bodyYaw;
        }
    }

    @Override
    public void postShoot() {
        this.despawnCounter = 0;
    }


    @Override
    public int getAngerTime() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    public void setAngerTime(int ticks) {
        this.remainingPersistentAngerTime = ticks;
    }

    @Nullable
    @Override
    public UUID getAngryAt() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setAngryAt(@Nullable UUID uuid) {
        this.persistentAngerTarget = uuid;
    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerTime(angerTime.get(random));
    }

    @Override
    public void onInventoryChanged(Inventory sender) {

    }
    @Override
    protected void damageArmor(DamageSource damageSource, float damage) {
        if (damage >= 0.0F) {
            damage = damage / 4.0F;
            if (damage < 1.0F) {
                damage = 1.0F;
            }
            for (int i = 0; i < this.guardInventory.size(); ++i) {
                ItemStack itemstack = this.guardInventory.getStack(i);
                if ((!damageSource.isFire() || !itemstack.getItem().isFireproof())
                && itemstack.getItem() instanceof ArmorItem) {
                    int j = i;
                    itemstack.damage((int) damage, this, (p_214023_1_) -> {
                        p_214023_1_.sendEquipmentBreakStatus(EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, j));
                    });
                }
            }
        }
    }

    @Override
    public void onStruckByLightning(ServerWorld serverWorld, LightningEntity lightningEntity) {
        if (serverWorld.getDifficulty() != Difficulty.PEACEFUL) {
            WitchEntity witchEntity = EntityType.WITCH.create(serverWorld);
            if (witchEntity == null)
                return;
            witchEntity.copyPositionAndRotation(this);
            witchEntity.initialize(serverWorld, world.getLocalDifficulty(witchEntity.getBlockPos()), SpawnReason.CONVERSION, (EntityData)null, (NbtCompound)null);
            witchEntity.setAiDisabled(this.isAiDisabled());
            witchEntity.setCustomName(this.getCustomName());
            witchEntity.setCustomNameVisible(this.isCustomNameVisible());
            witchEntity.setPersistent();
            serverWorld.spawnEntity(witchEntity);
            this.discard();
        } else {
            super.onStruckByLightning(serverWorld, lightningEntity);
        }
    }

    public void setGuardVariant(int typeId) {
        this.dataTracker.set(GUARD_VARIANT, typeId);
    }


    public static class GuardData implements EntityData {
        public final int variantData;

        public GuardData(int type) {
            this.variantData = type;
        }
    }

    public static class DefendVillageGuardGoal extends TrackTargetGoal {
        private final GuardEntity guard;
        private LivingEntity villageAggressorTarget;

        public DefendVillageGuardGoal(GuardEntity guardIn) {
            super(guardIn, false, true);
            this.guard = guardIn;
            this.setControls(EnumSet.of(Goal.Control.TARGET, Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            Box axisalignedbb = this.guard.getBoundingBox().expand(10.0D, 8.0D, 10.0D);
            List<VillagerEntity> list = guard.world.getNonSpectatingEntities(VillagerEntity.class, axisalignedbb);
            List<PlayerEntity> list1 = guard.world.getNonSpectatingEntities(PlayerEntity.class, axisalignedbb);
            for (LivingEntity livingentity : list) {
                VillagerEntity villagerentity = (VillagerEntity) livingentity;
                for (PlayerEntity playerentity : list1) {
                    int i = villagerentity.getReputation(playerentity);
                    if (i <= -100) {
                        this.villageAggressorTarget = playerentity;
                    }
                }
            }
            return villageAggressorTarget != null && !villageAggressorTarget.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)
            && !this.villageAggressorTarget.isSpectator()
            && !((PlayerEntity) this.villageAggressorTarget).isCreative();
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
            guard = mob;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public void start() {
            super.start();
            if (guard.getOwner() != null) {
                guard.getNavigation().startMovingTo(guard.getOwner(), 0.9D);
            }
        }

        @Override
        public void tick() {
            if (guard.getOwner() != null) {
                guard.getNavigation().startMovingTo(guard.getOwner(), 0.9D);
            }
        }

        @Override
        public boolean shouldContinue() {
            return guard.isFollowing() && this.canStart();
        }

        @Override
        public boolean canStart() {
            List<PlayerEntity> list = this.guard.world.getNonSpectatingEntities(PlayerEntity.class, this.guard.getBoundingBox().expand(10.0D));
            if (!list.isEmpty()) {
                for (LivingEntity mob : list) {
                    PlayerEntity player = (PlayerEntity) mob;
                    if (!player.isInvisible() && player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
                        guard.setOwnerId(player.getUuid());
                        return guard.isFollowing();
                    }
                }
            }
            return false;
        }

        @Override
        public void stop() {
            this.guard.getNavigation().stop();
            if (guard.getOwner() != null && !guard.getOwner().hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
                guard.setOwnerId(null);
                guard.setFollowing(false);
            }
        }
    }


    public static class GuardMeleeGoal extends MeleeAttackGoal {
        public final GuardEntity guard;

        public GuardMeleeGoal(GuardEntity guard, double speedIn, boolean useLongMemory) {
            super(guard, speedIn, useLongMemory);
            this.guard = guard;
        }

        @Override
        public boolean canStart() {
            return !(this.guard.getMainHandStack().getItem() instanceof CrossbowItem) && this.guard.getTarget() != null
            && !this.guard.isEating() && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && this.guard.getTarget() != null
            && !(this.guard.getMainHandStack().getItem() instanceof CrossbowItem);
        }

        @Override
        public void tick() {
            LivingEntity target = guard.getTarget();
            if (target != null) {
                if (target.distanceTo(guard) <= 3.0D && !guard.isBlocking()) {
                    guard.getMoveControl().strafeTo(-2.0F, 0.0F);
                    guard.lookAtEntity(target, 30.0F, 30.0F);
                }
                if (((MeleeAttackGoalAccessor)this).path() != null && target.distanceTo(guard) <= 2.0D)
                    guard.getNavigation().stop();
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
                if (guard.shieldCoolDown == 0)
                    this.guard.shieldCoolDown = 8;
                this.guard.swingHand(Hand.MAIN_HAND);
                this.guard.tryAttack(enemy);
            }
        }
    }

    public static class ToolActions {
        public static final ToolAction SHIELD_BLOCK = ToolAction.get("shield_block");
        public static final Set<ToolAction> DEFAULT_SHIELD_ACTIONS = of(SHIELD_BLOCK);

        private static Set<ToolAction> of(ToolAction... actions) {
            return Stream.of(actions).collect(Collectors.toCollection(Sets::newIdentityHashSet));
        }
    }
}
