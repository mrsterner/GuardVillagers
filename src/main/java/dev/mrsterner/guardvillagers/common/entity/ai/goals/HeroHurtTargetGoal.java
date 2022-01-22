package dev.mrsterner.guardvillagers.common.entity.ai.goals;

import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.passive.MerchantEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class HeroHurtTargetGoal extends TrackTargetGoal {
    private final GuardEntity guard;
    private LivingEntity attacker;
    private int timestamp;

    public HeroHurtTargetGoal(GuardEntity theEntityTameableIn) {
        super(theEntityTameableIn, false);
        this.guard = theEntityTameableIn;
        this.setControls(EnumSet.of(Goal.Control.TARGET));
    }

    @Override
    public boolean canStart() {
        LivingEntity livingentity = this.guard.getOwner();
        if (livingentity == null) {
            return false;
        } else {
            this.attacker = livingentity.getAttacker();
            int i = livingentity.getLastAttackedTime();
            return i != this.timestamp && this.canTrack(this.attacker, TargetPredicate.DEFAULT);
        }
    }

    @Override
    protected boolean canTrack(@Nullable LivingEntity potentialTarget, TargetPredicate targetPredicate) {
        return super.canTrack(potentialTarget, targetPredicate) && !(potentialTarget instanceof MerchantEntity) && !(potentialTarget instanceof GuardEntity);
    }

    @Override
    public void start() {
        this.mob.setTarget(this.attacker);
        LivingEntity livingentity = this.guard.getOwner();
        if (livingentity != null) {
            this.timestamp = livingentity.getLastAttackedTime();
        }
        super.start();
    }
}