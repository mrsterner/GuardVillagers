package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import dev.mrsterner.guardvillagers.common.ToolAction;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.ItemStack;
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

    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return GuardEntity.ToolActions.DEFAULT_SHIELD_ACTIONS.contains(toolAction);
    }

    @Override
    public boolean canStart() {
        List<? extends GuardEntity> list = this.taskOwner.world.getNonSpectatingEntities(this.taskOwner.getClass(),
        this.taskOwner.getBoundingBox().expand(8.0D, 8.0D, 8.0D));
        if (!list.isEmpty()) {
            for (GuardEntity guard : list) {
                if (!guard.isInvisible() && canPerformAction(guard.getOffHandStack(), GuardEntity.ToolActions.SHIELD_BLOCK) && guard.isBlocking()
                && this.taskOwner.world
                .getTargets(GuardEntity.class, NEARBY_GUARDS.setBaseMaxDistance(3.0D), guard,
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
        return NoPenaltyTargeting.find(this.taskOwner, 16, 7, this.guardtofollow.getPos(), (double)((float)Math.PI / 2F));
    }

    @Override
    public boolean shouldContinue() {
        return !this.taskOwner.getNavigation().isIdle() && !this.taskOwner.hasVehicle();
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