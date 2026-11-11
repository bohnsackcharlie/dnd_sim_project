package com.dndcombat;

import java.time.Duration;
import java.util.List;

import dev.langchain4j.model.ollama.OllamaChatModel;

import com.dndcombat.actions.attacks.WeaponAttack;
import com.dndcombat.ai.PlayerControlledStrategy;
import com.dndcombat.ai.ResolvedActionDecision;
import com.dndcombat.ai.SnapshotSerializer;
import com.dndcombat.ai.CombatSnapshot;
import com.dndcombat.ai.CombatSnapshotFactory;
import com.dndcombat.ai.DecisionValidator;
import com.dndcombat.ai.ActionDecision;
import com.dndcombat.ai.ActionDecisionParser;
import com.dndcombat.ai.CombatPromptBuilder;
import com.dndcombat.core.creatures.Creature;
// import com.dndcombat.core.combat.CombatManager;
import com.dndcombat.core.creatures.Monster;
import com.dndcombat.core.creatures.PlayerCharacter;
import com.dndcombat.enums.CharacterClass;
import com.dndcombat.enums.CreatureType;
import com.dndcombat.equipment.weapons.Weapons;
import com.dndcombat.ui.CLIInputHandler;
import com.dndcombat.ui.InputHandler;

public class Main {

    public static void main(String[] args) {
        InputHandler input = new CLIInputHandler();
        PlayerControlledStrategy playerAI = new PlayerControlledStrategy(input);

        // --- Create Party ---

        // Fighter: Level 5, 52 HP, AC 18 (chain mail + shield)
        // STR 16, DEX 12, CON 14, INT 10, WIS 13, CHA 8
        PlayerCharacter fighter = new PlayerCharacter(
                "Aldric", 5, CharacterClass.FIGHTER,
                52, 18,
                16, 12, 14, 10, 13, 8);
        fighter.addAction(new WeaponAttack(Weapons.LONGSWORD));

        // Rogue: Level 5, 38 HP, AC 15 (studded leather)
        // STR 10, DEX 18, CON 12, INT 14, WIS 10, CHA 14
        PlayerCharacter rogue = new PlayerCharacter(
                "Sable", 5, CharacterClass.ROGUE,
                38, 15,
                10, 18, 12, 14, 10, 14);
        rogue.addAction(new WeaponAttack(Weapons.RAPIER));
        rogue.addAction(new WeaponAttack(Weapons.SHORTBOW));

        // --- Create Monsters ---

        // Goblin 1: CR 1/4, 7 HP, AC 15, Prof +2
        // STR 8, DEX 14, CON 10, INT 10, WIS 8, CHA 8
        Monster goblin1 = new Monster(
                "Goblin Skirmisher", 17, 15, 2,
                0.25, CreatureType.HUMANOID,
                8, 14, 10, 10, 8, 8);
        goblin1.setAiStrategy(playerAI);
        goblin1.addAction(new WeaponAttack(Weapons.HANDAXE));

        // Goblin 2
        Monster goblin2 = new Monster(
                "Goblin Archer", 5, 15, 2,
                0.25, CreatureType.HUMANOID,
                8, 14, 10, 10, 8, 8);
        goblin2.setAiStrategy(playerAI);
        goblin2.addAction(new WeaponAttack(Weapons.SHORTBOW));

        CombatSnapshotFactory combatSnapshotFactory = new CombatSnapshotFactory();
        SnapshotSerializer snapshotSerializer = new SnapshotSerializer();
        CombatPromptBuilder promptBuilder = new CombatPromptBuilder();
        ActionDecisionParser parser = new ActionDecisionParser();
        DecisionValidator validator = new DecisionValidator();

        CombatSnapshot snapshot = combatSnapshotFactory.create(rogue, List.of(fighter, goblin1, goblin2));
        System.out.println(snapshotSerializer.serialize(snapshot));

        String prompt = promptBuilder.buildPrompt(snapshotSerializer.serialize(snapshot));

        // 4. Initialize model
        OllamaChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen3.5:9b")
                .temperature(0.0)
                .timeout(Duration.ofMinutes(5))
                .maxRetries(0) // it normally gets three tries but only has a duration cap of 1 minute. This leads to it reliably failing because it often takes over a minute to run
                .build();

        // 5. Ask the model
        String response = model.chat(prompt);

        System.out.println("Qwen's decision:");
        System.out.println(response);


        ActionDecision decision = parser.parse(response);

        // Validate and resolve
        ResolvedActionDecision resolved = validator.validate(
                decision,
                snapshot,
                rogue,
                List.of(fighter, goblin1, goblin2)
        );

        System.out.println("Selected action: " + resolved.action().getName());

        for (Creature target : resolved.targets()) {
        System.out.println("Selected target: " + target.getName());
        }
        // --- Set up combat ---

        // CombatManager combat = new CombatManager(input);
        // combat.addCombatant(fighter);
        // combat.addCombatant(rogue);
        // combat.addCombatant(goblin1);
        // combat.addCombatant(goblin2);

        // combat.startCombat();
    }
}
