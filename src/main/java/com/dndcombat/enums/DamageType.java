package com.dndcombat.enums;

public enum DamageType {
    SLASHING,
    PIERCING,
    BLUDGEONING,
    FIRE,
    COLD,
    LIGHTNING,
    THUNDER,
    ACID,
    POISON,
    NECROTIC,
    RADIANT,
    FORCE,
    PSYCHIC;

    public boolean isPhysical() {
        return this == SLASHING || this == PIERCING || this == BLUDGEONING;
    }

    public boolean isElemental() {
        return this == FIRE || this == COLD || this == LIGHTNING || this == THUNDER || this == ACID;
    }
}
