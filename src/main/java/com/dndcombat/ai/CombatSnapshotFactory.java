package com.dndcombat.ai;

import java.util.ArrayList;
import java.util.List;

import com.dndcombat.actions.Action;
import com.dndcombat.core.creatures.Creature;

public class CombatSnapshotFactory {

    // maybe at some point this should also take the combat manager and/or info like initiative order
    // TODO right now the self snapshot is the same as the others. We should have it include stuff like availbale action economy later
    public CombatSnapshot create(
        Creature self,
        List<Creature> combatants
    ) {
        List<Creature> allies = combatants.stream()
        .filter(c -> c != self)
        .filter(c -> c.getTeam() == self.getTeam())
        .toList();

        List<Creature> enemies = combatants.stream()
        .filter(c -> c != self)
        .filter(c -> c.getTeam() != self.getTeam())
        .toList();

        return new CombatSnapshot(
            createCreatureSnapshot(self),
            allies.stream()
                .map(this::createCreatureSnapshot)
                .toList(),
            enemies.stream()
                .map(this::createCreatureSnapshot)
                .toList(),
            getActionOptions(self, allies, enemies)
        );
    }

    private CreatureSnapshot createCreatureSnapshot(Creature creature) {
        return new CreatureSnapshot(
            creature.getId(),
            creature.getName(),
            creature.getCurrentHP(),
            creature.getMaxHP(),
            creature.getArmorClass()
        );
    }

    private List<ActionOption> getActionOptions(
        Creature self,
        List<Creature> allies,
        List<Creature> enemies
    ) {
        List<ActionOption> actionOptions = new ArrayList<>();

        if (!self.isActionUsed()) {
            actionOptions.addAll(
                createActionOptions(self, self.getAvailableActions(), allies, enemies)
            );
        }

        if (!self.isBonusActionUsed()) {
            actionOptions.addAll(
                createActionOptions(self, self.getAvailableBonusActions(), allies, enemies)
            );
        }

        return actionOptions;
    }

    private List<ActionOption> createActionOptions(
        Creature self,
        List<Action> actions,
        List<Creature> allies,
        List<Creature> enemies
    ) {
        List<ActionOption> options = new ArrayList<>();

        for (Action action : actions) {
            if (!action.canUse(self)) continue;

            List<Integer> legalTargets = new ArrayList<>();

            if (action.getTargetingReqs().isSelfOnly()) {
                if (action.isValidTarget(self, self)) {
                    legalTargets.add(self.getId());
                }
            } else {
                for (Creature target : enemies) {
                    if (action.isValidTarget(target, self)) {
                        legalTargets.add(target.getId());
                    }
                }

                for (Creature target : allies) {
                    if (action.isValidTarget(target, self)) {
                        legalTargets.add(target.getId());
                    }
                }
            }

            if (legalTargets.size() < action.getTargetingReqs().getMinTargets()) continue;

            options.add(createActionOption(self, action, legalTargets));
        }

        return options;
    }

    private ActionOption createActionOption(Creature self, Action action, List<Integer> legalTargetIds) {
        return new ActionOption(
            action.getName(), // right now name is used as a unique id, maybe needs to convert to true unique id later
            action.getDescription(self),
            action.getActionType().name(),
            legalTargetIds
        );
    }
}