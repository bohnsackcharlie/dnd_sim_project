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
        // TODO this comment below is dumb, we probably should just have spellsave DC on the creature itself, we don't need to worry about multi-classing and different DCs I don't think
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

    @Override
    public String getDescription(Creature user) {
        //append attack bonus and save dc to description
        return """
                %s.
                Attack bonus: %+d.
                Spell Save DC: %d.
                """
                .formatted(
                        getDescription(),
                        getSpellAttackBonus(user),
                        getSpellSaveDC(user)
                ).strip();
    }
}
