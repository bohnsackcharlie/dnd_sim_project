package com.dndcombat.core.creatures;

import com.dndcombat.enums.CharacterClass;
import com.dndcombat.enums.CreatureType;
import com.dndcombat.enums.ResourceType;
import com.dndcombat.enums.Team;

import java.util.EnumMap;
import java.util.Map;

public class PlayerCharacter extends Creature {

    private int level;
    private CharacterClass characterClass;
    private Map<ResourceType, Integer> resources;

    public PlayerCharacter(String name, int level, CharacterClass characterClass,
                           int maxHP, int armorClass,
                           int strength, int dexterity, int constitution,
                           int intelligence, int wisdom, int charisma) {
        super(name, Team.PARTY, CreatureType.HUMANOID, maxHP, armorClass,
              calculateProficiencyBonus(level),
              strength, dexterity, constitution, intelligence, wisdom, charisma);
        this.level = level;
        this.characterClass = characterClass;
        this.resources = new EnumMap<>(ResourceType.class);
    }

    private static int calculateProficiencyBonus(int level) {
        return (level - 1) / 4 + 2;
    }

    // --- Resource Management ---

    public int getResource(ResourceType type) {
        return resources.getOrDefault(type, 0);
    }

    public void setResource(ResourceType type, int amount) {
        resources.put(type, amount);
    }

    public boolean hasResource(ResourceType type, int amount) {
        return getResource(type) >= amount;
    }

    public void consumeResource(ResourceType type, int amount) {
        int current = getResource(type);
        if (current < amount) {
            throw new IllegalStateException(
                    String.format("Not enough %s: have %d, need %d", type, current, amount));
        }
        resources.put(type, current - amount);
    }

    // --- Getters ---

    public int getLevel() {
        return level;
    }

    public CharacterClass getCharacterClass() {
        return characterClass;
    }
}
