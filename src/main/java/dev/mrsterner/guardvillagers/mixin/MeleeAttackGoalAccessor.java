package dev.mrsterner.guardvillagers.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.loot.context.LootContextType;
import net.minecraft.loot.context.LootContextTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;

@Mixin(MeleeAttackGoal.class)
public interface MeleeAttackGoalAccessor {

	@Accessor("path")
	Path path();
}
