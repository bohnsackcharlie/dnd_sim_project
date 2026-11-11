package com.dndcombat.ai;

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

        // Find the requested action in the snapshot.
        ActionOption actionOption = snapshot.availableActions().stream()
                .filter(a -> a.name().equals(decision.actionName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Action not available: " + decision.actionName()));

        // Find the actual Action object.
        Action action = self.getAvailableActions().stream()
                .filter(a -> a.getName().equals(decision.actionName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Action not found on creature: " + decision.actionName()));

        // Check that the action is still usable.
        if (!action.canUse(self)) {
            throw new IllegalArgumentException("Action can no longer be used");
        }

        // Build a lookup of actual creatures by ID.
        Map<Integer, Creature> creaturesById = combatants.stream()
                .collect(Collectors.toMap(
                        Creature::getId,
                        Function.identity()
                ));

        // Resolve and validate targets.
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