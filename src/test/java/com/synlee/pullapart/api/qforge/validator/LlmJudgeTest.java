package com.synlee.pullapart.api.qforge.validator;

import com.synlee.qforge.validator.JudgeVerdict;
import com.synlee.qforge.validator.LlmJudgeService;
import com.synlee.qforge.validator.LlmJudgeService.PairwiseResult;
import com.synlee.qforge.validator.LlmValidatorService;
import com.synlee.qforge.validator.LlmValidatorService.LlmResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * QForge — Module 2: LLM-as-a-Judge Tests
 *
 * Uses Claude as an evaluator to grade response QUALITY — things the
 * deterministic checks in LlmValidatorTest cannot catch (a fluent answer
 * that contains the right keyword but is factually wrong).
 *
 * Covers:
 *   - Pointwise rubric scoring of a live Claude response
 *   - Judge calibration: a known-bad answer MUST fail (validates the judge itself)
 *   - Reference-based grading driven by a TestNG DataProvider
 *   - Groundedness / hallucination detection (RAG-style)
 *   - Pairwise comparison with an A/B position-bias check
 *   - Judge consistency across repeated runs
 */
public class LlmJudgeTest {

    private LlmValidatorService validatorService;   // system under test
    private LlmJudgeService judgeService;           // evaluator

    /** Minimum judge score (1–5) for a response to pass. */
    private static final int PASS_THRESHOLD = 4;

    @BeforeClass
    public void setUp() {
        System.out.println("[QForge] Initializing LlmJudgeTest...");
        validatorService = new LlmValidatorService();
        judgeService = new LlmJudgeService();
    }

    // ═══════════════════════════════════════════════════════════
    // Test 1 — Pointwise rubric scoring of a live response
    // ═══════════════════════════════════════════════════════════

    @Test(priority = 1, description = "Judge scores a live Claude answer as correct and relevant")
    public void testJudgePassesLiveResponse() {
        String question = "What does HTTP status code 404 mean? Answer in one or two sentences.";

        LlmResponse answer = validatorService.sendPrompt(question);

        // Layer 1: cheap deterministic check first
        Assert.assertTrue(answer.isNotEmpty(), "Response should not be empty");

        // Layer 2: semantic check by the judge
        JudgeVerdict verdict = judgeService.judge(
                question,
                answer.getText(),
                "1. Factually correct. 2. Directly answers the question. "
                        + "3. Contains no invented or misleading details. 4. Is concise (one or two sentences)."
        );

        Assert.assertTrue(verdict.meetsThreshold(PASS_THRESHOLD),
                "Judge failed the response (score " + verdict.getScore() + "): " + verdict.getReasoning());
    }

    // ═══════════════════════════════════════════════════════════
    // Test 2 — Judge calibration: known-bad answer must fail
    // ═══════════════════════════════════════════════════════════

    /**
     * Negative control. This answer would PASS the deterministic checks
     * (not empty, contains "404", reasonable length) but is factually wrong.
     * If the judge passes it, the judge is broken — not the system under test.
     */
    @Test(priority = 2, description = "Judge fails a fluent but factually wrong answer (negative control)")
    public void testJudgeFailsKnownBadResponse() {
        String question = "What does HTTP status code 404 mean? Answer in one or two sentences.";
        String wrongAnswer = "HTTP 404 means the server encountered an internal error while processing "
                + "the request, usually caused by a crash in the application code.";

        // Prove the gap: deterministic checks alone would let this through
        LlmResponse simulated = new LlmResponse(wrongAnswer, 0);
        Assert.assertTrue(simulated.isNotEmpty() && simulated.containsKeyword("404"),
                "Precondition: the bad answer passes keyword checks");

        JudgeVerdict verdict = judgeService.judge(
                question,
                wrongAnswer,
                "1. Factually correct. 2. Directly answers the question. 3. Contains no invented details."
        );

        Assert.assertFalse(verdict.isPass(),
                "Judge should have FAILED a wrong answer but passed it: " + verdict.getReasoning());
        Assert.assertTrue(verdict.getScore() <= 2,
                "Expected score <= 2 for a wrong answer, got " + verdict.getScore());
    }

    // ═══════════════════════════════════════════════════════════
    // Test 3 — Reference-based grading (data-driven)
    // ═══════════════════════════════════════════════════════════

    @DataProvider(name = "referenceCases")
    public Object[][] referenceCases() {
        return new Object[][]{
                {
                        "Which HTTP method is idempotent and used to fully replace a resource? Answer in one sentence.",
                        "PUT is the idempotent HTTP method used to replace a resource entirely."
                },
                {
                        "What is the difference between a 401 and a 403 status code? Answer in two sentences.",
                        "401 Unauthorized means the client is not authenticated. "
                                + "403 Forbidden means the client is authenticated but lacks permission."
                },
                {
                        "In TestNG, which annotation runs a method once before all tests in a class? Answer in one sentence.",
                        "The @BeforeClass annotation runs a method once before any test methods in the class."
                }
        };
    }

