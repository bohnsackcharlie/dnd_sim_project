package com.dndcombat.ai;

import java.util.List;

import com.dndcombat.actions.Action;
import com.dndcombat.core.creatures.Creature;

public record ResolvedActionDecision(
    Action action,
    List<Creature> targets
) {}