package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

public class GuardRunToEatGoal extends WanderAroundGoal {
    private final GuardEntity guard;
    private int walkTimer;
    private boolean startedRunning;

    public GuardRunToEatGoal(GuardEntity guard) {
        super(guard, 1.0D);
        this.guard = guard;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.TARGET, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return this.guard.isRunningToEat() && this.getWanderTarget() != null;
    }

    @Override
    public void start() {
        super.start();
        if (this.walkTimer <= 0 && !startedRunning) {
            this.walkTimer = 20;
            startedRunning = true;
        }
    }

    @Override
    public void tick() {
        if (--walkTimer <= 0 && guard.isRunningToEat()) {
            this.guard.setRunningToEat(false);
            this.guard.setEating(true);
            startedRunning = false;
            this.guard.getNavigation().stop();
        }
        List<LivingEntity> list = this.guard.world.getNonSpectatingEntities(LivingEntity.class, this.guard.getBoundingBox().expand(5.0D, 3.0D, 5.0D));
        if (!list.isEmpty()) {
            for (LivingEntity mob : list) {
                if (mob != null) {
                    if (mob.getAttacking() instanceof GuardEntity || mob instanceof MobEntity && ((MobEntity) mob).getTarget() instanceof GuardEntity) {
                        if (walkTimer < 20)
                            this.walkTimer += 5;
                    }
                }
            }
        }
    }

    @Override
    protected Vec3d getWanderTarget() {
        List<LivingEntity> list = this.guard.world.getNonSpectatingEntities(LivingEntity.class, this.guard.getBoundingBox().expand(5.0D, 3.0D, 5.0D));
        if (!list.isEmpty()) {
            for (LivingEntity mob : list) {
                if (mob != null) {
                    if (mob.getAttacking() instanceof GuardEntity || mob instanceof MobEntity && ((MobEntity) mob).getTarget() instanceof GuardEntity) {
                        return NoPenaltyTargeting.find(guard, 16, 7, mob.getPos());
                    }
                }
            }
        }
        return super.getWanderTarget();
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && this.walkTimer > 0 && this.guard.isRunningToEat() && !guard.isEating() && startedRunning;
    }
}
