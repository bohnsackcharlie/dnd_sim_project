package com.dndcombat.actions.abilities.barbarian;

import com.dndcombat.actions.Action;
import com.dndcombat.actions.ActionType;
import com.dndcombat.actions.TargetingRequirements;
import com.dndcombat.core.combat.ActionResult;
import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.effects.barbarian.RageBuff;

public class Rage extends Action {
    private static final int DURATION = 100; // turns

    public Rage() {
        super("Rage", ActionType.BONUS_ACTION, TargetingRequirements.self());

        // TODO we could probably do a getDescription method that fills in the damage bonus based on Barb level later
        this.description = """
                As a bonus action you begin your Rage and gain the following benefits:
                Damage Resistance. You have Resistance to Bludgeoning, Piercing, and Slashing damage.

                Rage Damage. When you make an attack using Strength with either a weapon or an Unarmed Strike—and deal damage to the target, you gain a +2 bonus to the damage.

                No Concentration or Spells. You can't maintain Concentration, and you can't cast spells.

                Duration. The Rage lasts until the end of your next turn, and it ends early if you don Heavy armor or have the Incapacitated condition. If your Rage is still active on your next turn, you can extend the Rage for another round by doing one of the following:

                Make an attack roll against an enemy.
                Force an enemy to make a saving throw.
                Take a Bonus Action to extend your Rage.
                Each time the Rage is extended, it lasts until the end of your next turn. You can maintain a Rage for up to 10 minutes.
                """;
    }

    @Override
    public ActionResult execute(EffectContext context) {
        Creature user = context.getActionUser();

        RageBuff buff = new RageBuff(DURATION);
        user.addEffect(buff);

        // TODO consume rage use

        return ActionResult.success();
    }
}