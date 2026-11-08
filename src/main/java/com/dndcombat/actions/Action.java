package com.dndcombat.actions;

import com.dndcombat.core.combat.ActionResult;
import com.dndcombat.core.combat.CombatLog;
import com.dndcombat.core.context.AttackContext;
import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.context.SaveContext;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.core.dice.DiceRoller;
import com.dndcombat.effects.EffectHook;

public abstract class Action {

    protected String name;
    protected String description;
    protected ActionType actionType;
    protected TargetingRequirements targetingReqs;

    protected Action(String name, ActionType actionType) {
        this.name = name;
        this.actionType = actionType;
    }

    protected Action(String name, ActionType actionType, TargetingRequirements targetingReqs) {
        this.name = name;
        this.actionType = actionType;
        this.targetingReqs = targetingReqs;
    }

    public abstract ActionResult execute(EffectContext context);

    public boolean canUse(Creature user, EffectContext context) {
        return switch (actionType) {
            case ACTION -> !user.isActionUsed();
            case BONUS_ACTION -> !user.isBonusActionUsed();
            case REACTION -> user.hasReactionAvailable();
        };
    }

    public boolean isValidTarget(Creature target, Creature user) {
        if (targetingReqs == null) return true;
        if (targetingReqs.isRequiresHostile() && !user.isHostileTo(target)) return false;
        return target.isAlive();
    }

    // --- Attack Roll Resolution ---

    protected void rollAttackRoll(Creature attacker, Creature target,
                                   AttackContext attackCtx) {
        CombatLog log = attackCtx.getCombatManager().getLog();

        attacker.triggerEffects(EffectHook.BEFORE_ATTACK_ROLL, attackCtx);
        target.triggerEffects(EffectHook.WHEN_BEING_ATTACKED, attackCtx);

        int attackRoll = attackCtx.rollAttack();
        int totalAttack = attackCtx.applyRollModifiers(attackRoll + attackCtx.getAttackBonus());

        attackCtx.setAttackRoll(attackRoll);
        attackCtx.setAttackTotal(totalAttack);
        attackCtx.setCritical(attackRoll == 20);

        log.logAttackRoll(attacker.getName(), target.getName(),
                attackRoll, attackCtx.getAttackBonus(), totalAttack, target.getArmorClass(),
                attackCtx.shouldRollWithAdvantage(),
                attackCtx.shouldRollWithDisadvantage());

        if (totalAttack >= target.getArmorClass() || attackCtx.isCritical()) {
            attackCtx.setHit(true);
            target.triggerEffects(EffectHook.WHEN_HIT_BY_ATTACK, attackCtx);
        }
    }

    // --- Saving Throw Resolution ---

    protected void rollSavingThrow(Creature target, SaveContext saveCtx) {
        CombatLog log = saveCtx.getCombatManager().getLog();

        target.triggerEffects(EffectHook.BEFORE_SAVING_THROW, saveCtx);

        int saveRoll = DiceRoller.d20();
        int saveBonus = target.getSavingThrowBonus(saveCtx.getSaveAbility());
        int totalSave = saveCtx.applyModifiers(saveRoll + saveBonus);

        saveCtx.setSaveRoll(saveRoll);
        saveCtx.setSaveTotal(totalSave);
        saveCtx.setSucceeded(totalSave >= saveCtx.getSaveDC());

        log.logMessage(String.format("%s %s save: d20(%d) + %d = %d vs DC %d — %s",
                target.getName(), saveCtx.getSaveAbility(), saveRoll, saveBonus,
                totalSave, saveCtx.getSaveDC(),
                saveCtx.isSucceeded() ? "PASS" : "FAIL"));
    }

    // --- Getters ---

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public TargetingRequirements getTargetingReqs() {
        return targetingReqs;
    }
}
