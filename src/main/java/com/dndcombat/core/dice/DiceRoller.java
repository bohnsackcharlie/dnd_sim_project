package com.dndcombat.core.dice;

import java.util.Random;

public class DiceRoller {

    private static Random random = new Random();

    public static int d20() {
        return random.nextInt(20) + 1;
    }

    public static int d4() {
        return random.nextInt(4) + 1;
    }

    public static int roll(int count, int sides) {
        int total = 0;
        for (int i = 0; i < count; i++) {
            total += random.nextInt(sides) + 1;
        }
        return total;
    }

    public static void setRandom(Random r) {
        random = r;
    }
}
