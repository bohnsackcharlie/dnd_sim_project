package com.dndcombat.effects;

public enum EffectHook {
    START_OF_TURN,
    END_OF_TURN,

    // Attack-related hooks (receive AttackContext)
    BEFORE_ATTACK_ROLL,
    AFTER_ATTACK_ROLL,
    WHEN_BEING_ATTACKED,
    WHEN_HIT_BY_ATTACK,

    // Damage-related hooks (receive DamageContext)
    BEFORE_DAMAGE_DEALT,
    AFTER_DAMAGE_DEALT,
    BEFORE_DAMAGE_TAKEN,
    AFTER_DAMAGE_TAKEN,

    // Saving throw hooks (receive SaveContext)
    BEFORE_SAVING_THROW,
    AFTER_SAVING_THROW,
    AFTER_ENEMY_SAVING_THROW,
    WHEN_FORCING_SAVE,

    // Spell-related hooks (receive base EffectContext)
    WHEN_CASTING_SPELL,

    // Other hooks
    AFTER_ENEMY_ATTACK_ROLL
}
