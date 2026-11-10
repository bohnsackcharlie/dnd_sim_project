package com.dndcombat.actions.spells.cantrip;

import java.util.List;

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

// TODO another poor implementation that needs to be reworked. 

public class EldritchBlast extends Cantrip {

    public EldritchBlast(int scalingTier) {
        super("Eldritch Blast", School.EVOCATION, ActionType.ACTION, scalingTier);

        // At lower levels: single target. At level 5+: can target multiple creatures
        int maxTargets = scalingTier >= 5 ? 4 : 1; // Max is 4 (level 17+)
        this.targetingReqs = new TargetingRequirements(1, maxTargets, true, 120);
    }

    @Override
    public ActionResult execute(EffectContext context) {
        Creature caster = context.getActionUser();
        List<Creature> targets = context.getTargets();
        CombatLog log = context.getCombatManager().getLog();

        int totalDamageDealt = 0;

        int numBeams = getScalingTier() >= 17 ? 4 :
                       getScalingTier() >= 11 ? 3 :
                       getScalingTier() >= 5 ? 2 : 1;

        // Execute attack and damage for each beam
        for (Creature target : targets) {
            AttackContext attackCtx = new AttackContext(context, AttackType.RANGED_SPELL, getSpellAttackBonus(caster));
            rollAttackRoll(caster, target, attackCtx);

            if (attackCtx.isHit()) {
                // === DAMAGE PHASE — create typed DamageContext for this beam ===
                int damageRoll = DiceRoller.roll(getScalingTier(), 10);

                // Critical hit — double the dice
                if (attackCtx.isCritical()) {
                    int critDamage = DiceRoller.roll(getScalingTier(), 10);
                    log.logCritDamageRoll("Eldritch Blast", damageRoll, critDamage,
                            0, damageRoll + critDamage,
                            DamageType.FORCE);
                    damageRoll += critDamage;
                } else {
                    log.logDamageRoll("Eldritch Blast", damageRoll, 0,
                            damageRoll, DamageType.FORCE);
                }

                DamageContext damageCtx = new DamageContext(
                        context, damageRoll, DamageType.FORCE);

                // BEFORE_DAMAGE hooks — both sides modify the same DamageContext
                caster.triggerEffects(EffectHook.BEFORE_DAMAGE_DEALT, damageCtx);
                target.triggerEffects(EffectHook.BEFORE_DAMAGE_TAKEN, damageCtx);

                // Single path for final damage — all components resolved together
                int totalDamage = damageCtx.calculateFinalDamage();
                target.takeDamage(totalDamage);
                totalDamageDealt += totalDamage;
            }
        }

        if (totalDamageDealt > 0) {
            return ActionResult.hit(totalDamageDealt, null, false);
        }
        return ActionResult.miss();
    }
}
