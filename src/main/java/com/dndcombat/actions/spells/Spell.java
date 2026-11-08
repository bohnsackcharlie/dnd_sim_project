package com.dndcombat.actions.spells;

import com.dndcombat.actions.Action;
import com.dndcombat.actions.ActionType;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.enums.AbilityType;

public abstract class Spell extends Action {

    protected int level;
    protected School school;
    protected boolean requiresConcentration;
    protected int duration; // in turns
    protected ActionType castingTime;

    protected Spell(String name, int level, School school, ActionType castingTime) {
        super(name, castingTime);
        this.level = level;
        this.school = school;
    }

    protected int getSpellSaveDC(Creature caster) {
        return 8 + caster.getProficiencyBonus()
                + caster.getAbilityModifier(getSpellcastingAbility(caster));
    }

    protected int getSpellAttackBonus(Creature caster) {
        return caster.getProficiencyBonus()
                + caster.getAbilityModifier(getSpellcastingAbility(caster));
    }

    protected AbilityType getSpellcastingAbility(Creature caster) {
        // Default to intelligence; subclasses can override for WIS/CHA casters
        return AbilityType.INTELLIGENCE;
    }

    // --- Getters ---

    public int getLevel() {
        return level;
    }

    public School getSchool() {
        return school;
    }

    public boolean isRequiresConcentration() {
        return requiresConcentration;
    }

    public int getDuration() {
        return duration;
    }

    public ActionType getCastingTime() {
        return castingTime;
    }
}
