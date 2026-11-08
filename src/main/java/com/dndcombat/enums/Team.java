package com.dndcombat.enums;

public enum Team {
    PARTY,
    ENEMIES,
    NEUTRAL;

    public boolean isHostileTo(Team other) {
        if (this == NEUTRAL || other == NEUTRAL) return false;
        return this != other;
    }
}
