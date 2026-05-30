package com.synlee.qforge.agent;

import com.synlee.qforge.agent.FailureAnalyzerService.FailureAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QForge — AI Agent: Test Runner Agent
 *
 * The central coordinator of the QForge AI Agent. It:
 *   1. Runs the Maven test suite as a subprocess
 *   2. Captures and parses the output for failures
 *   3. Reads testng-results.xml for detailed failure info
 *   4. Sends each failure to FailureAnalyzerService for Claude's analysis
 *   5. Collects all analyses into an AgentRunResult for reporting
 *
 * Usage:
 *   TestRunnerAgent agent = new TestRunnerAgent("/path/to/project");
 *   AgentRunResult result = agent.runAndAnalyze();
 *   System.out.println(result.getSummary());
 */
public class TestRunnerAgent {

    private final String projectPath;
    private final FailureAnalyzerService failureAnalyzer;

    // Surefire reports location relative to project root
    private static final String SUREFIRE_REPORTS_PATH = "target/surefire-reports";

    public TestRunnerAgent(String projectPath) {
        this.projectPath = projectPath;
        this.failureAnalyzer = new FailureAnalyzerService();
        System.out.println("[QForge Agent] Initialized for project: " + projectPath);
    }

    /**
     * Runs the full Maven test suite and analyzes any failures with Claude.
     */
    public AgentRunResult runAndAnalyze() {
        System.out.println("\n[QForge Agent] ═══════════════════════════════════════");
        System.out.println("[QForge Agent] Starting AI-powered test run...");
        System.out.println("[QForge Agent] ═══════════════════════════════════════");

        AgentRunResult result = new AgentRunResult();
        long startTime = System.currentTimeMillis();

        System.out.println("\n[QForge Agent] Step 1: Running Maven test suite...");
        MavenRunOutput mavenOutput = runMavenTests();
        result.setMavenOutput(mavenOutput);

        System.out.println("[QForge Agent] Maven exit code: " + mavenOutput.getExitCode());
        System.out.println("[QForge Agent] Tests run: " + mavenOutput.getTestsRun());
        System.out.println("[QForge Agent] Failures:   " + mavenOutput.getFailureCount());

        if (mavenOutput.getFailureCount() > 0) {
            System.out.println("\n[QForge Agent] Step 2: Reading Surefire failure reports...");
            List<TestFailure> failures = readSurefireReports();
            result.setFailures(failures);
            System.out.println("[QForge Agent] Found " + failures.size() + " failure(s) to analyze.");

            System.out.println("\n[QForge Agent] Step 3: Sending failures to Claude for analysis...");
            List<FailureAnalysis> analyses = new ArrayList<>();
            for (TestFailure failure : failures) {
                System.out.println("[QForge Agent] Analyzing: " + failure.getTestName());
                FailureAnalysis analysis = failureAnalyzer.analyze(
                        failure.getTestName(),
                        failure.getErrorMessage(),
                        failure.getStackTrace(),
                        failure.getTestCode()
                );
                analyses.add(analysis);
                System.out.println(analysis);
            }
            result.setAnalyses(analyses);

        } else {
            System.out.println("\n[QForge Agent] No failures detected — all tests passed!");
        }

        long totalTime = System.currentTimeMillis() - startTime;
        result.setTotalTimeMs(totalTime);
        System.out.println("\n[QForge Agent] Agent run complete in " + totalTime + "ms");
        return result;
    }

