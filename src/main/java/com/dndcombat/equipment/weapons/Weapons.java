package com.dndcombat.equipment.weapons;

import com.dndcombat.enums.DamageType;
import com.dndcombat.equipment.Weapon;
import com.dndcombat.equipment.WeaponProperty;

public final class Weapons {

    private Weapons() {
    }

    public static final Weapon LONGSWORD = new Weapon(
            "Longsword", 1, 8, DamageType.SLASHING, 5, 5,
            WeaponProperty.VERSATILE);

    public static final Weapon GREATSWORD = new Weapon(
            "Greatsword", 2, 6, DamageType.SLASHING, 5, 5,
            WeaponProperty.HEAVY, WeaponProperty.TWO_HANDED);

    public static final Weapon SHORTBOW = new Weapon(
            "Shortbow", 1, 6, DamageType.PIERCING, 80, 320,
            WeaponProperty.AMMUNITION, WeaponProperty.TWO_HANDED);

    public static final Weapon RAPIER = new Weapon(
            "Rapier", 1, 8, DamageType.PIERCING, 5, 5,
            WeaponProperty.FINESSE);

    public static final Weapon DAGGER = new Weapon(
            "Dagger", 1, 4, DamageType.PIERCING, 20, 60,
            WeaponProperty.FINESSE, WeaponProperty.LIGHT, WeaponProperty.THROWN);

    public static final Weapon HANDAXE = new Weapon(
            "Handaxe", 1, 6, DamageType.SLASHING, 20, 60,
            WeaponProperty.LIGHT, WeaponProperty.THROWN);

    public static final Weapon GREATAXE = new Weapon(
            "Greataxe", 1, 12, DamageType.SLASHING, 5, 5,
            WeaponProperty.HEAVY, WeaponProperty.TWO_HANDED);

    public static Weapon createMagicWeapon(Weapon base, int bonus) {
        Weapon magic = new Weapon(
                base.getName() + " +" + bonus,
                base.getDamageDieCount(), base.getDamageDice(),
                base.getDamageType(), base.getRange(), base.getLongRange(),
                base.getProperties().toArray(new WeaponProperty[0]));
        magic.setMagicBonus(bonus);
        return magic;
    }
}
