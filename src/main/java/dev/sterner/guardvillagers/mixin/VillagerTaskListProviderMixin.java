package dev.sterner.guardvillagers.mixin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.task.RepairGolemTask;
import dev.sterner.guardvillagers.common.entity.task.ShareGossipWithGuard;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(VillagerTaskListProvider.class)
public class VillagerTaskListProviderMixin {
    @Inject(method = "createMeetTasks", cancellable = true, at = @At("RETURN"))
    private static void createMeetTasks(VillagerProfession pProfession, float pSpeedModifier, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>>> cir) {
        List<Pair<Integer, ? extends Task<? super VillagerEntity>>> villagerList = new ArrayList<>(cir.getReturnValue());
        villagerList.add(Pair.of(2, new CompositeTask<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), CompositeTask.Order.ORDERED, CompositeTask.RunMode.RUN_ONE, ImmutableList.of(Pair.of(new ShareGossipWithGuard(), 1), Pair.of(new GatherItemsVillagerTask(), 1)))));
        cir.setReturnValue(ImmutableList.copyOf(villagerList));
    }

    @Inject(method = "createIdleTasks", cancellable = true, at = @At("RETURN"))
    private static void createIdleTasks(VillagerProfession pProfession, float pSpeedModifier, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>>> cir) {
        List<Pair<Integer, ? extends Task<? super VillagerEntity>>> villagerList = new ArrayList<>(cir.getReturnValue());
        villagerList.add(Pair.of(2, new RandomTask<>(ImmutableList.of(Pair.of(FindEntityTask.create(GuardVillagers.GUARD_VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, pSpeedModifier, 2), 3), Pair.of(FindEntityTask.create(EntityType.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, pSpeedModifier, 2), 3), Pair.of(new WaitTask(30, 60), 1)))));
        villagerList.add(Pair.of(2, new CompositeTask<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), CompositeTask.Order.ORDERED, CompositeTask.RunMode.RUN_ONE, ImmutableList.of(Pair.of(new ShareGossipWithGuard(), 1), Pair.of(new GatherItemsVillagerTask(), 1)))));
        cir.setReturnValue(ImmutableList.copyOf(villagerList));
    }

    @Inject(method = "createWorkTasks", cancellable = true, at = @At("RETURN"))
    private static void createWorkTasks(VillagerProfession profession, float speed, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>>> cir) {
        if (profession == VillagerProfession.TOOLSMITH || profession == VillagerProfession.WEAPONSMITH && GuardVillagersConfig.blackSmithHealing) {
            List<Pair<Integer, ? extends Task<? super VillagerEntity>>> villagerList = new ArrayList<>(cir.getReturnValue());
            villagerList.add(Pair.of(2, new CompositeTask<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), CompositeTask.Order.ORDERED, CompositeTask.RunMode.RUN_ONE, ImmutableList.of(Pair.of(new RepairGolemTask(), 1), Pair.of(new GatherItemsVillagerTask(), 1)))));
            cir.setReturnValue(ImmutableList.copyOf(villagerList));
        }
    }
}
