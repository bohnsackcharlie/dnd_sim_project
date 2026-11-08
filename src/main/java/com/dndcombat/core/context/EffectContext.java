package com.dndcombat.core.context;

import com.dndcombat.actions.Action;
import com.dndcombat.core.combat.CombatManager;
import com.dndcombat.core.creatures.Creature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EffectContext {

    private final CombatManager combatManager;
    private final Action action;
    private final Creature actionUser;
    private final List<Creature> targets;
    private final Map<String, Object> contextData;

    public EffectContext(CombatManager combatManager, Action action,
                         Creature actionUser, List<Creature> targets) {
        this.combatManager = combatManager;
        this.action = action;
        this.actionUser = actionUser;
        this.targets = targets;
        this.contextData = new HashMap<>();
    }

    public EffectContext(CombatManager combatManager, Creature actionUser) {
        this(combatManager, null, actionUser, new ArrayList<>());
    }

    // --- Getters ---

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public Action getAction() {
        return action;
    }

    public Creature getActionUser() {
        return actionUser;
    }

    public List<Creature> getTargets() {
        return targets;
    }

    public Creature getPrimaryTarget() {
        return targets.isEmpty() ? null : targets.get(0);
    }

    // --- Context data escape hatch ---

    public void setContextData(String key, Object value) {
        contextData.put(key, value);
    }

    public Object getContextData(String key) {
        return contextData.get(key);
    }
}
