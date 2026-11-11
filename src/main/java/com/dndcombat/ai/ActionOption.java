package com.dndcombat.ai;

import java.util.List;

public record ActionOption(
    String name,
    String description,
    String actionType,
    List<Integer> legalTargetIds
) {}