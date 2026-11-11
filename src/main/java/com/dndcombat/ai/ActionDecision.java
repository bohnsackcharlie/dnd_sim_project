package com.dndcombat.ai;

import java.util.List;

public record ActionDecision(
    String actionName,
    List<Integer> targetIds,
    String reasoning
) {}