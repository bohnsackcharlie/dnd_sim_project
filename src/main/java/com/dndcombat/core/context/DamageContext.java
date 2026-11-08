package com.dndcombat.core.context;

import com.dndcombat.enums.DamageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DamageContext extends EffectContext {

    private final List<DamageComponent> components;

    private final Set<DamageType> resistedTypes = new HashSet<>();
    private final Set<DamageType> immuneTypes = new HashSet<>();
    private final Set<DamageType> vulnerableTypes = new HashSet<>();

    public DamageContext(EffectContext parent, int damage, DamageType type) {
        super(parent.getCombatManager(), parent.getAction(),
              parent.getActionUser(), parent.getTargets());
        this.components = new ArrayList<>();
        this.components.add(new DamageComponent(type, damage));
    }

    public DamageContext(EffectContext parent, List<DamageComponent> components) {
        super(parent.getCombatManager(), parent.getAction(),
              parent.getActionUser(), parent.getTargets());
        this.components = new ArrayList<>(components);
    }

    // --- Component manipulation ---

    public void addComponent(DamageType type, int amount) {
        components.add(new DamageComponent(type, amount));
    }

    public void addModifier(int mod) {
        if (!components.isEmpty()) {
            components.get(0).addAmount(mod);
        }
    }

    public void addModifierToType(DamageType type, int mod) {
        for (DamageComponent comp : components) {
            if (comp.getType() == type) {
                comp.addAmount(mod);
                return;
            }
        }
    }

    // --- Per-type resistance / immunity / vulnerability ---

    public void addResistance(DamageType type) {
        resistedTypes.add(type);
    }

    public void addImmunity(DamageType type) {
        immuneTypes.add(type);
    }

    public void addVulnerability(DamageType type) {
        vulnerableTypes.add(type);
    }

    public void addPhysicalResistance() {
        resistedTypes.add(DamageType.BLUDGEONING);
        resistedTypes.add(DamageType.PIERCING);
        resistedTypes.add(DamageType.SLASHING);
    }

    // --- Damage calculation ---

    public int calculateFinalDamage() {
        int total = 0;
        for (DamageComponent comp : components) {
            if (immuneTypes.contains(comp.getType())) continue;
            int dmg = comp.getAmount();
            if (vulnerableTypes.contains(comp.getType())) dmg *= 2;
            if (resistedTypes.contains(comp.getType())) dmg /= 2;
            total += dmg;
        }
        return Math.max(0, total);
    }

    // --- Query methods ---

    public List<DamageComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public DamageType getPrimaryDamageType() {
        return components.isEmpty() ? null : components.get(0).getType();
    }

    public boolean includesDamageType(DamageType type) {
        return components.stream().anyMatch(c -> c.getType() == type);
    }

    public boolean includesPhysicalDamage() {
        return components.stream().anyMatch(c -> c.getType().isPhysical());
    }

    public boolean includesElementalDamage() {
        return components.stream().anyMatch(c -> c.getType().isElemental());
    }
}
