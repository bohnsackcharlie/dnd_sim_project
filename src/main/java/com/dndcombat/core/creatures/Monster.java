package com.dndcombat.core.creatures;

import com.dndcombat.ai.BehaviorStrategy;
import com.dndcombat.enums.CreatureType;
import com.dndcombat.enums.Team;

public class Monster extends Creature {

    private double challengeRating;
    private BehaviorStrategy aiStrategy;

    public Monster(String name, int maxHP, int armorClass, int proficiencyBonus,
                   double challengeRating, CreatureType creatureType,
                   int strength, int dexterity, int constitution,
                   int intelligence, int wisdom, int charisma) {
        super(name, Team.ENEMIES, creatureType, maxHP, armorClass, proficiencyBonus,
              strength, dexterity, constitution, intelligence, wisdom, charisma);
        this.challengeRating = challengeRating;
    }

    public double getChallengeRating() {
        return challengeRating;
    }

    public BehaviorStrategy getAiStrategy() {
        return aiStrategy;
    }

    public void setAiStrategy(BehaviorStrategy aiStrategy) {
        this.aiStrategy = aiStrategy;
    }
}
