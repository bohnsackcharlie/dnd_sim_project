package com.dndcombat.actions.spells.levelOne;

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

public class InflictWounds extends Spell {

    public InflictWounds() {
        super("Inflict Wounds", 1, School.NECROMANCY, ActionType.ACTION);
        this.targetingReqs = new TargetingRequirements(1, 1, true, 0); // Touch range (0)
    }

    @Override
    public ActionResult execute(EffectContext context) {
        Creature caster = context.getActionUser();
        List<Creature> targets = context.getTargets();
        CombatLog log = context.getCombatManager().getLog();

        int saveDC = getSpellSaveDC(caster);
        int baseDamage = DiceRoller.roll(2, 10);

        log.logMessage(String.format("Inflict Wounds! %s rolls 2d10 = %d necrotic damage (DC %d CON save)",
                caster.getName(), baseDamage, saveDC));

        ActionResult result = new ActionResult();

        for (Creature target : targets) {
            // === SAVE PHASE ===
            SaveContext saveCtx = new SaveContext(context, AbilityType.CONSTITUTION, saveDC);
            rollSavingThrow(target, saveCtx);

            // === DAMAGE PHASE ===
            int damage = saveCtx.isSucceeded() ? baseDamage / 2 : baseDamage;

            DamageContext damageCtx = new DamageContext(context, damage, DamageType.NECROTIC);

            target.triggerEffects(EffectHook.BEFORE_DAMAGE_TAKEN, damageCtx);

            int totalDamage = damageCtx.calculateFinalDamage();
            target.takeDamage(totalDamage);

            log.logMessage(String.format("%s takes %d necrotic damage [%s]",
                    target.getName(), totalDamage, target));

            result.addTargetResult(target, totalDamage, saveCtx.isSucceeded());
        }

        return result;
    }

    @Override
    protected AbilityType getSpellcastingAbility(Creature caster) {
        return AbilityType.WISDOM;
    }
}
