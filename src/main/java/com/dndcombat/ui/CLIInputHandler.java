package com.dndcombat.ui;

import com.dndcombat.actions.Action;
import com.dndcombat.core.creatures.Creature;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class CLIInputHandler implements InputHandler {

    private final Scanner scanner;

    public CLIInputHandler() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public Action chooseAction(List<Action> available, Creature creature) {
        System.out.printf("%n[%s] Choose an action:%n", creature.getName());
        for (int i = 0; i < available.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, available.get(i).getName());
        }
        System.out.printf("  0. Skip%n");
        System.out.print("> ");

        int choice = readInt(0, available.size());
        if (choice == 0) return null;
        return available.get(choice - 1);
    }

    @Override
    public List<Creature> chooseTargets(Action action, List<Creature> combatants,
                                         Creature user) {
        List<Creature> validTargets = combatants.stream()
                .filter(c -> c.isAlive() && c != user)
                .filter(c -> action.isValidTarget(c, user))
                .collect(Collectors.toList());

        if (validTargets.isEmpty()) {
            System.out.println("  No valid targets.");
            return List.of();
        }

        int maxTargets = action.getTargetingReqs() != null
                ? action.getTargetingReqs().getMaxTargets() : 1;

        // Single-target action
        if (maxTargets <= 1) {
            if (validTargets.size() == 1) {
                System.out.printf("  Target: %s%n", validTargets.get(0));
                return validTargets;
            }

            System.out.printf("[%s] Choose a target for %s:%n", user.getName(), action.getName());
            for (int i = 0; i < validTargets.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, validTargets.get(i));
            }
            System.out.print("> ");

            int choice = readInt(1, validTargets.size());
            return List.of(validTargets.get(choice - 1));
        }

        // Multi-target action
        List<Creature> chosen = new ArrayList<>();
        int minTargets = action.getTargetingReqs() != null
                ? action.getTargetingReqs().getMinTargets() : 1;

        System.out.printf("[%s] Choose targets for %s (min %d, max %d):%n",
                user.getName(), action.getName(), minTargets, maxTargets);

        while (chosen.size() < maxTargets) {
            List<Creature> remaining = validTargets.stream()
                    .filter(c -> !chosen.contains(c))
                    .collect(Collectors.toList());

            if (remaining.isEmpty()) break;

            for (int i = 0; i < remaining.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, remaining.get(i));
            }
            if (chosen.size() >= minTargets) {
                System.out.printf("  0. Done (%d selected)%n", chosen.size());
            }
            System.out.print("> ");

            int min = chosen.size() >= minTargets ? 0 : 1;
            int choice = readInt(min, remaining.size());

            if (choice == 0) break;
            chosen.add(remaining.get(choice - 1));
            System.out.printf("  Added %s%n", chosen.get(chosen.size() - 1).getName());
        }

        return chosen;
    }

    @Override
    public boolean promptYesNo(String message) {
        System.out.printf("%s (y/n): ", message);
        while (true) {
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) return true;
            if (input.equals("n") || input.equals("no")) return false;
            System.out.print("Please enter y or n: ");
        }
    }

    private int readInt(int min, int max) {
        while (true) {
            try {
                String line = scanner.nextLine().trim();
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) return value;
                System.out.printf("Please enter a number between %d and %d: ", min, max);
            } catch (NumberFormatException e) {
                System.out.printf("Please enter a number between %d and %d: ", min, max);
            }
        }
    }
}
