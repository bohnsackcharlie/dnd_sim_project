package com.dndcombat.ai;

import dev.langchain4j.model.ollama.OllamaChatModel;

import java.time.Duration;
import java.util.List;

import com.dndcombat.core.context.EffectContext;
import com.dndcombat.core.creatures.Creature;
import com.dndcombat.effects.OngoingEffect;
import com.dndcombat.ui.InputHandler;

public class AiControlledStrategy implements BehaviorStrategy {

    private final CombatSnapshotFactory snapshotFactory =
            new CombatSnapshotFactory();

    private final SnapshotSerializer serializer =
            new SnapshotSerializer();

    private final CombatPromptBuilder promptBuilder =
            new CombatPromptBuilder();

    private final ActionDecisionParser parser =
            new ActionDecisionParser();

    private final DecisionValidator validator =
            new DecisionValidator();

    private final InputHandler inputHandler;
    private final OllamaChatModel model;

    public AiControlledStrategy(InputHandler inputHandler) {
        this.inputHandler = inputHandler;

        model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen3.5:9b")
                .temperature(0.0)
                .timeout(Duration.ofMinutes(5))
                .maxRetries(0) // it normally gets three tries but only has a duration cap of 1 minute. This leads to it reliably failing because it often takes over a minute to run
                .build();
    }

    @Override
    public ResolvedActionDecision chooseAction(
            Creature creature,
            List<Creature> combatants) {

        CombatSnapshot snapshot =
                snapshotFactory.create(creature, combatants);

        String snapshotJson = serializer.serialize(snapshot);
        System.out.print(snapshotJson);

        String prompt = promptBuilder.buildPrompt(snapshotJson);

        String response = model.chat(prompt);

        System.out.print(response);

        ActionDecision decision = parser.parse(response);

        return validator.validate(
                decision, snapshot, creature, combatants
        );
    }

    // TODO allow ai to make reaction decisions
    @Override
    public boolean shouldUseReaction(OngoingEffect trigger, EffectContext context) {
        return inputHandler.promptYesNo(
                String.format("Use %s? (Reaction)", trigger.getName()));
    }
}
