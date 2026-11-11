package com.dndcombat.ai;

import java.util.List;

public record CombatSnapshot(
    CreatureSnapshot self,
    List<CreatureSnapshot> allies,
    List<CreatureSnapshot> enemies,
    List<ActionOption> availableActions
) {}