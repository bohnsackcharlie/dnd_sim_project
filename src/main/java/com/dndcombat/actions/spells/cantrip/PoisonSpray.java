package com.dndcombat.actions.spells.cantrip;

import com.dndcombat.actions.ActionType;
import com.dndcombat.actions.TargetingRequirements;
import com.dndcombat.actions.spells.Cantrip;
import com.dndcombat.actions.spells.School;
import com.dndcombat.core.combat.ActionResult;
import com.dndcombat.core.combat.CombatLog;
import com.dndcombat.core.context.DamageContext;
import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.core.dice.DiceRoller;
import com.dndcombat.effects.EffectHook;
import com.dndcombat.enums.AbilityType;
import com.dndcombat.enums.DamageType;

public class PoisonSpray extends Cantrip {

    public PoisonSpray() {
        super("Poison Spray", School.NECROMANCY, ActionType.REACTION, 0);
        this.targetingReqs = new TargetingRequirements(1, 1, true, 60);
    }

    @Override
    public ActionResult execute(EffectContext context) {
        Creature caster = context.getActionUser();
        Creature target = context.getPrimaryTarget();

        // Use Intelligence for DC if spellcasting ability
        int saveDC = getSpellSaveDC(caster);

        // Create DamageContext directly (no attack roll needed for cantrip damage)
        int damageRoll = DiceRoller.roll(1, 4);
        logDamageRoll("d4", damageRoll, 0, damageRoll, DamageType.POISON);

        DamageContext damageCtx = new DamageContext(context, damageRoll, DamageType.POISON);

        // BEFORE_DAMAGE hooks
        caster.triggerEffects(EffectHook.BEFORE_DAMAGE_DEALT, damageCtx);
        target.triggerEffects(EffectHook.BEFORE_DAMAGE_TAKEN, damageCtx);

        int totalDamage = damageCtx.calculateFinalDamage();
        target.takeDamage(totalDamage);

        logMessage(String.format("%s makes a ranged attack against %s — %d poison damage (DC %d CON save)",
                caster.getName(), target.getName(), totalDamage, saveDC));

        return ActionResult.success();
    }

    protected CombatLog getCombatLog() {
        return context.getCombatManager().getLog();
    }

    private void logMessage(String message) {
        getCombatLog().logMessage(message);
    }

    private void logDamageRoll(String weaponName, int diceResult, int bonus, int total,
                                DamageType damageType) {
        getCombatLog().logDamageRoll(weaponName, diceResult, bonus, total, damageType);
    }

    @Override
    protected AbilityType getSpellcastingAbility(Creature caster) {
        return AbilityType.INTELLIGENCE;
    }
}
