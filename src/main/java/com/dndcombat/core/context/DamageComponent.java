package com.dndcombat.core.context;

import com.dndcombat.enums.DamageType;

public class DamageComponent {

    private final DamageType type;
    private int amount;

    public DamageComponent(DamageType type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public DamageType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void addAmount(int mod) {
        this.amount += mod;
    }
}
