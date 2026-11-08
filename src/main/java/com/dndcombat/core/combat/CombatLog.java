package com.dndcombat.core.combat;

import com.dndcombat.actions.Action;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.enums.DamageType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CombatLog {

    private final List<String> logEntries = new ArrayList<>();

    public void logTurnStart(Creature creature) {
        log(String.format("%n=== %s's Turn === [%s]", creature.getName(), creature));
    }

    public void logTurnEnd(Creature creature) {
        log(String.format("--- End of %s's Turn ---", creature.getName()));
    }

    public void logAction(Creature user, Action action, List<Creature> targets,
                          ActionResult result) {
        String targetNames = targets.stream()
                .map(Creature::getName)
                .collect(Collectors.joining(", "));

        if (result.isHit()) {
            if (result.isCritical()) {
                log(String.format("  %s uses %s on %s — CRITICAL HIT!",
                        user.getName(), action.getName(), targetNames));
            } else {
                log(String.format("  %s uses %s on %s — Hit!",
                        user.getName(), action.getName(), targetNames));
            }
            for (var entry : result.getTargetResults().entrySet()) {
                Creature target = entry.getKey();
                ActionResult.TargetResult tr = entry.getValue();
                if (tr.damage() > 0) {
                    log(String.format("    %s takes %d damage [%s]",
                            target.getName(), tr.damage(), target));
                }
            }
            // Single-target hit without targetResults (WeaponAttack path)
            if (result.getTargetResults().isEmpty() && result.getDamageContext() != null) {
                Creature target = targets.get(0);
                log(String.format("    %s takes %d total damage [%s]",
                        target.getName(),
                        result.getDamageContext().calculateFinalDamage(),
                        target));
            }
        } else {
            log(String.format("  %s uses %s on %s — Miss!",
                    user.getName(), action.getName(), targetNames));
        }
    }

    // --- Dice Roll Logging ---

    public void logAttackRoll(String attackerName, String targetName,
                               int d20, int bonus, int total, int targetAC,
                               boolean advantage, boolean disadvantage) {
        String advStr = "";
        if (advantage) advStr = " (advantage)";
        if (disadvantage) advStr = " (disadvantage)";

        log(String.format("  Attack Roll%s: d20(%d) + %d = %d vs AC %d",
                advStr, d20, bonus, total, targetAC));
    }

    public void logDamageRoll(String weaponName, int diceResult, int bonus, int total,
                               DamageType damageType) {
        log(String.format("  Damage Roll: %s %d + %d = %d %s",
                weaponName, diceResult, bonus, total, damageType));
    }

    public void logCritDamageRoll(String weaponName, int firstRoll, int critRoll,
                                   int bonus, int total, DamageType damageType) {
        log(String.format("  Damage Roll (CRIT): %s (%d + %d) + %d = %d %s",
                weaponName, firstRoll, critRoll, bonus, total, damageType));
    }

    // --- Other Logging ---

    public void logRoundEnd(int round) {
        log(String.format("%n========== End of Round %d ==========", round));
    }

    public void logCombatStart() {
        log("*** COMBAT BEGINS ***");
    }

    public void logCombatEnd() {
        log("\n*** COMBAT ENDS ***");
    }

    public void logInitiative(Creature creature, int d20, int bonus, int total) {
        log(String.format("  %s rolls initiative: d20(%d) + %d = %d",
                creature.getName(), d20, bonus, total));
    }

    public void logConcentrationCheck(Creature creature, boolean success, int dc,
                                       int d20, int bonus, int total) {
        String result = success ? "PASS" : "FAIL";
        log(String.format("  Concentration save: d20(%d) + %d = %d vs DC %d — %s",
                d20, bonus, total, dc, result));
        if (!success) {
            log(String.format("  %s LOSES concentration!", creature.getName()));
        }
    }

    public void logMessage(String message) {
        log("  " + message);
    }

    private void log(String message) {
        logEntries.add(message);
        System.out.println(message);
    }

    public List<String> getLogEntries() {
        return logEntries;
    }
}
