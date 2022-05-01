package dev.mrsterner.guardvillagers.common;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.mixin.LootContextTypesAccessor;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextType;
import net.minecraft.util.Identifier;

public class GuardLootTables {

    public static final LootContextType SLOT = LootContextTypesAccessor.guardvillager$register("slot", (builder) -> {
        builder.allow(LootContextParameters.THIS_ENTITY);
    });

    public static final Identifier GUARD_MAIN_HAND = new Identifier(GuardVillagers.MODID, "entities/guard_main_hand");
    public static final Identifier GUARD_OFF_HAND = new Identifier(GuardVillagers.MODID, "entities/guard_off_hand");
    public static final Identifier GUARD_HELMET = new Identifier(GuardVillagers.MODID, "entities/guard_helmet");
    public static final Identifier GUARD_CHEST = new Identifier(GuardVillagers.MODID, "entities/guard_chestplate");
    public static final Identifier GUARD_LEGGINGS = new Identifier(GuardVillagers.MODID, "entities/guard_legs");
    public static final Identifier GUARD_FEET = new Identifier(GuardVillagers.MODID, "entities/guard_feet");

}