    @Test(priority = 3, dataProvider = "referenceCases",
            description = "Live answer is semantically equivalent to the golden reference answer")
    public void testResponseMatchesReference(String question, String referenceAnswer) {
        LlmResponse answer = validatorService.sendPrompt(question);

        JudgeVerdict verdict = judgeService.judgeAgainstReference(question, answer.getText(), referenceAnswer);

        Assert.assertTrue(verdict.meetsThreshold(PASS_THRESHOLD),
                "Answer did not match reference (score " + verdict.getScore() + "): " + verdict.getReasoning());
    }

    // ═══════════════════════════════════════════════════════════
    // Test 4 & 5 — Groundedness / hallucination detection
    // ═══════════════════════════════════════════════════════════

    private static final String STORE_CONTEXT = """
            Acme Auto Parts Return Policy (fictional test fixture):
            - Parts may be returned within 30 days of purchase for store credit.
            - A receipt is required for all returns.
            - Core charges are refunded when the old part is returned.
            - Electrical parts are exchange-only and cannot be returned for credit.
            """;

    private static final String STORE_QUESTION = "Can I return an alternator I bought last week?";

    @Test(priority = 4, description = "Judge passes an answer that is fully supported by the context")
    public void testGroundedResponsePasses() {
        // Generate the answer RAG-style: the system under test is told to use only the context
        LlmResponse answer = validatorService.sendPrompt(
                "Context:\n" + STORE_CONTEXT + "\nQuestion: " + STORE_QUESTION,
                "Answer using ONLY the provided context. If the context does not cover something, say so. "
                        + "Keep the answer to two or three sentences."
        );

        JudgeVerdict verdict = judgeService.judgeGroundedness(STORE_QUESTION, answer.getText(), STORE_CONTEXT);

        Assert.assertTrue(verdict.meetsThreshold(PASS_THRESHOLD),
                "Grounded answer was failed (score " + verdict.getScore() + "): " + verdict.getReasoning());
    }

    @Test(priority = 5, description = "Judge catches a hallucinated claim not supported by the context")
    public void testHallucinatedResponseFails() {
        String hallucinated = "Yes. Alternators can be returned within 90 days for a full cash refund, "
                + "and no receipt is needed if you paid by card.";

        JudgeVerdict verdict = judgeService.judgeGroundedness(STORE_QUESTION, hallucinated, STORE_CONTEXT);

        Assert.assertFalse(verdict.isPass(),
                "Judge should have caught the hallucination but passed it: " + verdict.getReasoning());
    }

    // ═══════════════════════════════════════════════════════════
    // Test 6 — Pairwise comparison with position-bias check
    // ═══════════════════════════════════════════════════════════

    @Test(priority = 6, description = "Judge prefers the better answer regardless of presentation order")
    public void testPairwisePrefersBetterAnswer() {
        String question = "Why should API tests avoid depending on each other's execution order?";
        String criteria = "Accuracy, clarity, and whether the answer explains the practical consequence.";

        String strongAnswer = "Order-dependent tests share state, so one failure cascades into misleading "
                + "failures elsewhere, and the suite breaks when tests run in parallel or are re-run individually. "
                + "Each test should set up and clean up its own data.";
        String weakAnswer = "Because it is a best practice and tests look cleaner that way.";

        PairwiseResult result = judgeService.comparePairwise(question, criteria, strongAnswer, weakAnswer);

        Assert.assertTrue(result.isConsistent(),
                "Judge showed position bias — winner changed when order was swapped: " + result);
        Assert.assertEquals(result.getWinner(), "A",
                "Judge should prefer the stronger answer (A): " + result);
    }

    // ═══════════════════════════════════════════════════════════
    // Test 7 — Judge consistency across repeated runs
    // ═══════════════════════════════════════════════════════════

    @Test(priority = 7, description = "Judge returns the same pass/fail decision across repeated runs")
    public void testJudgeIsConsistentAcrossRuns() {
        String question = "What does the HTTP GET method do? Answer in one sentence.";
        String answer = "GET retrieves a representation of a resource without modifying it.";
        String criteria = "Factually correct and directly answers the question.";

        int runs = 3;
        int passes = 0;
        for (int i = 0; i < runs; i++) {
            if (judgeService.judge(question, answer, criteria).isPass()) {
                passes++;
            }
        }

        Assert.assertEquals(passes, runs,
                "Judge should consistently pass a correct answer; passed " + passes + " of " + runs + " runs");
    }
}