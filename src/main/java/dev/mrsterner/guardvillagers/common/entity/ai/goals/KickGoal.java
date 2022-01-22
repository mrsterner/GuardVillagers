package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.ai.goal.Goal;

public class KickGoal extends Goal {

    public final GuardEntity guard;

    public KickGoal(GuardEntity guard) {
        this.guard = guard;
    }

    @Override
    public boolean canStart() {
        return guard.getTarget() != null && guard.getTarget().distanceTo(guard) <= 2.5D && guard.getMainHandStack().getItem().isUsedOnRelease(guard.getMainHandStack()) && !guard.isBlocking() && guard.kickCoolDown == 0;
    }

    @Override
    public void start() {
        guard.setKicking(true);
        if (guard.kickTicks <= 0) {
            guard.kickTicks = 10;
        }
        guard.tryAttack(guard.getTarget());
    }

    @Override
    public void stop() {
        guard.setKicking(false);
        guard.kickCoolDown = 50;
    }
}