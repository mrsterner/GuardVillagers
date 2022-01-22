package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

public class GuardSetRunningToEatGoal extends Goal {
    protected final GuardEntity guard;

    public GuardSetRunningToEatGoal(GuardEntity guard, double speedIn) {
        super();
        this.guard = guard;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return !guard.isRunningToEat() && guard.getHealth() < guard.getMaxHealth() / 2 && GuardEatFoodGoal.isConsumable(guard.getOffHandStack()) && !guard.isEating() && guard.getTarget() != null;
    }

    @Override
    public void start() {
        this.guard.setTarget(null);
        if (!guard.isRunningToEat())
            this.guard.setRunningToEat(true);

    }
}