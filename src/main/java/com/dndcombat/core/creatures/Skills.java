package com.dndcombat.core.creatures;

import com.dndcombat.enums.AbilityType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class Skills {

    public enum Skill {
        ACROBATICS(AbilityType.DEXTERITY),
        ANIMAL_HANDLING(AbilityType.WISDOM),
        ARCANA(AbilityType.INTELLIGENCE),
        ATHLETICS(AbilityType.STRENGTH),
        DECEPTION(AbilityType.CHARISMA),
        HISTORY(AbilityType.INTELLIGENCE),
        INSIGHT(AbilityType.WISDOM),
        INTIMIDATION(AbilityType.CHARISMA),
        INVESTIGATION(AbilityType.INTELLIGENCE),
        MEDICINE(AbilityType.WISDOM),
        NATURE(AbilityType.INTELLIGENCE),
        PERCEPTION(AbilityType.WISDOM),
        PERFORMANCE(AbilityType.CHARISMA),
        PERSUASION(AbilityType.CHARISMA),
        RELIGION(AbilityType.INTELLIGENCE),
        SLEIGHT_OF_HAND(AbilityType.DEXTERITY),
        STEALTH(AbilityType.DEXTERITY),
        SURVIVAL(AbilityType.WISDOM);

        private final AbilityType ability;

        Skill(AbilityType ability) {
            this.ability = ability;
        }

        public AbilityType getAbility() {
            return ability;
        }
    }

    private final Set<Skill> proficiencies;
    private final Set<Skill> expertises;

    public Skills() {
        this.proficiencies = EnumSet.noneOf(Skill.class);
        this.expertises = EnumSet.noneOf(Skill.class);
    }

    public boolean isProficient(Skill skill) {
        return proficiencies.contains(skill);
    }

    public boolean hasExpertise(Skill skill) {
        return expertises.contains(skill);
    }

    public void addProficiency(Skill skill) {
        proficiencies.add(skill);
    }

    public void addExpertise(Skill skill) {
        proficiencies.add(skill);
        expertises.add(skill);
    }

    public int getBonus(Skill skill, int abilityModifier, int proficiencyBonus) {
        int bonus = abilityModifier;
        if (hasExpertise(skill)) {
            bonus += proficiencyBonus * 2;
        } else if (isProficient(skill)) {
            bonus += proficiencyBonus;
        }
        return bonus;
    }
}
