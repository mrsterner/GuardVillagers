package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FollowShieldGuards extends Goal {
    private static final TargetPredicate NEARBY_GUARDS = TargetPredicate.createNonAttackable().setBaseMaxDistance(8.0D).ignoreVisibility();
    private final GuardEntity taskOwner;
    private GuardEntity guardtofollow;
    private double x;
    private double y;
    private double z;

    public FollowShieldGuards(GuardEntity taskOwnerIn) {
        this.taskOwner = taskOwnerIn;
    }

    @Override
    public boolean canStart() {
        List<? extends GuardEntity> list = this.taskOwner.getWorld().getNonSpectatingEntities(this.taskOwner.getClass(), this.taskOwner.getBoundingBox().expand(8.0D, 8.0D, 8.0D));
        if (!list.isEmpty()) {
            for (GuardEntity guard : list) {
                if (!guard.isInvisible() && guard.getOffHandStack().getItem() == Items.SHIELD && guard.isBlocking() // Might create compatibility problems
                        && this.taskOwner.getWorld().getTargets(GuardEntity.class, NEARBY_GUARDS.setBaseMaxDistance(3.0D), guard,
                                this.taskOwner.getBoundingBox().expand(5.0D))
                        .size() < 5) {
                    this.guardtofollow = guard;
                    Vec3d vec3d = this.getPosition();
                    if (vec3d == null) {
                        return false;
                    } else {
                        this.x = vec3d.x;
                        this.y = vec3d.y;
                        this.z = vec3d.z;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Nullable
    protected Vec3d getPosition() {
        return NoPenaltyTargeting.findTo(this.taskOwner, 16, 7, this.guardtofollow.getPos(), (float) Math.PI / 2F);
    }

    @Override
    public boolean shouldContinue() {
        return !this.taskOwner.getNavigation().isIdle() && !this.taskOwner.hasPassengers();
    }

    @Override
    public void stop() {
        this.taskOwner.getNavigation().stop();
        super.stop();
    }

    @Override
    public void start() {
        this.taskOwner.getNavigation().startMovingTo(x, y, z, 0.4D);
    }
}