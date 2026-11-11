package com.dndcombat.ai;

import com.dndcombat.actions.Action;
import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.effects.OngoingEffect;
import com.dndcombat.ui.InputHandler;

import java.util.ArrayList;
import java.util.List;

// manual control of monsters
public class PlayerControlledStrategy implements BehaviorStrategy {

    private final InputHandler inputHandler;

    public PlayerControlledStrategy(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    @Override
    public ResolvedActionDecision chooseAction(Creature creature, List<Creature> allCombatants) {
        List<Action> actions = new ArrayList<>();
        if (!creature.isActionUsed()) {
            actions.addAll(creature.getAvailableActions());
        }
        if (!creature.isBonusActionUsed()) {
            actions.addAll(creature.getAvailableBonusActions());
        }

        if (actions.isEmpty()) return null;
        Action action = inputHandler.chooseAction(actions, creature);
        List<Creature> targets = inputHandler.chooseTargets(action, allCombatants, creature);
        return new ResolvedActionDecision(
            action,
            targets);
    }

    @Override
    public boolean shouldUseReaction(OngoingEffect trigger, EffectContext context) {
        return inputHandler.promptYesNo(
                String.format("Use %s? (Reaction)", trigger.getName()));
    }
}
