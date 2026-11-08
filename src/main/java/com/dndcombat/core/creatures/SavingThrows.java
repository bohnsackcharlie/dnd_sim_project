package com.dndcombat.core.creatures;

import com.dndcombat.enums.AbilityType;

import java.util.EnumSet;
import java.util.Set;

public class SavingThrows {

    private final Set<AbilityType> proficiencies;

    public SavingThrows() {
        this.proficiencies = EnumSet.noneOf(AbilityType.class);
    }

    public SavingThrows(Set<AbilityType> proficiencies) {
        this.proficiencies = EnumSet.copyOf(proficiencies);
    }

    public boolean isProficient(AbilityType type) {
        return proficiencies.contains(type);
    }

    public void addProficiency(AbilityType type) {
        proficiencies.add(type);
    }

    public void removeProficiency(AbilityType type) {
        proficiencies.remove(type);
    }

    public int getBonus(AbilityType type, int abilityModifier, int proficiencyBonus) {
        int bonus = abilityModifier;
        if (isProficient(type)) {
            bonus += proficiencyBonus;
        }
        return bonus;
    }
}
