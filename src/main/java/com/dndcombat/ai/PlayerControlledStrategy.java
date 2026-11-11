package com.dndcombat.ai;

import com.dndcombat.actions.Action;
import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.effects.OngoingEffect;
import com.dndcombat.ui.InputHandler;

import java.util.List;

// manual control of monsters
public class PlayerControlledStrategy implements BehaviorStrategy {

    private final InputHandler inputHandler;

    public PlayerControlledStrategy(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    @Override
    public Action chooseAction(Creature creature, List<Creature> allCombatants) {
        List<Action> actions = creature.getAvailableActions();
        if (actions.isEmpty()) return null;
        return inputHandler.chooseAction(actions, creature);
    }

    @Override
    public List<Creature> chooseTargets(Action action, List<Creature> allCombatants,
                                         Creature user) {
        return inputHandler.chooseTargets(action, allCombatants, user);
    }

    @Override
    public boolean shouldUseReaction(OngoingEffect trigger, EffectContext context) {
        return inputHandler.promptYesNo(
                String.format("Use %s? (Reaction)", trigger.getName()));
    }
}