    /**
     * Runs 'mvn test' as a subprocess and captures the output.
     */
    private MavenRunOutput runMavenTests() {
        MavenRunOutput output = new MavenRunOutput();
        StringBuilder fullOutput = new StringBuilder();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    getMavenCommand(), "test", "-Dsurefire.failIfNoSpecifiedTests=false"
            );
            pb.directory(new File(projectPath));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fullOutput.append(line).append("\n");
                    if (line.contains("Tests run:")) {
                        parseMavenTestLine(line, output);
                    }
                }
            }

            output.setExitCode(process.waitFor());
            output.setFullOutput(fullOutput.toString());

        } catch (Exception e) {
            System.err.println("[QForge Agent] Error running Maven: " + e.getMessage());
            output.setExitCode(-1);
            output.setFullOutput("Error: " + e.getMessage());
        }

        return output;
    }

    /**
     * Parses a Maven "Tests run: X, Failures: Y" line to extract counts.
     */
    private void parseMavenTestLine(String line, MavenRunOutput output) {
        try {
            Pattern pattern = Pattern.compile(
                    "Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)"
            );
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                output.setTestsRun(output.getTestsRun() + Integer.parseInt(matcher.group(1)));
                output.setFailureCount(output.getFailureCount() + Integer.parseInt(matcher.group(2)));
                output.setErrorCount(output.getErrorCount() + Integer.parseInt(matcher.group(3)));
                output.setSkippedCount(output.getSkippedCount() + Integer.parseInt(matcher.group(4)));
            }
        } catch (Exception e) {
            // Non-critical parsing error — continue
        }
    }

    /**
     * Reads testng-results.xml from the Surefire reports directory
     * and extracts failure details for each failed test.
     */
    private List<TestFailure> readSurefireReports() {
        List<TestFailure> failures = new ArrayList<>();
        Path resultsFile = Paths.get(projectPath, SUREFIRE_REPORTS_PATH, "testng-results.xml");

        if (!Files.exists(resultsFile)) {
            System.err.println("[QForge Agent] testng-results.xml not found at: " + resultsFile);
            return failures;
        }

        try {
            String content = Files.readString(resultsFile);
            failures = parseTestNgResultsXml(content);
            System.out.println("[QForge Agent] Parsed testng-results.xml — found "
                    + failures.size() + " failure(s)");
        } catch (Exception e) {
            System.err.println("[QForge Agent] Error reading testng-results.xml: " + e.getMessage());
        }

        return failures;
    }

    /**
     * Parses testng-results.xml and extracts all FAIL test methods.
     */
    private List<TestFailure> parseTestNgResultsXml(String xmlContent) {
        List<TestFailure> failures = new ArrayList<>();

        String searchToken = "status=\"FAIL\"";
        int searchStart = 0;

        while (true) {
            int failIndex = xmlContent.indexOf(searchToken, searchStart);
            if (failIndex == -1) break;

            int blockStart = xmlContent.lastIndexOf("<test-method", failIndex);
            if (blockStart == -1) {
                searchStart = failIndex + searchToken.length();
                continue;
            }

            int blockEnd = xmlContent.indexOf("</test-method>", failIndex);
            if (blockEnd == -1) {
                searchStart = failIndex + searchToken.length();
                continue;
            }

            String block = xmlContent.substring(blockStart, blockEnd + 14);

            String testName = extractXmlAttribute(block, "name");
            String className = extractEnclosingClassName(xmlContent, blockStart);
            String fullTestName = className.isEmpty() ? testName : className + "." + testName;

            String errorMessage = extractCdata(block, "<message>", "</message>");
            String stackTrace = extractCdata(block, "<full-stacktrace>", "</full-stacktrace>");

            if (stackTrace.length() > 1500) {
                stackTrace = stackTrace.substring(0, 1500) + "\n... (truncated)";
            }

            if (!testName.isEmpty() && !errorMessage.isEmpty()) {
                failures.add(new TestFailure(fullTestName, errorMessage, stackTrace, null));
                System.out.println("[QForge Agent] Found failure: " + fullTestName);
            }

            searchStart = blockEnd + 14;
        }

        return failures;
    }

    /**
     * Extracts the value of an XML attribute from a tag block.
     */
    private String extractXmlAttribute(String block, String attributeName) {
        String search = attributeName + "=\"";
        int start = block.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = block.indexOf("\"", start);
        if (end == -1) return "";
        return block.substring(start, end);
    }

    /**
     * Extracts CDATA content between two XML tags.
     */
    private String extractCdata(String block, String openTag, String closeTag) {
        int start = block.indexOf(openTag);
        if (start == -1) return "";
        start += openTag.length();

        int cdataStart = block.indexOf("<![CDATA[", start);
        int closeIndex = block.indexOf(closeTag, start);
        if (closeIndex == -1) return "";

        if (cdataStart != -1 && cdataStart < closeIndex) {
            start = cdataStart + 9;
            int cdataEnd = block.indexOf("]]>", start);
            if (cdataEnd != -1) {
                return block.substring(start, cdataEnd).trim();
            }
        }

        return block.substring(start, closeIndex).trim();
    }

    /**
     * Finds the class name of the enclosing <class> element
     * for a given test-method position in the XML.
     */
    private String extractEnclosingClassName(String xmlContent, int testMethodPosition) {
        int classTagIndex = xmlContent.lastIndexOf("<class name=\"", testMethodPosition);
        if (classTagIndex == -1) return "";
        return extractXmlAttribute(xmlContent.substring(classTagIndex), "name");
    }

    /**
     * Returns the correct Maven command for the current OS.
     */
    private String getMavenCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("windows") ? "mvn.cmd" : "mvn";
    }

    // ═══════════════════════════════════════════════════════════
    // Inner class: TestFailure
    // ═══════════════════════════════════════════════════════════

    public static class TestFailure {
        private final String testName;
        private final String errorMessage;
        private final String stackTrace;
        private final String testCode;

        public TestFailure(String testName, String errorMessage,
                           String stackTrace, String testCode) {
            this.testName = testName;
            this.errorMessage = errorMessage;
            this.stackTrace = stackTrace;
            this.testCode = testCode;
        }

        public String getTestName()     { return testName; }
        public String getErrorMessage() { return errorMessage; }
        public String getStackTrace()   { return stackTrace; }
        public String getTestCode()     { return testCode; }
    }

    // ═══════════════════════════════════════════════════════════
    // Inner class: MavenRunOutput
    // ═══════════════════════════════════════════════════════════

    public static class MavenRunOutput {
        private int exitCode;
        private int testsRun;
        private int failureCount;
        private int errorCount;
        private int skippedCount;
        private String fullOutput;

        public int getExitCode()       { return exitCode; }
        public int getTestsRun()       { return testsRun; }
        public int getFailureCount()   { return failureCount; }
        public int getErrorCount()     { return errorCount; }
        public int getSkippedCount()   { return skippedCount; }
        public String getFullOutput()  { return fullOutput; }

        public void setExitCode(int exitCode)         { this.exitCode = exitCode; }
        public void setTestsRun(int testsRun)         { this.testsRun = testsRun; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public void setErrorCount(int errorCount)     { this.errorCount = errorCount; }
        public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }
        public void setFullOutput(String fullOutput)  { this.fullOutput = fullOutput; }
    }

    // ═══════════════════════════════════════════════════════════
    // Inner class: AgentRunResult
    // ═══════════════════════════════════════════════════════════

    public static class AgentRunResult {
        private MavenRunOutput mavenOutput;
        private List<TestFailure> failures = new ArrayList<>();
        private List<FailureAnalysis> analyses = new ArrayList<>();
        private long totalTimeMs;

        public MavenRunOutput getMavenOutput()       { return mavenOutput; }
        public List<TestFailure> getFailures()       { return failures; }
        public List<FailureAnalysis> getAnalyses()   { return analyses; }
        public long getTotalTimeMs()                 { return totalTimeMs; }

        public void setMavenOutput(MavenRunOutput mavenOutput) { this.mavenOutput = mavenOutput; }
        public void setFailures(List<TestFailure> failures)     { this.failures = failures; }
        public void setAnalyses(List<FailureAnalysis> analyses) { this.analyses = analyses; }
        public void setTotalTimeMs(long totalTimeMs)            { this.totalTimeMs = totalTimeMs; }

        public boolean hasFailures() {
            return failures != null && !failures.isEmpty();
        }

        public int getHighConfidenceFixCount() {
            if (analyses == null) return 0;
            return (int) analyses.stream()
                    .filter(FailureAnalysis::isHighConfidence)
                    .count();
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n╔══════════════════════════════════════════════════════");
            sb.append("\n║         QFORGE AI AGENT — RUN SUMMARY               ");
            sb.append("\n╠══════════════════════════════════════════════════════");

            if (mavenOutput != null) {
                sb.append("\n║ Tests Run    : ").append(mavenOutput.getTestsRun());
                sb.append("\n║ Failures     : ").append(mavenOutput.getFailureCount());
                sb.append("\n║ Errors       : ").append(mavenOutput.getErrorCount());
                sb.append("\n║ Skipped      : ").append(mavenOutput.getSkippedCount());
            }

            sb.append("\n║ Total Time   : ").append(totalTimeMs).append("ms");
            sb.append("\n╠══════════════════════════════════════════════════════");

            if (analyses != null && !analyses.isEmpty()) {
                sb.append("\n║ AI ANALYSES  : ").append(analyses.size()).append(" failure(s) analyzed");
                sb.append("\n║ High Confidence Fixes: ").append(getHighConfidenceFixCount());
                sb.append("\n╠══════════════════════════════════════════════════════");
                for (FailureAnalysis analysis : analyses) {
                    sb.append("\n║ ► ").append(analysis.getTestName());
                    sb.append("\n║   Root Cause : ").append(analysis.getRootCause());
                    sb.append("\n║   Fix        : ").append(analysis.getFixSuggestion());
                    sb.append("\n║   Confidence : ").append(analysis.getConfidence());
                    sb.append("\n║   Human Review: ").append(analysis.requiresHumanReview());
                    sb.append("\n║");
                }
            } else {
                sb.append("\n║ All tests passed — no AI analysis needed!");
            }

            sb.append("\n╚══════════════════════════════════════════════════════");
            return sb.toString();
        }
    }
}
