package com.dndcombat.ui;

import com.dndcombat.actions.Action;
import com.dndcombat.core.creatures.Creature;

import java.util.List;

public interface InputHandler {

    Action chooseAction(List<Action> available, Creature creature);

    List<Creature> chooseTargets(Action action, List<Creature> combatants, Creature user);

    boolean promptYesNo(String message);
}
