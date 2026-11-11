package com.dndcombat.actions;

public class TargetingRequirements {

    private final int minTargets;
    private final int maxTargets;
    private final boolean requiresHostile;
    private final int range; // in feet, 0 for self

    public TargetingRequirements(int minTargets, int maxTargets,
                                  boolean requiresHostile, int range) {
        this.minTargets = minTargets;
        this.maxTargets = maxTargets;
        this.requiresHostile = requiresHostile;
        this.range = range;
    }

    public static TargetingRequirements singleHostile(int range) {
        return new TargetingRequirements(1, 1, true, range);
    }

    public static TargetingRequirements self() {
        return new TargetingRequirements(0, 0, false, 0);
    }

    public int getMinTargets() {
        return minTargets;
    }

    public int getMaxTargets() {
        return maxTargets;
    }

    public boolean isRequiresHostile() {
        return requiresHostile;
    }

    public int getRange() {
        return range;
    }

    public boolean isSelfOnly() {
        return range == 0;
    }
}
