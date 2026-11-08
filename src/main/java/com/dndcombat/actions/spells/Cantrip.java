package com.dndcombat.actions.spells;

import com.dndcombat.actions.ActionType;

public abstract class Cantrip extends Spell {

    protected int scalingTier;

    protected Cantrip(String name, School school, ActionType castingTime, int scalingTier) {
        super(name, 0, school, castingTime);
        this.scalingTier = scalingTier;
    }

    public int getScalingTier() {
        return scalingTier;
    }
}
