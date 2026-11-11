package com.dndcombat.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.dndcombat.actions.Action;
import com.dndcombat.core.creatures.Creature;

public class DecisionValidator {

    public ResolvedActionDecision validate(
            ActionDecision decision,
            CombatSnapshot snapshot,
            Creature self,
            List<Creature> combatants) {

        ActionOption actionOption = snapshot.availableActions().stream()
                .filter(a -> a.name().equals(decision.actionName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Action not available: " + decision.actionName()));

        
        
        List<Action> actions = new ArrayList<>();
        actions.addAll(self.getAvailableActions());
        actions.addAll(self.getAvailableBonusActions());        
        Action action = actions.stream()
                .filter(a -> a.getName().equals(decision.actionName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Action not found on creature: " + decision.actionName()));

        if (!action.canUse(self)) {
            throw new IllegalArgumentException("Action can no longer be used");
        }

        Map<Integer, Creature> creaturesById = combatants.stream()
                .collect(Collectors.toMap(
                        Creature::getId,
                        Function.identity()
                ));

        List<Creature> targets = decision.targetIds().stream()
                .map(id -> {
                    if (!actionOption.legalTargetIds().contains(id)) {
                        throw new IllegalArgumentException(
                                "Target ID " + id + " is not a legal target");
                    }

                    Creature target = creaturesById.get(id);

                    if (target == null) {
                        throw new IllegalArgumentException(
                                "Creature not found: " + id);
                    }

                    if (!action.isValidTarget(target, self)) {
                        throw new IllegalArgumentException(
                                "Target is no longer valid: " + target.getName());
                    }

                    return target;
                })
                .toList();

        if (targets.isEmpty()) {
            throw new IllegalArgumentException("Decision has no targets");
        }

        return new ResolvedActionDecision(action, targets);
    }
}