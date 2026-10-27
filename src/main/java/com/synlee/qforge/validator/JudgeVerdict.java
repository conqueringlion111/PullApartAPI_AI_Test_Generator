package com.synlee.qforge.validator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * QForge — Module 2: LLM-as-a-Judge Verdict
 *
 * Structured result returned by the judge model after grading a response.
 * Deserialized from the judge's JSON output with Jackson.
 *
 * Example JSON:
 * {
 *   "reasoning": "The response correctly states that 404 means Not Found...",
 *   "score": 5,
 *   "pass": true
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JudgeVerdict {

    private String reasoning;
    private int score;
    private boolean pass;

    /** Raw judge output — kept for debugging and reporting. Not part of the JSON. */
    private String rawJudgeOutput;

    public JudgeVerdict() {
        // Required by Jackson
    }

    public JudgeVerdict(String reasoning, int score, boolean pass) {
        this.reasoning = reasoning;
        this.score = score;
        this.pass = pass;
    }

    /** The judge's step-by-step explanation, written before the score. */
    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    /** Score on a 1–5 scale (5 = fully meets every criterion). */
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    /** The judge's overall pass/fail decision against the criteria. */
    public boolean isPass() {
        return pass;
    }

    public void setPass(boolean pass) {
        this.pass = pass;
    }

    public String getRawJudgeOutput() {
        return rawJudgeOutput;
    }

    public void setRawJudgeOutput(String rawJudgeOutput) {
        this.rawJudgeOutput = rawJudgeOutput;
    }

    /** Convenience check used by tests: judge passed it AND the score meets the threshold. */
    public boolean meetsThreshold(int minScore) {
        return pass && score >= minScore;
    }

    @Override
    public String toString() {
        return "JudgeVerdict{score=" + score + ", pass=" + pass + ", reasoning='" + reasoning + "'}";
    }
}