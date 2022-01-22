package dev.mrsterner.guardvillagers.mixin;

import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerPlayerEntity.class)
public interface ServerPlayerEntityAccessor {

	@Invoker("incrementScreenHandlerSyncId")
	public void incrementScreenHandlerSyncId();

	@Invoker("onScreenHandlerOpened")
	public void onScreenHandlerOpened(ScreenHandler screenHandler);

	@Accessor("screenHandlerSyncId")
	int screenHandlerSyncId();

}
