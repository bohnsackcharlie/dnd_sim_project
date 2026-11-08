package com.dndcombat.enums;

public enum AttackType {
    MELEE_WEAPON,
    RANGED_WEAPON,
    MELEE_SPELL,
    RANGED_SPELL;

    public boolean isMelee() {
        return this == MELEE_WEAPON || this == MELEE_SPELL;
    }

    public boolean isRanged() {
        return this == RANGED_WEAPON || this == RANGED_SPELL;
    }

    public boolean isWeaponAttack() {
        return this == MELEE_WEAPON || this == RANGED_WEAPON;
    }

    public boolean isSpellAttack() {
        return this == MELEE_SPELL || this == RANGED_SPELL;
    }
}
