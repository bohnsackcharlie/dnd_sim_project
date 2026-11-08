package com.dndcombat.actions.spells.levelThree;

import com.dndcombat.actions.ActionType;
import com.dndcombat.actions.TargetingRequirements;
import com.dndcombat.actions.spells.School;
import com.dndcombat.actions.spells.Spell;
import com.dndcombat.core.combat.ActionResult;
import com.dndcombat.core.combat.CombatLog;
import com.dndcombat.core.context.DamageContext;
import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.context.SaveContext;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.core.dice.DiceRoller;
import com.dndcombat.effects.EffectHook;
import com.dndcombat.enums.AbilityType;
import com.dndcombat.enums.DamageType;

import java.util.List;

public class LightningBolt extends Spell {

    public LightningBolt() {
        super("Lightning Bolt", 3, School.EVOCATION, ActionType.ACTION);
        this.targetingReqs = new TargetingRequirements(1, 20, true, 100);
    }

    @Override
    public ActionResult execute(EffectContext context) {
        Creature caster = context.getActionUser();
        List<Creature> targets = context.getTargets();
        CombatLog log = context.getCombatManager().getLog();

        int saveDC = getSpellSaveDC(caster);
        int baseDamage = DiceRoller.roll(8, 6);

        log.logMessage(String.format("Lightning Bolt! %s rolls 8d6 = %d lightning damage (DC %d DEX save)",
                caster.getName(), baseDamage, saveDC));

        ActionResult result = new ActionResult();

        for (Creature target : targets) {
            // === SAVE PHASE ===
            SaveContext saveCtx = new SaveContext(context, AbilityType.DEXTERITY, saveDC);
            rollSavingThrow(target, saveCtx);

            // === DAMAGE PHASE ===
            int damage = saveCtx.isSucceeded() ? baseDamage / 2 : baseDamage;

            DamageContext damageCtx = new DamageContext(context, damage, DamageType.LIGHTNING);

            target.triggerEffects(EffectHook.BEFORE_DAMAGE_TAKEN, damageCtx);

            int totalDamage = damageCtx.calculateFinalDamage();
            target.takeDamage(totalDamage);

            log.logMessage(String.format("%s takes %d lightning damage [%s]",
                    target.getName(), totalDamage, target));

            result.addTargetResult(target, totalDamage, saveCtx.isSucceeded());
        }

        return result;
    }
}
