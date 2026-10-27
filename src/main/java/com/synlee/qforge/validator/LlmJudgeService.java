package com.synlee.qforge.validator;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synlee.qforge.client.AnthropicClientProvider;

/**
 * QForge — Module 2: LLM-as-a-Judge Service
 *
 * Uses Claude as an impartial evaluator ("judge") to grade the quality of an
 * LLM response against a rubric. This complements LlmValidatorService:
 *
 *   LlmValidatorService → deterministic checks (not empty, keywords, length, latency)
 *   LlmJudgeService     → semantic checks (correct? relevant? grounded? better?)
 *
 * Supported techniques:
 *   1. Pointwise rubric scoring       — judge(question, response, criteria)
 *   2. Reference-based grading        — judgeAgainstReference(question, response, reference)
 *   3. Groundedness / hallucination   — judgeGroundedness(question, response, context)
 *   4. Pairwise comparison with
 *      position-bias check            — comparePairwise(question, criteria, a, b)
 *
 * Reliability measures:
 *   - temperature = 0 for more consistent verdicts
 *   - reasoning is produced BEFORE the score (chain-of-thought then verdict)
 *   - strict JSON output parsed with Jackson
 *   - pairwise comparisons are run twice with A/B order swapped
 *
 * Usage:
 *   LlmJudgeService judge = new LlmJudgeService();
 *   JudgeVerdict v = judge.judge(question, answer, "Factually correct and directly answers the question");
 *   assertTrue(v.meetsThreshold(4), v.getReasoning());
 */
public class LlmJudgeService {

    private static final int MAX_TOKENS = 1024;
    private static final double JUDGE_TEMPERATURE = 0.0;

    private static final String JUDGE_SYSTEM_PROMPT = """
            You are a strict, impartial evaluator of AI-generated responses.
            You grade ONLY against the criteria you are given.
            Do not reward length, confident tone, or formatting on their own.
            Any factual error or unsupported claim must lower the score.

            Scoring scale:
              5 = fully meets every criterion
              4 = meets the criteria with only trivial issues
              3 = partially meets the criteria; noticeable gaps
              2 = mostly fails the criteria
              1 = completely fails or is incorrect

            Set "pass" to true only if the score is 4 or 5.

            Think through the evaluation first, then give the verdict.
            Respond with ONLY a JSON object in this exact format
            (no markdown, no code fences, no text outside the JSON):
            {
              "reasoning": "step-by-step evaluation against each criterion",
              "score": 1-5,
              "pass": true | false
            }
            """;

    private static final String PAIRWISE_SYSTEM_PROMPT = """
            You are a strict, impartial evaluator comparing two AI-generated responses
            to the same question. Judge ONLY against the criteria you are given.
            Do not let the order the responses are presented in influence you.
            Do not prefer a response just because it is longer.

            Respond with ONLY a JSON object in this exact format
            (no markdown, no code fences, no text outside the JSON):
            {
              "reasoning": "comparison of both responses against the criteria",
              "winner": "A" | "B" | "TIE"
            }
            """;

    private final AnthropicClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmJudgeService() {
        this.client = AnthropicClientProvider.getClient();
    }

    // ═══════════════════════════════════════════════════════════
    // 1. Pointwise rubric scoring
    // ═══════════════════════════════════════════════════════════

    /**
     * Grades a single response against a rubric.
     *
     * @param question The original prompt/question given to the system under test
     * @param response The response produced by the system under test
     * @param criteria Plain-English rubric the judge should apply
     * @return JudgeVerdict with reasoning, score (1–5), and pass/fail
     */
    public JudgeVerdict judge(String question, String response, String criteria) {
        String prompt = """
                Evaluate the RESPONSE to the QUESTION using the CRITERIA.

                QUESTION:
                %s

                RESPONSE:
                %s

                CRITERIA:
                %s
                """.formatted(question, response, criteria);

        return runPointwise(prompt);
    }

    // ═══════════════════════════════════════════════════════════
    // 2. Reference-based grading
    // ═══════════════════════════════════════════════════════════

    /**
     * Grades a response for semantic equivalence to a known-good ("golden") answer.
     * Wording may differ — the meaning must match and nothing may contradict it.
     */
    public JudgeVerdict judgeAgainstReference(String question, String response, String referenceAnswer) {
        String prompt = """
                Evaluate whether the RESPONSE is semantically equivalent to the REFERENCE ANSWER
                for the QUESTION. Wording may differ. The RESPONSE passes if it conveys the same
                key facts as the REFERENCE ANSWER and contains nothing that contradicts it.

                QUESTION:
                %s

                REFERENCE ANSWER:
                %s

                RESPONSE:
                %s
                """.formatted(question, referenceAnswer, response);

        return runPointwise(prompt);
    }

    // ═══════════════════════════════════════════════════════════
    // 3. Groundedness / hallucination check (RAG-style)
    // ═══════════════════════════════════════════════════════════

    /**
     * Checks that every claim in the response is supported by the provided context.
     * This is the standard hallucination check for RAG (retrieval-augmented) systems.
     */
    public JudgeVerdict judgeGroundedness(String question, String response, String context) {
        String prompt = """
                Evaluate whether the RESPONSE is fully grounded in the CONTEXT.
                List each factual claim in the RESPONSE and check whether the CONTEXT supports it.
                Any claim not supported by the CONTEXT is a hallucination and means the
                RESPONSE must fail (score 2 or lower), even if the claim happens to be true
                in the real world.

                QUESTION:
                %s

                CONTEXT:
                %s

                RESPONSE:
                %s
                """.formatted(question, context, response);

        return runPointwise(prompt);
    }

