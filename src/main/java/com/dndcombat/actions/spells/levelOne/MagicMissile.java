package com.dndcombat.actions.spells.levelOne;

import com.dndcombat.actions.ActionType;
import com.dndcombat.actions.TargetingRequirements;
import com.dndcombat.actions.spells.School;
import com.dndcombat.actions.spells.Spell;
import com.dndcombat.core.combat.ActionResult;
import com.dndcombat.core.combat.CombatLog;
import com.dndcombat.core.context.DamageContext;
import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.effects.EffectHook;
import com.dndcombat.enums.AbilityType;
import com.dndcombat.enums.DamageType;

import java.util.List;

public class MagicMissile extends Spell {

    public MagicMissile() {
        super("Magic Missile", 1, School.ABJURATION, ActionType.ACTION);
        this.targetingReqs = new TargetingRequirements(3, 3, true, 120);
    }

    @Override
    public ActionResult execute(EffectContext context) {
        Creature caster = context.getActionUser();
        List<Creature> targets = context.getTargets();
        CombatLog log = context.getCombatManager().getLog();

        int saveDC = getSpellSaveDC(caster);
        log.logMessage(String.format("%s casts %s!", caster.getName(), name));

        // Always hits (no saving throw) - creates 3 missiles
        ActionResult result = new ActionResult();

        for (int i = 0; i < Math.min(targets.size(), 3); i++) {
            Creature target = targets.get(i);
            int damageRoll = DiceRoller.roll(i == 2 ? 1 : 2, 4);
            log.logDamageRoll("Magic Missile", damageRoll, 0, damageRoll, DamageType.FORCE);

            DamageContext damageCtx = new DamageContext(context, damageRoll, DamageType.FORCE);

            // BEFORE_DAMAGE hooks
            caster.triggerEffects(EffectHook.BEFORE_DAMAGE_DEALT, damageCtx);

            int totalDamage = damageCtx.calculateFinalDamage();
            result.addTargetResult(target, totalDamage, false);  // save always fails
        }

        return result;
    }

    @Override
    protected AbilityType getSpellcastingAbility(Creature caster) {
        return AbilityType.INTELLIGENCE;
    }
}
