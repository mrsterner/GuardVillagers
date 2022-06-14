package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class WalkBackToCheckPointGoal extends Goal {
    private final GuardEntity guard;
    private final double speed;

    public WalkBackToCheckPointGoal(GuardEntity guard, double speedIn) {
        this.guard = guard;
        this.speed = speedIn;
    }

    @Override
    public boolean canStart() {
        return guard.getPatrolPos() != null && !this.guard.getPatrolPos().isWithinDistance(new Vec3i(this.guard.getPos().x, this.guard.getPos().y,this.guard.getPos().z), 1.0D) && !guard.isFollowing() && guard.isPatrolling();
    }

    @Override
    public boolean shouldContinue() {
        return this.canStart();
    }

    @Override
    public void tick() {
        BlockPos blockpos = this.guard.getPatrolPos();
        if (blockpos != null) {
            Vec3d vector3d = Vec3d.ofBottomCenter(blockpos);
            Vec3d vector3d1 = NoPenaltyTargeting.find(this.guard, 16, 3, vector3d, ((float) Math.PI / 10F));
            this.guard.getPatrolPos().isWithinDistance(new Vec3i(this.guard.getPos().x, this.guard.getPos().y, this.guard.getPos().z), 1.0D);
            if (guard.getMainHandStack().getItem() instanceof RangedWeaponItem) {
                this.guard.getNavigation().startMovingTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.speed);
            } else if (vector3d1 != null && guard.getTarget() == null) {
                this.guard.getNavigation().startMovingTo(vector3d1.x, vector3d1.y, vector3d1.z, this.speed);
            }
        }
    }
}