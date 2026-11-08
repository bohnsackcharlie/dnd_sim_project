package com.dndcombat.core.context;

import com.dndcombat.enums.AbilityType;

public class SaveContext extends EffectContext {

    private final AbilityType saveAbility;
    private final int saveDC;
    private int saveRoll;
    private int saveTotal;
    private boolean succeeded;
    private boolean forceReroll;

    private int rollModifier;

    public SaveContext(EffectContext parent, AbilityType ability, int dc) {
        super(parent.getCombatManager(), parent.getAction(),
              parent.getActionUser(), parent.getTargets());
        this.saveAbility = ability;
        this.saveDC = dc;
    }

    // --- Modifiers ---

    public void addModifier(int mod) {
        rollModifier += mod;
    }

    public int applyModifiers(int baseRoll) {
        return baseRoll + rollModifier;
    }

    // --- Getters / Setters ---

    public AbilityType getSaveAbility() {
        return saveAbility;
    }

    public int getSaveDC() {
        return saveDC;
    }

    public int getSaveRoll() {
        return saveRoll;
    }

    public void setSaveRoll(int roll) {
        this.saveRoll = roll;
    }

    public int getSaveTotal() {
        return saveTotal;
    }

    public void setSaveTotal(int total) {
        this.saveTotal = total;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public void setSucceeded(boolean success) {
        this.succeeded = success;
    }

    public boolean isForceReroll() {
        return forceReroll;
    }

    public void setForceReroll(boolean force) {
        this.forceReroll = force;
    }
}
