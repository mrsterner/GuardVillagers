package dev.mrsterner.guardvillagers.common.entity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import dev.mrsterner.guardvillagers.common.GuardLootTables;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.village.VillagerType;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        for (int i = 0; i < this.guardInventory.size(); ++i) {
            ItemStack itemstack = this.guardInventory.getStack(i);
            if (!itemstack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemstack))
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
        /*
        if (nbt.contains("ArmorItems", 9)) {
            NbtList armorItems = nbt.getList("ArmorItems", 10);
            for (int i = 0; i < this.armorItems.size(); ++i) {
                int index = GuardEntity
                .slotToInventoryIndex(MobEntity.getEquipmentForSlot(ItemStack.fromNbt(armorItems.getCompound(i))));
                this.guardInventory.setItem(index, ItemStack.fromNbt(armorItems.getCompound(i)));
            }
        }

         */
        /*
        if (nbt.contains("HandItems", 9)) {
            NbtList handItems = nbt.getList("HandItems", 10);
            for (int i = 0; i < this.handItems.size(); ++i) {
                int handSlot = i == 0 ? 5 : 4;
                this.guardInventory.setStack(handSlot, ItemStack.fromNbt(handItems.getCompound(i)));
            }
        }

         */
        if (!world.isClient())
            this.readAngerFromNbt((ServerWorld) this.world, nbt);
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
            if (!itemstack.isEmpty()) {
                NbtCompound compoundnbt = new NbtCompound();
                compoundnbt.putByte("Slot", (byte) i);
                itemstack.writeNbt(compoundnbt);
                listnbt.add(compoundnbt);
            }
        }
        nbt.put("Inventory", listnbt);
        if (this.getPatrolPos() != null) {
            nbt.putInt("PatrolPosX", this.getPatrolPos().getX());
            nbt.putInt("PatrolPosY", this.getPatrolPos().getY());
            nbt.putInt("PatrolPosZ", this.getPatrolPos().getZ());
        }
        this.readAngerFromNbt(nbt);
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

    public boolean isFollowing() {
        return this.dataTracker.get(FOLLOWING);
    }

    public boolean isEating() {
        return this.dataTracker.get(EATING);
    }

    public boolean isPatrolling() {
        return this.dataTracker.get(PATROLLING);
    }

        // TODO
    /*
    public ItemStack getPickedResult(HitResult target) {
        return new ItemStack(GuardItems.GUARD_SPAWN_EGG.get());
    }

     */

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
    protected void registerGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(0, new KickGoal(this));
        this.goalSelector.add(0, new GuardEatFoodGoal(this));
        this.goalSelector.add(0, new RaiseShieldGoal(this));
        this.goalSelector.add(1, new GuardRunToEatGoal(this));
        this.goalSelector.add(1, new GuardSetRunningToEatGoal(this, 1.0D));
        this.goalSelector.add(2, new RangedCrossbowAttackPassiveGoal<>(this, 1.0D, 8.0F));
        this.goalSelector.add(2, new RangedBowAttackPassiveGoal<>(this, 0.5D, 20, 15.0F));
        this.goalSelector.add(2, new Guard.GuardMeleeGoal(this, 0.8D, true));
        this.goalSelector.add(3, new Guard.FollowHeroGoal(this));
        if (GuardConfig.GuardsRunFromPolarBears)
            this.goalSelector.add(3, new AvoidEntityGoal<>(this, PolarBear.class, 12.0F, 1.0D, 1.2D));
        this.goalSelector.add(3, new MoveBackToVillageGoal(this, 0.5D, false));
        this.goalSelector.add(3, new GolemRandomStrollInVillageGoal(this, 0.5D));
        this.goalSelector.add(3, new MoveThroughVillageGoal(this, 0.5D, false, 4, () -> false));
        if (GuardConfig.GuardsOpenDoors)
            this.goalSelector.add(3, new OpenDoorGoal(this, true) {
                @Override
                public void start() {
                    this.mob.swing(InteractionHand.MAIN_HAND);
                    super.start();
                }
            });
        if (GuardConfig.GuardFormation)
            this.goalSelector.add(5, new FollowShieldGuards(this)); // phalanx
        if (GuardConfig.ClericHealing)
            this.goalSelector.add(6, new RunToClericGoal(this));
        if (GuardConfig.armorerRepairGuardArmor)
            this.goalSelector.add(6, new ArmorerRepairGuardArmorGoal(this));
        this.goalSelector.add(4, new WalkBackToCheckPointGoal(this, 0.5D));
        this.goalSelector.add(8, new LookAtPlayerGoal(this, AbstractVillager.class, 8.0F));
        this.goalSelector.add(8, new RandomStrollGoal(this, 0.5D));
        this.goalSelector.add(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.add(5, new Guard.DefendVillageGuardGoal(this));
        this.targetSelector.add(2, new NearestAttackableTargetGoal<>(this, Ravager.class, true));
        this.targetSelector.add(2, (new HurtByTargetGoal(this, Guard.class, IronGolem.class)).setAlertOthers());
        this.targetSelector.add(2, new NearestAttackableTargetGoal<>(this, Witch.class, true));
        this.targetSelector.add(3, new HeroHurtByTargetGoal(this));
        this.targetSelector.add(3, new HeroHurtTargetGoal(this));
        this.targetSelector.add(3, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
        this.targetSelector.add(3, new NearestAttackableTargetGoal<>(this, Raider.class, true));
        this.targetSelector.add(3, new NearestAttackableTargetGoal<>(this, Illusioner.class, true));
        if (GuardConfig.AttackAllMobs) {
            this.targetSelector.add(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, true, (mob) -> {
                return mob instanceof Enemy && !GuardConfig.MobBlackList.contains(mob.getEncodeId());
            }));
        }
        this.targetSelector.add(3,
        new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.add(4, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
        this.targetSelector.add(4, new ResetUniversalAngerTargetGoal<>(this, false));
    }
   
    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        super.equipStack(slot, stack);
        switch (slot) {
            case CHEST:
                if (this.guardInventory.getStack(1).isEmpty())
                    //this.guardInventory.setStack(1, this.getEquippedStack(slot.getEntitySlotId()));//TODO
                break;
            case FEET:
                if (this.guardInventory.getStack(3).isEmpty())
                    //this.guardInventory.setStack(3, this.getArmorItems().iterator().next());
                break;
            case HEAD:
                if (this.guardInventory.getStack(0).isEmpty())
                    //this.guardInventory.setStack(0, this.getArmorItems().get(slotIn.getIndex()));
                break;
            case LEGS:
                if (this.guardInventory.getStack(2).isEmpty())
                    //this.guardInventory.setStack(2, this.armorItems.get(slotIn.getIndex()));
                break;
            case MAINHAND:
                if (this.guardInventory.getStack(5).isEmpty())
                    //this.guardInventory.setStack(5, this.handItems.get(slotIn.getIndex()));
                break;
            case OFFHAND:
                if (this.guardInventory.getStack(4).isEmpty())
                    //this.guardInventory.setStack(4, this.handItems.get(slotIn.getIndex()));
                break;
        }
    }

    public static int getRandomTypeForBiome(ServerWorldAccess world, BlockPos pos) {
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

    }

    @Override
    public void postShoot() {

    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {

    }

    @Override
    public int getAngerTime() {
        return 0;
    }

    @Override
    public void setAngerTime(int ticks) {

    }

    @Nullable
    @Override
    public UUID getAngryAt() {
        return null;
    }

    @Override
    public void setAngryAt(@Nullable UUID uuid) {

    }

    @Override
    public void chooseRandomAngerTime() {

    }

    @Override
    public void onInventoryChanged(Inventory sender) {

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
}
