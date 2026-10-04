package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class FollowShieldGuards extends Goal {
    private final GuardEntity taskOwner;
    private GuardEntity guardtofollow;
    private double x;
    private double y;
    private double z;

    public FollowShieldGuards(GuardEntity taskOwnerIn) {
        this.taskOwner = taskOwnerIn;
    }

    @Override
    public boolean canUse() {
        List<? extends GuardEntity> list = this.taskOwner.level().getEntitiesOfClass(this.taskOwner.getClass(), this.taskOwner.getBoundingBox().inflate(8.0D, 8.0D, 8.0D));
        if (!list.isEmpty()) {
            for (GuardEntity guard : list) {
                if (!guard.isInvisible()
                        && guard.getOffhandItem().getItem() == Items.SHIELD
                        && guard.isBlocking()
                        && taskOwner.level().getEntitiesOfClass(
                        GuardEntity.class,
                        taskOwner.getBoundingBox().inflate(5.0),
                        e -> e != guard && !e.isInvisible()
                ).size() < 5) {
                    this.guardtofollow = guard;
                    Vec3 vec3d = this.getPosition();
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
    protected Vec3 getPosition() {
        return DefaultRandomPos.getPosTowards(this.taskOwner, 16, 7, this.guardtofollow.position(), (float) Math.PI / 2F);
    }

    @Override
    public boolean canContinueToUse() {
        return !this.taskOwner.getNavigation().isDone() && !this.taskOwner.isVehicle();
    }

    @Override
    public void stop() {
        this.taskOwner.getNavigation().stop();
        super.stop();
    }

    @Override
    public void start() {
        this.taskOwner.getNavigation().moveTo(x, y, z, 0.4D);
    }
}