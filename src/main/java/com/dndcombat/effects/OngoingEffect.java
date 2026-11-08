package com.dndcombat.effects;

import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.creatures.Creature;

import java.util.Set;

public abstract class OngoingEffect {

    public static final int PRIORITY_EARLY = 25;
    public static final int PRIORITY_NORMAL = 100;
    public static final int PRIORITY_LATE = 175;
    public static final int PRIORITY_REACTION = 200;

    protected String name;
    protected Creature source;
    protected Creature target;
    protected int remainingDuration; // -1 for permanent (e.g., reaction triggers)
    protected boolean requiresConcentration;
    protected int priority = PRIORITY_NORMAL;

    // --- Abstract methods ---

    public abstract Set<EffectHook> getRegisteredHooks();

    public abstract void apply(EffectHook hook, EffectContext context);

    // --- Concrete methods ---

    public boolean shouldEnd(EffectContext context) {
        return remainingDuration == 0;
    }

    public void onEffectEnd() {
    }

    public void decrementDuration() {
        if (remainingDuration > 0) {
            remainingDuration--;
        }
    }

    // --- Getters / Setters ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Creature getSource() {
        return source;
    }

    public void setSource(Creature source) {
        this.source = source;
    }

    public Creature getTarget() {
        return target;
    }

    public void setTarget(Creature target) {
        this.target = target;
    }

    public int getRemainingDuration() {
        return remainingDuration;
    }

    public void setRemainingDuration(int remainingDuration) {
        this.remainingDuration = remainingDuration;
    }

    public boolean isRequiresConcentration() {
        return requiresConcentration;
    }

    public void setRequiresConcentration(boolean requiresConcentration) {
        this.requiresConcentration = requiresConcentration;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
