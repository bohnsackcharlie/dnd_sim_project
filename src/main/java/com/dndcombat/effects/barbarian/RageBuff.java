package com.dndcombat.effects.barbarian;

import com.dndcombat.core.context.EffectContext;

import java.util.Set;

import com.dndcombat.core.context.DamageContext;
import com.dndcombat.actions.attacks.WeaponAttack;
import com.dndcombat.effects.EffectHook;
import com.dndcombat.effects.OngoingEffect;

public class RageBuff extends OngoingEffect {
    private boolean attackedThisTurn = false;

    public RageBuff(int duration) {
        this.remainingDuration = duration;
        this.priority = PRIORITY_NORMAL;
    }

    @Override
    public Set<EffectHook> getRegisteredHooks() {
        return Set.of(
            EffectHook.BEFORE_DAMAGE_DEALT,
            EffectHook.BEFORE_DAMAGE_TAKEN,
            EffectHook.WHEN_CASTING_SPELL,
            EffectHook.START_OF_TURN,
            EffectHook.END_OF_TURN
        );
    }

    @Override
    public void apply(EffectHook hook, EffectContext context) {
        switch (hook) {
            case BEFORE_DAMAGE_DEALT:
                // +2 to melee weapon damage
                DamageContext dmgDealtCtx = (DamageContext) context;
                if (context.getAction() instanceof WeaponAttack) {
                    WeaponAttack attack = (WeaponAttack) context.getAction();
                    if (attack.isMelee()) {
                        dmgDealtCtx.addModifier(+2);
                        attackedThisTurn = true;
                    }
                }
                break;

            case BEFORE_DAMAGE_TAKEN:
                // Resistance to bludgeoning, piercing, and slashing damage.
                // Per-type resistance means only physical components are halved —
                // fire damage from a Flame Tongue still hits at full.
                DamageContext dmgTakenCtx = (DamageContext) context;
                dmgTakenCtx.addPhysicalResistance();
                break;

            case WHEN_CASTING_SPELL:
                // Can't cast spells while raging — use contextData escape hatch
                // since this is an unusual action-cancellation case
                // TODO I don't think this actually works yet
                context.setContextData("cancelAction", true);
                context.setContextData("cancelReason", "Cannot cast spells while raging");
                break;

            case START_OF_TURN:
                attackedThisTurn = false;
                break;

            // TODO add removal of the effect when it should end
        }
    }
}