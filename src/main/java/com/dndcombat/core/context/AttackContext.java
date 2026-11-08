package com.dndcombat.core.context;

import com.dndcombat.core.dice.DiceRoller;
import com.dndcombat.enums.AttackType;

public class AttackContext extends EffectContext {

    private final AttackType attackType;
    private final int attackBonus;
    private int attackRoll;
    private int attackTotal;
    private boolean hit;
    private boolean critical;
    private boolean attackNegated;

    private boolean hasAdvantage;
    private boolean hasDisadvantage;

    private int rollModifier;

    public AttackContext(EffectContext parent, AttackType attackType, int attackBonus) {
        super(parent.getCombatManager(), parent.getAction(),
              parent.getActionUser(), parent.getTargets());
        this.attackType = attackType;
        this.attackBonus = attackBonus;
    }

    // --- Advantage / Disadvantage ---

    public void grantAdvantage() {
        hasAdvantage = true;
    }

    public void grantDisadvantage() {
        hasDisadvantage = true;
    }

    public boolean shouldRollWithAdvantage() {
        if (hasAdvantage && hasDisadvantage) return false;
        return hasAdvantage;
    }

    public boolean shouldRollWithDisadvantage() {
        if (hasAdvantage && hasDisadvantage) return false;
        return hasDisadvantage;
    }

    public int rollAttack() {
        if (shouldRollWithAdvantage()) {
            return Math.max(DiceRoller.d20(), DiceRoller.d20());
        } else if (shouldRollWithDisadvantage()) {
            return Math.min(DiceRoller.d20(), DiceRoller.d20());
        }
        return DiceRoller.d20();
    }

    // --- Roll Modifiers ---

    public void addRollModifier(int mod) {
        rollModifier += mod;
    }

    public int applyRollModifiers(int baseRoll) {
        return baseRoll + rollModifier;
    }

    // --- Getters / Setters ---

    public AttackType getAttackType() {
        return attackType;
    }

    public int getAttackBonus() {
        return attackBonus;
    }

    public boolean isMelee() {
        return attackType.isMelee();
    }

    public boolean isWeaponAttack() {
        return attackType.isWeaponAttack();
    }

    public boolean isSpellAttack() {
        return attackType.isSpellAttack();
    }

    public int getAttackRoll() {
        return attackRoll;
    }

    public void setAttackRoll(int roll) {
        this.attackRoll = roll;
    }

    public int getAttackTotal() {
        return attackTotal;
    }

    public void setAttackTotal(int total) {
        this.attackTotal = total;
    }

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean crit) {
        this.critical = crit;
    }

    public boolean isHit() {
        return hit && !attackNegated;
    }

    public void setHit(boolean hit) {
        this.hit = hit;
    }

    public boolean isAttackNegated() {
        return attackNegated;
    }

    public void setAttackNegated(boolean negated) {
        this.attackNegated = negated;
    }
}
