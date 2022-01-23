package dev.mrsterner.guardvillagers.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.transformer.meta.MixinInner;

@Mixin(MobEntity.class)
public interface MobEntityAccessor {
    @Accessor("targetSelector")
    GoalSelector targetSelector();

    @Accessor("goalSelector")
    GoalSelector goalSelector();
}
