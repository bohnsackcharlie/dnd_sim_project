package com.dndcombat.ai;

public class CombatPromptBuilder {

    // TODO, we can integrate the prompt and information better
    public String buildPrompt(String snapshotJson) {
        return """
                You are controlling a creature in a Dungeons & Dragons combat encounter.

                Your job is to choose the best available action and its target(s).

                Rules:
                - You can only choose actions listed in availableActions.
                - You can only target creatures whose IDs appear in the chosen action's legalTargetIds.
                - You must use the exact action name provided.
                - You must use the exact creature IDs provided.
                - Do not invent actions or targets.
                - Choose exactly one action.
                - Provide a brief explanation for your decision.

                Return your decision as JSON using this exact structure:
                {
                  "actionName": "exact action name",
                  "targetIds": [1],
                  "reasoning": "Brief explanation"
                }

                Return only valid JSON. Do not include markdown formatting or additional text.

                Combat state:
                %s
                """.formatted(snapshotJson);
    }
}