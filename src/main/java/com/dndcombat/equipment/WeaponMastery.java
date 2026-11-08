package com.dndcombat.equipment;

import com.dndcombat.core.context.AttackContext;
import com.dndcombat.core.creatures.Creature;

public interface WeaponMastery {

    void applyOnAttack(AttackContext context);

    void applyOnHit(AttackContext context, Creature target);
}
