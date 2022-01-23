package dev.mrsterner.guardvillagers.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEntity.class)
public interface MobEntityAccessor {
    @Accessor("targetSelector")
    GoalSelector targetSelector();

    @Accessor("goalSelector")
    GoalSelector goalSelector();

    @Accessor("armorItems")
    DefaultedList<ItemStack> armorItems();

    @Accessor("handItems")
    DefaultedList<ItemStack> handItems();
}