    // ═══════════════════════════════════════════════════════════
    // 4. Pairwise comparison with position-bias check
    // ═══════════════════════════════════════════════════════════

    /**
     * Compares two responses and returns which one is better.
     *
     * The comparison is run twice — once as (A, B) and once as (B, A) — to detect
     * position bias. If the judge picks a different winner depending on order, the
     * result is marked inconsistent and the winner is reported as TIE.
     *
     * @return PairwiseResult whose winner is "A", "B", or "TIE" in terms of the original arguments
     */
    public PairwiseResult comparePairwise(String question, String criteria,
                                          String responseA, String responseB) {
        // Run 1: original order
        String firstWinner = runPairwise(question, criteria, responseA, responseB);

        // Run 2: swapped order — map the answer back to the original labels
        String swappedWinner = runPairwise(question, criteria, responseB, responseA);
        String secondWinner = switch (swappedWinner) {
            case "A" -> "B";
            case "B" -> "A";
            default -> "TIE";
        };

        boolean consistent = firstWinner.equals(secondWinner);
        String finalWinner = consistent ? firstWinner : "TIE";

        System.out.println("[QForge Judge] Pairwise: run1=" + firstWinner
                + ", run2(swapped)=" + secondWinner + ", consistent=" + consistent);

        return new PairwiseResult(finalWinner, firstWinner, secondWinner, consistent);
    }

    private String runPairwise(String question, String criteria, String first, String second) {
        String prompt = """
                Compare RESPONSE A and RESPONSE B to the QUESTION using the CRITERIA.

                QUESTION:
                %s

                CRITERIA:
                %s

                RESPONSE A:
                %s

                RESPONSE B:
                %s
                """.formatted(question, criteria, first, second);

        String raw = callJudge(PAIRWISE_SYSTEM_PROMPT, prompt);
        try {
            JsonNode node = mapper.readTree(extractJson(raw));
            String winner = node.path("winner").asText("TIE").trim().toUpperCase();
            return switch (winner) {
                case "A", "B" -> winner;
                default -> "TIE";
            };
        } catch (Exception e) {
            throw new JudgeParseException("Could not parse pairwise judge output: " + raw, e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Internal helpers
    // ═══════════════════════════════════════════════════════════

    private JudgeVerdict runPointwise(String prompt) {
        String raw = callJudge(JUDGE_SYSTEM_PROMPT, prompt);
        try {
            JudgeVerdict verdict = mapper.readValue(extractJson(raw), JudgeVerdict.class);
            verdict.setRawJudgeOutput(raw);
            System.out.println("[QForge Judge] " + verdict);
            return verdict;
        } catch (Exception e) {
            // Fail loudly: an unparseable verdict should never silently pass a test
            throw new JudgeParseException("Could not parse judge output: " + raw, e);
        }
    }

    /** Calls Claude as the judge with temperature 0. */
    private String callJudge(String systemPrompt, String userPrompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.CLAUDE_SONNET_4_6)
                .maxTokens(MAX_TOKENS)
                .temperature(JUDGE_TEMPERATURE)
                .system(systemPrompt)
                .addUserMessage(userPrompt)
                .build();

        Message message = client.messages().create(params);

        StringBuilder result = new StringBuilder();
        for (ContentBlock block : message.content()) {
            if (block.isText()) {
                result.append(block.asText().text());
            }
        }
        return result.toString().trim();
    }

    /**
     * Pulls the JSON object out of the judge output, tolerating stray
     * code fences or text before/after the object.
     */
    static String extractJson(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Judge output was null");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("No JSON object found in judge output");
        }
        return raw.substring(start, end + 1);
    }

    // ═══════════════════════════════════════════════════════════
    // Inner classes
    // ═══════════════════════════════════════════════════════════

    /** Result of a pairwise comparison, including the position-bias check. */
    public static class PairwiseResult {

        private final String winner;
        private final String firstRunWinner;
        private final String swappedRunWinner;
        private final boolean consistent;

        public PairwiseResult(String winner, String firstRunWinner,
                              String swappedRunWinner, boolean consistent) {
            this.winner = winner;
            this.firstRunWinner = firstRunWinner;
            this.swappedRunWinner = swappedRunWinner;
            this.consistent = consistent;
        }

        /** Final winner in terms of the original arguments: "A", "B", or "TIE". */
        public String getWinner() {
            return winner;
        }

        public String getFirstRunWinner() {
            return firstRunWinner;
        }

        public String getSwappedRunWinner() {
            return swappedRunWinner;
        }

        /** True if the judge picked the same winner in both orders (no position bias). */
        public boolean isConsistent() {
            return consistent;
        }

        @Override
        public String toString() {
            return "PairwiseResult{winner=" + winner + ", run1=" + firstRunWinner
                    + ", run2=" + swappedRunWinner + ", consistent=" + consistent + "}";
        }
    }

    /** Thrown when the judge returns output that cannot be parsed into a verdict. */
    public static class JudgeParseException extends RuntimeException {
        public JudgeParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
