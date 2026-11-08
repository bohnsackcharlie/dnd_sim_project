package com.dndcombat.core.combat;

import com.dndcombat.core.context.DamageContext;
import com.dndcombat.core.creatures.Creature;

import java.util.HashMap;
import java.util.Map;

public class ActionResult {

    private boolean hit;
    private boolean critical;
    private DamageContext damageContext;

    private final Map<Creature, TargetResult> targetResults = new HashMap<>();

    public static ActionResult miss() {
        ActionResult result = new ActionResult();
        result.hit = false;
        return result;
    }

    public static ActionResult hit(int totalDamage, DamageContext damageContext, boolean critical) {
        ActionResult result = new ActionResult();
        result.hit = true;
        result.critical = critical;
        result.damageContext = damageContext;
        return result;
    }

    public static ActionResult success() {
        ActionResult result = new ActionResult();
        result.hit = true;
        return result;
    }

    public void addTargetResult(Creature target, int damage, boolean savedSuccessfully) {
        targetResults.put(target, new TargetResult(damage, savedSuccessfully));
    }

    public boolean dealtDamage(Creature target) {
        TargetResult tr = targetResults.get(target);
        return tr != null && tr.damage > 0;
    }

    public int getDamage(Creature target) {
        TargetResult tr = targetResults.get(target);
        return tr != null ? tr.damage : 0;
    }

    public boolean isHit() {
        return hit;
    }

    public boolean isCritical() {
        return critical;
    }

    public DamageContext getDamageContext() {
        return damageContext;
    }

    public Map<Creature, TargetResult> getTargetResults() {
        return targetResults;
    }

    public record TargetResult(int damage, boolean savedSuccessfully) {
    }
}
