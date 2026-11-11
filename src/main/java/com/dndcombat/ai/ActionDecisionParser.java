package com.dndcombat.ai;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ActionDecisionParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public ActionDecision parse(String response) {
        try {
            // Handle markdown code fences if Qwen includes them.
            String json = response.trim();

            if (json.startsWith("```")) {
                json = json.replaceFirst("^```(?:json)?\\s*", "");
                json = json.replaceFirst("\\s*```$", "");
            }

            return mapper.readValue(json, ActionDecision.class);

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not parse Qwen's decision: " + response, e);
        }
    }
}