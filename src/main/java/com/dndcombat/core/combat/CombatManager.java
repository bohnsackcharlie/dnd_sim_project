package com.dndcombat.core.combat;

import com.dndcombat.actions.Action;
import com.dndcombat.actions.ActionType;
import com.dndcombat.ai.ActionDecision;
import com.dndcombat.ai.ResolvedActionDecision;
import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.core.creatures.Monster;
import com.dndcombat.core.dice.DiceRoller;
import com.dndcombat.effects.EffectHook;
import com.dndcombat.effects.OngoingEffect;
import com.dndcombat.enums.AbilityType;
import com.dndcombat.enums.Team;
import com.dndcombat.ui.InputHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CombatManager {

    private final List<Creature> combatants = new ArrayList<>();
    private Queue<Creature> initiativeOrder;
    private int currentRound;
    private Creature currentTurn;

    private final CombatLog log;
    private final InputHandler inputHandler;

    public CombatManager(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
        this.log = new CombatLog();
    }

    public CombatManager(InputHandler inputHandler, CombatLog log) {
        this.inputHandler = inputHandler;
        this.log = log;
    }

    // --- Setup ---

    public void addCombatant(Creature creature) {
        combatants.add(creature);
    }

    // --- Combat Loop ---

    public void startCombat() {
        log.logCombatStart();
        rollInitiative();
        currentRound = 1;

        while (!combatEnded()) {
            Creature creature = initiativeOrder.poll();
            if (creature != null && creature.isAlive()) {
                processTurn(creature);
            }
        }

        log.logCombatEnd();
        logVictory();
    }

    private void rollInitiative() {
        // Roll initiative for each combatant and sort descending
        List<InitiativeEntry> entries = new ArrayList<>();
        for (Creature creature : combatants) {
            int d20 = DiceRoller.d20();
            int bonus = creature.getInitiativeBonus();
            int total = d20 + bonus;
            entries.add(new InitiativeEntry(creature, total));
            log.logInitiative(creature, d20, bonus, total);
        }

        entries.sort(Comparator.comparingInt(InitiativeEntry::roll).reversed());

        initiativeOrder = new LinkedList<>();
        for (InitiativeEntry entry : entries) {
            initiativeOrder.offer(entry.creature());
        }
    }

    // --- Turn Processing ---

    private void processTurn(Creature creature) {
        currentTurn = creature;
        log.logTurnStart(creature);

        creature.resetActions();

        creature.triggerEffects(
                EffectHook.START_OF_TURN,
                new EffectContext(this, creature)
        );

        if (creature.canAct()) {
            processAvailableActions(creature);
        }

        processTurnEnd(creature);
    }

    private List<Action> getAvailableTurnActions(Creature creature) {
        List<Action> available = new ArrayList<>();

        if (!creature.isActionUsed()) {
            available.addAll(creature.getAvailableActions());
        }

        if (!creature.isBonusActionUsed()) {
            available.addAll(creature.getAvailableBonusActions());
        }

        return available.stream()
                .filter(action -> action.canUse(creature))
                .toList();
    }

    private void processAvailableActions(Creature creature) {

        while (creature.canAct()) {

            List<Action> availableActions = getAvailableTurnActions(creature);

            if (availableActions.isEmpty()) {
                break;
            }

            Action chosen;
            List<Creature> targets;

            if (creature.getTeam() == Team.PARTY) {

                chosen = inputHandler.chooseAction(availableActions, creature);
                if (chosen == null) break;

                targets = inputHandler.chooseTargets(
                        chosen, combatants, creature
                );

            } else if (creature instanceof Monster monster
                    && monster.getAiStrategy() != null) {

                ResolvedActionDecision decision = monster.getAiStrategy()
                        .chooseAction(creature, combatants);
                System.out.println(decision);

                if (decision == null) break;

                chosen = decision.action();
                targets = decision.targets();

            } else {
                break;
            }

            if (chosen == null || targets == null || targets.isEmpty()) {
                break;
            }

            // Safety check: the selected action must still be available.
            if (!availableActions.contains(chosen)) {
                break;
            }

            executeAction(creature, chosen, targets);
        }
    }

    private void executeAction(Creature user, Action action, List<Creature> targets) {
        // Create base context — action creates typed sub-contexts internally
        EffectContext context = new EffectContext(this, action, user, targets);

        // Execute the action
        ActionResult result = action.execute(context);

        // Log result
        log.logAction(user, action, targets, result);

        // Mark action as used
        if (action.getActionType() == ActionType.ACTION) {
            user.setActionUsed(true);
        } else if (action.getActionType() == ActionType.BONUS_ACTION) {
            user.setBonusActionUsed(true);
        }

        // Check for concentration breaks on damaged targets
        for (Creature target : targets) {
            if (result.dealtDamage(target)) {
                checkConcentration(target, result.getDamage(target));
            }
        }

        // Also handle single-target WeaponAttack path (uses ActionResult.hit directly)
        if (result.isHit() && result.getDamageContext() != null
                && result.getTargetResults().isEmpty()) {
            Creature target = targets.get(0);
            int damage = result.getDamageContext().calculateFinalDamage();
            if (damage > 0 && target.getConcentratingOn() != null) {
                checkConcentration(target, damage);
            }
        }
    }

    private void processTurnEnd(Creature creature) {
        // Trigger end-of-turn effects
        creature.triggerEffects(EffectHook.END_OF_TURN,
                new EffectContext(this, creature));

        // Decrement effect durations and remove expired
        creature.decrementEffectDurations();
        creature.removeExpiredEffects(new EffectContext(this, creature));

        log.logTurnEnd(creature);

        // Re-queue if still alive
        if (creature.isAlive()) {
            initiativeOrder.offer(creature);
        }

        // Check if round ended (all combatants have acted)
        if (!initiativeOrder.isEmpty()
                && initiativeOrder.peek() == getFirstAlive()) {
            log.logRoundEnd(currentRound);
            currentRound++;
        }
    }

    // --- Concentration ---

    private void checkConcentration(Creature creature, int damage) {
        if (creature.getConcentratingOn() == null) return;

        int dc = Math.max(10, damage / 2);
        int d20 = DiceRoller.d20();
        int saveBonus = creature.getSavingThrowBonus(AbilityType.CONSTITUTION);
        int total = d20 + saveBonus;
        boolean success = total >= dc;

        log.logConcentrationCheck(creature, success, dc, d20, saveBonus, total);

        if (!success) {
            creature.breakConcentration();
        }
    }

    // --- Reaction Prompting ---

    public boolean promptReaction(Creature creature, OngoingEffect trigger,
                                   EffectContext context) {
        if (creature.getTeam() == Team.PARTY) {
            return inputHandler.promptYesNo(
                    String.format("Use %s? (Reaction)", trigger.getName()));
        } else if (creature instanceof Monster monster && monster.getAiStrategy() != null) {
            return monster.getAiStrategy().shouldUseReaction(trigger, context);
        }
        return false;
    }

    // --- Combat End Conditions ---

    private boolean combatEnded() {
        long aliveParty = combatants.stream()
                .filter(c -> c.getTeam() == Team.PARTY && c.isAlive())
                .count();
        long aliveEnemies = combatants.stream()
                .filter(c -> c.getTeam() == Team.ENEMIES && c.isAlive())
                .count();

        return aliveParty == 0 || aliveEnemies == 0;
    }

    private void logVictory() {
        long aliveParty = combatants.stream()
                .filter(c -> c.getTeam() == Team.PARTY && c.isAlive())
                .count();

        if (aliveParty > 0) {
            log.logMessage("The party is victorious!");
        } else {
            log.logMessage("The party has been defeated.");
        }
    }

    // --- Getters ---

    public List<Creature> getAllCombatants() {
        return combatants;
    }

    public Creature getCurrentTurn() {
        return currentTurn;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public CombatLog getLog() {
        return log;
    }

    private Creature getFirstAlive() {
        return combatants.stream()
                .filter(Creature::isAlive)
                .findFirst()
                .orElse(null);
    }

    // --- Internal ---

    private record InitiativeEntry(Creature creature, int roll) {
    }
}
