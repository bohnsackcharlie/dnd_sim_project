package com.dndcombat.ai;

public record CreatureSnapshot(
    int id,
    String name,
    int currentHp,
    int maxHp,
    int armorClass
) {}