package com.dndcombat.actions.attacks;

import com.dndcombat.actions.Action;
import com.dndcombat.actions.ActionType;
import com.dndcombat.actions.TargetingRequirements;
import com.dndcombat.core.combat.ActionResult;
import com.dndcombat.core.combat.CombatLog;
import com.dndcombat.core.context.AttackContext;
import com.dndcombat.core.context.DamageContext;
import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.effects.EffectHook;
import com.dndcombat.enums.AttackType;
import com.dndcombat.equipment.Weapon;
import com.dndcombat.equipment.WeaponProperty;

public class WeaponAttack extends Action {

    private final Weapon weapon;

    public WeaponAttack(Weapon weapon) {
        super(weapon.getName() + " Attack", ActionType.ACTION,
              TargetingRequirements.singleHostile(weapon.getRange()));
        this.weapon = weapon;
        // TODO we can probably add more info to this description conditionally on its range, properties, magic bonus etc, should break out into its own method at that point
        this.description = "Use the Attack action to attack with your " + weapon.getName() + ", dealing " + weapon.getDamageDieCount() + "d" + weapon.getDamageDice() + " " + weapon.getDamageType().name() + " damage to the selected target.";
    }

    @Override
    public ActionResult execute(EffectContext context) {
        Creature attacker = context.getActionUser();
        Creature target = context.getPrimaryTarget();

        // === ATTACK PHASE ===
        AttackType type = weapon.hasProperty(WeaponProperty.AMMUNITION)
                ? AttackType.RANGED_WEAPON : AttackType.MELEE_WEAPON;
        int attackBonus = attacker.getAttackBonus(weapon) + weapon.getMagicBonus();
        AttackContext attackCtx = new AttackContext(context, type, attackBonus);

        rollAttackRoll(attacker, target, attackCtx);
        CombatLog log = context.getCombatManager().getLog();

        // === CHECK FOR HIT ===
        if (attackCtx.isHit()) {

            // === DAMAGE PHASE — create typed DamageContext ===
            int damageRoll = weapon.rollDamage();
            int damageBonus = attacker.getDamageBonus(weapon) + weapon.getMagicBonus();

            // Critical hit — double the dice
            if (attackCtx.isCritical()) {
                int critRoll = weapon.rollDamage();
                log.logCritDamageRoll(weapon.getName(), damageRoll, critRoll,
                        damageBonus, damageRoll + critRoll + damageBonus,
                        weapon.getDamageType());
                damageRoll += critRoll;
            } else {
                log.logDamageRoll(weapon.getName(), damageRoll, damageBonus,
                        damageRoll + damageBonus, weapon.getDamageType());
            }

            DamageContext damageCtx = new DamageContext(
                    context, damageRoll + damageBonus, weapon.getDamageType());

            // Weapon subclass hook for bonus damage (e.g., FlameTongue adds 2d6 fire)
            weapon.onHit(damageCtx);

            // BEFORE_DAMAGE hooks — both sides modify the same DamageContext
            attacker.triggerEffects(EffectHook.BEFORE_DAMAGE_DEALT, damageCtx);
            target.triggerEffects(EffectHook.BEFORE_DAMAGE_TAKEN, damageCtx);

            // Apply weapon mastery effects on hit
            if (weapon.getMastery() != null) {
                weapon.getMastery().applyOnHit(attackCtx, target);
            }

            // Single path for final damage — all components resolved together
            int totalDamage = damageCtx.calculateFinalDamage();
            target.takeDamage(totalDamage);

            return ActionResult.hit(totalDamage, damageCtx, attackCtx.isCritical());
        }

        return ActionResult.miss();
    }

    @Override
    public String getDescription(Creature user) {
        // TODO break these out into their own actions if they get more complicated
        int attackBonus = user.getAttackBonus(weapon) + weapon.getMagicBonus();
        int damageBonus = user.getDamageBonus(weapon) + weapon.getMagicBonus();

        return """
                Use the Attack action to attack with your %s.
                Attack bonus: %+d.
                Damage: %dd%d %+d %s.
                """
                .formatted(
                        weapon.getName(),
                        attackBonus,
                        weapon.getDamageDieCount(),
                        weapon.getDamageDice(),
                        damageBonus,
                        weapon.getDamageType().name()
                ).strip();
    }

    public boolean isMelee() {
        return weapon.isMelee();
    }

    public Weapon getWeapon() {
        return weapon;
    }
}
