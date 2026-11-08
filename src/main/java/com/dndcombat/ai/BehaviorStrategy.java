package com.dndcombat.ai;

import com.dndcombat.actions.Action;
import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.effects.OngoingEffect;

import java.util.List;

public interface BehaviorStrategy {

    Action chooseAction(Creature creature, List<Creature> allCombatants);

    List<Creature> chooseTargets(Action action, List<Creature> allCombatants, Creature user);

    boolean shouldUseReaction(OngoingEffect trigger, EffectContext context);
}
