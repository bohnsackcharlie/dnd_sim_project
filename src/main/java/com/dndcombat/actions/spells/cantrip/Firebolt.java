package com.dndcombat.actions.spells.cantrip;

import com.dndcombat.actions.ActionType;
import com.dndcombat.actions.TargetingRequirements;
import com.dndcombat.actions.spells.Cantrip;
import com.dndcombat.actions.spells.School;
import com.dndcombat.core.combat.ActionResult;
import com.dndcombat.core.combat.CombatLog;
import com.dndcombat.core.context.AttackContext;
import com.dndcombat.core.context.DamageContext;
import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.core.dice.DiceRoller;
import com.dndcombat.effects.EffectHook;
import com.dndcombat.enums.AttackType;
import com.dndcombat.enums.DamageType;

public class Firebolt extends Cantrip {

    public Firebolt(int scalingTier) {
        super("Firebolt", School.EVOCATION, ActionType.ACTION, scalingTier);
        this.targetingReqs = new TargetingRequirements(1, 1, true, 120);
        this.description = "You hurl a mote of fire at a creature or an object within range. Make a ranged spell attack against the target. On a hit, the target takes 1d10 Fire damage. A flammable object hit by this spell starts burning if it isn't being worn or carried.";
    }

    @Override
    public ActionResult execute(EffectContext context){
        Creature attacker = context.getActionUser();
        Creature target = context.getPrimaryTarget();

        AttackContext attackCtx = new AttackContext(context, AttackType.RANGED_SPELL, getSpellAttackBonus(attacker));
        rollAttackRoll(attacker, target, attackCtx);
        CombatLog log = context.getCombatManager().getLog();

        if (attackCtx.isHit()) {
            // === DAMAGE PHASE — create typed DamageContext ===
            int damageRoll = DiceRoller.roll(getScalingTier(), 10);

            // Critical hit — double the dice
            if (attackCtx.isCritical()) {
                int critDamage = DiceRoller.roll(getScalingTier(), 10);
                log.logCritDamageRoll("Firebolt", damageRoll, critDamage,
                        0, damageRoll + critDamage,
                        DamageType.FIRE);
                damageRoll += critDamage;
            } else {
                log.logDamageRoll("Firebolt", damageRoll, 0,
                        damageRoll, DamageType.FIRE);
            }

            DamageContext damageCtx = new DamageContext(
                    context, damageRoll, DamageType.FIRE);

            // BEFORE_DAMAGE hooks — both sides modify the same DamageContext
            attacker.triggerEffects(EffectHook.BEFORE_DAMAGE_DEALT, damageCtx);
            target.triggerEffects(EffectHook.BEFORE_DAMAGE_TAKEN, damageCtx);

            // Single path for final damage — all components resolved together
            int totalDamage = damageCtx.calculateFinalDamage();
            target.takeDamage(totalDamage);

            return ActionResult.hit(totalDamage, damageCtx, attackCtx.isCritical());
        }
        return ActionResult.miss();
    }
}
