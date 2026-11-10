package com.dndcombat.core.creatures;

import com.dndcombat.actions.Action;
import com.dndcombat.core.context.EffectContext;
import com.dndcombat.effects.EffectHook;
import com.dndcombat.effects.OngoingEffect;
import com.dndcombat.enums.AbilityType;
import com.dndcombat.enums.CreatureType;
import com.dndcombat.enums.DamageType;
import com.dndcombat.enums.Team;
import com.dndcombat.equipment.Weapon;
import com.dndcombat.equipment.WeaponProperty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public abstract class Creature {

    // Identity
    private String name;
    private Team team;
    private CreatureType creatureType;

    // Core Stats
    private int currentHP;
    private int maxHP;
    private int armorClass;
    private int proficiencyBonus;
    private int movementSpeed; //max amount of speed that can be used on a turn, currently unused

    // Ability Scores (stored as primitives)
    private int strength;
    private int dexterity;
    private int constitution;
    private int intelligence;
    private int wisdom;
    private int charisma;

    // Saving Throws
    private SavingThrows savingThrows;

    // Skills
    private Skills skills;

    // Effects — separated for efficiency:
    // activeEffects: buffs/debuffs with durations (decremented and cleaned up each turn)
    // reactionTriggers: permanent triggers, only checked when reaction is available
    private final List<OngoingEffect> activeEffects;
    private final List<OngoingEffect> reactionTriggers;
    private OngoingEffect concentratingOn;

    // Available Actions
    private final List<Action> availableActions;
    private final List<Action> availableBonusActions;

    // Combat State
    private boolean actionUsed;
    private boolean bonusActionUsed;
    private boolean reactionUsed;
    private int movementRemaining;

    protected Creature(String name, Team team, CreatureType creatureType,
                       int maxHP, int armorClass, int proficiencyBonus,
                       int strength, int dexterity, int constitution,
                       int intelligence, int wisdom, int charisma) {
        this.name = name;
        this.team = team;
        this.creatureType = creatureType;
        this.maxHP = maxHP;
        this.currentHP = maxHP;
        this.armorClass = armorClass;
        this.proficiencyBonus = proficiencyBonus;

        this.strength = strength;
        this.dexterity = dexterity;
        this.constitution = constitution;
        this.intelligence = intelligence;
        this.wisdom = wisdom;
        this.charisma = charisma;

        this.savingThrows = new SavingThrows();
        this.skills = new Skills();

        this.activeEffects = new ArrayList<>();
        this.reactionTriggers = new ArrayList<>();
        this.concentratingOn = null;

        this.availableActions = new ArrayList<>();
        this.availableBonusActions = new ArrayList<>();
    }

    // --- Ability Score Access ---

    public int getAbilityScore(AbilityType type) {
        return switch (type) {
            case STRENGTH -> strength;
            case DEXTERITY -> dexterity;
            case CONSTITUTION -> constitution;
            case INTELLIGENCE -> intelligence;
            case WISDOM -> wisdom;
            case CHARISMA -> charisma;
        };
    }

    public void setAbilityScore(AbilityType type, int value) {
        switch (type) {
            case STRENGTH -> strength = value;
            case DEXTERITY -> dexterity = value;
            case CONSTITUTION -> constitution = value;
            case INTELLIGENCE -> intelligence = value;
            case WISDOM -> wisdom = value;
            case CHARISMA -> charisma = value;
        }
    }

    public int getAbilityModifier(AbilityType type) {
        return (getAbilityScore(type) - 10) / 2;
    }

    // --- Calculated Methods ---

    public int getInitiativeBonus() {
        return getAbilityModifier(AbilityType.DEXTERITY);
    }

    public int getSavingThrowBonus(AbilityType abilityType) {
        return savingThrows.getBonus(abilityType, getAbilityModifier(abilityType), proficiencyBonus);
    }

    public int getSkillBonus(Skills.Skill skill) {
        return skills.getBonus(skill, getAbilityModifier(skill.getAbility()), proficiencyBonus);
    }

    // --- Combat Bonus Calculations ---

    public int getAttackBonus(Weapon weapon) {
        int abilityMod;
        if (weapon.hasProperty(WeaponProperty.FINESSE)) {
            // Finesse: use the higher of STR or DEX
            abilityMod = Math.max(
                    getAbilityModifier(AbilityType.STRENGTH),
                    getAbilityModifier(AbilityType.DEXTERITY));
        } else if (weapon.isRanged()) {
            abilityMod = getAbilityModifier(AbilityType.DEXTERITY);
        } else {
            abilityMod = getAbilityModifier(AbilityType.STRENGTH);
        }
        return abilityMod + proficiencyBonus;
    }

    public int getDamageBonus(Weapon weapon) {
        if (weapon.hasProperty(WeaponProperty.FINESSE)) {
            return Math.max(
                    getAbilityModifier(AbilityType.STRENGTH),
                    getAbilityModifier(AbilityType.DEXTERITY));
        } else if (weapon.isRanged()) {
            return getAbilityModifier(AbilityType.DEXTERITY);
        } else {
            return getAbilityModifier(AbilityType.STRENGTH);
        }
    }

    // --- Damage & Healing ---

    public void takeDamage(int amount) {
        currentHP = Math.max(0, currentHP - amount);
    }

    public void takeDamage(int amount, DamageType type) {
        currentHP = Math.max(0, currentHP - amount);
    }

    public void heal(int amount) {
        currentHP = Math.min(maxHP, currentHP + amount);
    }

    public boolean isAlive() {
        return currentHP > 0;
    }

    // --- Action Economy ---

    public void resetActions() {
        actionUsed = false;
        bonusActionUsed = false;
        reactionUsed = false;
        movementRemaining = 30;
    }

    public boolean canAct() {
        return isAlive();
    }

    public boolean hasReactionAvailable() {
        return !reactionUsed;
    }

    // --- Action Management ---

    public List<Action> getAvailableActions() {
        return availableActions;
    }

    public List<Action> getAvailableBonusActions() {
        return availableBonusActions;
    }

    public void addAction(Action action) {
        availableActions.add(action);
    }

    public void addBonusAction(Action action) {
        availableBonusActions.add(action);
    }

    // --- Effect Management ---

    public void triggerEffects(EffectHook hook, EffectContext context) {
        List<OngoingEffect> toFire = new ArrayList<>();

        for (OngoingEffect effect : activeEffects) {
            if (effect.getRegisteredHooks().contains(hook)) {
                toFire.add(effect);
            }
        }

        if (hasReactionAvailable()) {
            for (OngoingEffect trigger : reactionTriggers) {
                if (trigger.getRegisteredHooks().contains(hook)) {
                    toFire.add(trigger);
                }
            }
        }

        toFire.sort(Comparator.comparingInt(OngoingEffect::getPriority));

        for (OngoingEffect effect : toFire) {
            effect.apply(hook, context);
        }
    }

    public void addEffect(OngoingEffect effect) {
        activeEffects.add(effect);
    }

    public void removeEffect(OngoingEffect effect) {
        activeEffects.remove(effect);
        if (concentratingOn == effect) {
            concentratingOn = null;
        }
    }

    public void addReactionTrigger(OngoingEffect trigger) {
        reactionTriggers.add(trigger);
    }

    public void removeReactionTrigger(OngoingEffect trigger) {
        reactionTriggers.remove(trigger);
    }

    public List<OngoingEffect> getActiveEffects() {
        return activeEffects;
    }

    public List<OngoingEffect> getReactionTriggers() {
        return reactionTriggers;
    }

    public void decrementEffectDurations() {
        for (OngoingEffect effect : activeEffects) {
            effect.decrementDuration();
        }
    }

    public void removeExpiredEffects(EffectContext context) {
        Iterator<OngoingEffect> it = activeEffects.iterator();
        while (it.hasNext()) {
            OngoingEffect effect = it.next();
            if (effect.shouldEnd(context)) {
                effect.onEffectEnd();
                if (concentratingOn == effect) {
                    concentratingOn = null;
                }
                it.remove();
            }
        }
    }

    // --- Concentration ---

    public void setConcentration(OngoingEffect effect) {
        if (concentratingOn != null) {
            breakConcentration();
        }
        concentratingOn = effect;
    }

    public void breakConcentration() {
        if (concentratingOn != null) {
            OngoingEffect broken = concentratingOn;
            concentratingOn = null;
            broken.onEffectEnd();
            activeEffects.remove(broken);
        }
    }

    public boolean isConcentratingOn(OngoingEffect effect) {
        return concentratingOn == effect;
    }

    public OngoingEffect getConcentratingOn() {
        return concentratingOn;
    }

    // --- Team Helpers ---

    public boolean isHostileTo(Creature other) {
        return team.isHostileTo(other.getTeam());
    }

    // --- Getters & Setters ---

    public String getName() {
        return name;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public CreatureType getCreatureType() {
        return creatureType;
    }

    public void setCreatureType(CreatureType creatureType) {
        this.creatureType = creatureType;
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public int getArmorClass() {
        return armorClass;
    }

    public void setArmorClass(int armorClass) {
        this.armorClass = armorClass;
    }

    public int getProficiencyBonus() {
        return proficiencyBonus;
    }

    public int getStrength() {
        return strength;
    }

    public int getDexterity() {
        return dexterity;
    }

    public int getConstitution() {
        return constitution;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public int getWisdom() {
        return wisdom;
    }

    public int getCharisma() {
        return charisma;
    }

    public SavingThrows getSavingThrows() {
        return savingThrows;
    }

    public Skills getSkills() {
        return skills;
    }

    public boolean isActionUsed() {
        return actionUsed;
    }

    public void setActionUsed(boolean actionUsed) {
        this.actionUsed = actionUsed;
    }

    public boolean isBonusActionUsed() {
        return bonusActionUsed;
    }

    public void setBonusActionUsed(boolean bonusActionUsed) {
        this.bonusActionUsed = bonusActionUsed;
    }

    public boolean isReactionUsed() {
        return reactionUsed;
    }

    public void setReactionUsed(boolean reactionUsed) {
        this.reactionUsed = reactionUsed;
    }

    public int getMovementRemaining() {
        return movementRemaining;
    }

    public void setMovementRemaining(int movementRemaining) {
        this.movementRemaining = movementRemaining;
    }

    @Override
    public String toString() {
        return String.format("%s (%d/%d HP)", name, currentHP, maxHP);
    }
}
