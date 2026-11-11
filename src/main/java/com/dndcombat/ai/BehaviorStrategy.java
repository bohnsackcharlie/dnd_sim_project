package com.dndcombat.ai;

import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.effects.OngoingEffect;

import java.util.List;

public interface BehaviorStrategy {

    ResolvedActionDecision chooseAction(Creature creature, List<Creature> allCombatants);

    boolean shouldUseReaction(OngoingEffect trigger, EffectContext context);
}
