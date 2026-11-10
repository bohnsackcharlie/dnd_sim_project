package com.dndcombat.effects.generic_effects;

import com.dndcombat.core.context.EffectContext;

import java.util.Set;

import com.dndcombat.core.context.AttackContext;
import com.dndcombat.effects.EffectHook;
import com.dndcombat.effects.OngoingEffect;

public class BlurBuff extends OngoingEffect {

    @Override
    public Set<EffectHook> getRegisteredHooks() {
        return Set.of(
            EffectHook.WHEN_BEING_ATTACKED
        );
    }

    public BlurBuff(int duration, boolean requiresConcentration) {
        this.name = "Blur";
        this.remainingDuration = duration;
        this.requiresConcentration = requiresConcentration;
        this.priority = 100; // Normal priority - apply after early effects
    }

    @Override
    public void apply(EffectHook hook, EffectContext context) {
        if (hook == EffectHook.WHEN_BEING_ATTACKED) {
            AttackContext attackCtx = (AttackContext) context;
            attackCtx.grantDisadvantage();
        }
    }

    @Override
    public boolean shouldEnd(EffectContext context) {
        return remainingDuration <= 0;
    }
}
