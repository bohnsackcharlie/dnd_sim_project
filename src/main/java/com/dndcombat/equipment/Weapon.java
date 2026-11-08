package com.dndcombat.equipment;

import com.dndcombat.core.context.DamageContext;
import com.dndcombat.core.dice.DiceRoller;
import com.dndcombat.enums.DamageType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public class Weapon {

    private final String name;
    private final int damageDieCount;
    private final int damageDice;
    private final DamageType damageType;
    private final int range;
    private final int longRange;
    private final Set<WeaponProperty> properties;
    private int magicBonus;
    private WeaponMastery mastery;

    public Weapon(String name, int damageDieCount, int damageDice,
                  DamageType damageType, int range, int longRange,
                  WeaponProperty... properties) {
        this.name = name;
        this.damageDieCount = damageDieCount;
        this.damageDice = damageDice;
        this.damageType = damageType;
        this.range = range;
        this.longRange = longRange;
        this.properties = properties.length > 0
                ? EnumSet.copyOf(Arrays.asList(properties))
                : EnumSet.noneOf(WeaponProperty.class);
    }

    public int rollDamage() {
        return DiceRoller.roll(damageDieCount, damageDice);
    }

    public boolean hasProperty(WeaponProperty prop) {
        return properties.contains(prop);
    }

    public boolean isRanged() {
        return hasProperty(WeaponProperty.AMMUNITION);
    }

    public boolean isMelee() {
        return !isRanged();
    }

    /**
     * Called when this weapon hits a target, before effect hooks.
     * Subclasses override to add bonus damage components
     * (e.g., FlameTongue adds 2d6 fire).
     */
    public void onHit(DamageContext damageCtx) {
        // Default: no bonus damage.
    }

    // --- Getters / Setters ---

    public String getName() {
        return name;
    }

    public int getDamageDieCount() {
        return damageDieCount;
    }

    public int getDamageDice() {
        return damageDice;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public int getRange() {
        return range;
    }

    public int getLongRange() {
        return longRange;
    }

    public Set<WeaponProperty> getProperties() {
        return properties;
    }

    public int getMagicBonus() {
        return magicBonus;
    }

    public void setMagicBonus(int magicBonus) {
        this.magicBonus = magicBonus;
    }

    public WeaponMastery getMastery() {
        return mastery;
    }

    public void setMastery(WeaponMastery mastery) {
        this.mastery = mastery;
    }
}
