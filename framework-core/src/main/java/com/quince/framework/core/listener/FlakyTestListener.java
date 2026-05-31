package com.quince.framework.core.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;


import org.testng.ITestResult;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * FlakyTestListener — detects and reports flaky tests across runs.
 *
 * <p>Strategy:
 * <ol>
 *   <li>On every test failure: appends a {@link FlakyEntry} to
 *       {@code target/flaky-history.json}</li>
 *   <li>On suite finish: {@link FlakyAnalyzer} reads history, flags tests
 *       that failed more than {@value FlakyAnalyzer#FLAKY_THRESHOLD} times
 *       in the last {@value FlakyAnalyzer#HISTORY_WINDOW} runs</li>
 *   <li>Flaky tests get an Allure {@code flaky} label and a warning
 *       attachment with root cause summary</li>
 *   <li>A human-readable summary is printed to console and written to
 *       {@code target/flaky-summary.txt}</li>
 * </ol>
 *
 * <p>All state is written to disk so history accumulates across multiple
 * local runs (just like a CI trend). Delete {@code target/flaky-history.json}
 * to reset.
 *
 * <p>Thread-safe: all in-memory state uses {@link ConcurrentHashMap} and
 * {@link Collections# synchronizedList}; file writes are {@code synchronized}.
 *
 * <p>Registration — add to your TestNG suite XML:
 * <pre>{@code
 * <listeners>
 *   <listener class-name="com.quince.framework.listeners.FlakyTestListener"/>
 * </listeners>
 * }</pre>
 */
public class FlakyTestListener implements ITestListener {

    private static final Logger logger = LogManager.getLogger(FlakyTestListener.class);

    /**
     * Path where flaky history is persisted across runs.
     */
    static final Path HISTORY_FILE = Paths.get("target", "flaky-history.json");

    /**
     * Path for the human-readable summary written after each suite.
     */
    static final Path SUMMARY_FILE = Paths.get("target", "flaky-summary.txt");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * In-memory list of entries recorded in THIS run.
     * Synchronized because TestNG may call listeners from multiple threads.
     */
    private final List<FlakyEntry> currentRunEntries =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * Track pass/fail counts per test in current run for retry detection.
     * Key: fully qualified test name.
     */
    private final Map<String, Integer> passCount = new ConcurrentHashMap<>();
    private final Map<String, Integer> failCount = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // ITestListener callbacks
    // -------------------------------------------------------------------------

    @Override
    public void onStart(ITestContext context) {
        logger.info("[FlakyListener] Suite started: {}", context.getName());
        ensureTargetDirectory();
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = getFullTestName(result);
        passCount.merge(testName, 1, Integer::sum);
        logger.debug("[FlakyListener] PASS — {}", testName);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        recordFailure(result, "FAILED");
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        recordFailure(result, "FAILED_WITH_SUCCESS_PERCENTAGE");
    }

    @Override
    public void onTestFailedWithTimeout(ITestResult result) {
        recordFailure(result, "TIMED_OUT");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        logger.debug("[FlakyListener] SKIPPED — {}", getFullTestName(result));
    }

    @Override
    public void onFinish(ITestContext context) {
        logger.info("[FlakyListener] Suite finished: {}. Persisting {} failure(s) from this run.",
                context.getName(), currentRunEntries.size());

        // 1. Persist this run's failures into the rolling history file
        persistCurrentRunEntries();

        // 2. Analyse full history and identify flaky tests
        FlakyAnalyzer analyzer = new FlakyAnalyzer();
        List<FlakyAnalyzer.FlakyTestReport> flakyReports = analyzer.analyze();

        // 3. Decorate Allure + print summary
        if (flakyReports.isEmpty()) {
            logger.info("[FlakyListener] No flaky tests detected in history.");
            writeSummary(Collections.emptyList(), context.getName());
        } else {
            logger.warn("[FlakyListener] {} flaky test(s) detected!", flakyReports.size());
            flakyReports.forEach(this::decorateAllure);
            writeSummary(flakyReports, context.getName());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Records a test failure — both in-memory and will be flushed to disk
     * in {@link #onFinish}.
     */
    private void recordFailure(ITestResult result, String failureType) {
        String testName = getFullTestName(result);
        failCount.merge(testName, 1, Integer::sum);

        String errorMessage = extractErrorMessage(result);
        String stackTrace = extractStackTrace(result);
        long durationMs = result.getEndMillis() - result.getStartMillis();

        FlakyEntry entry = new FlakyEntry(
                testName,
                result.getMethod().getRealClass().getSimpleName(),
                result.getMethod().getMethodName(),
                failureType,
                errorMessage,
                stackTrace,
                Instant.now().toEpochMilli(),
                LocalDateTime.now().format(TS_FMT),
                durationMs,
                getGroups(result),
                getParameters(result)
        );

        currentRunEntries.add(entry);
        logger.warn("[FlakyListener] {} — {} | Error: {}", failureType, testName, errorMessage);
    }

    /**
     * Appends this run's entries into the rolling JSON history file.
     * Thread-safe via synchronized block on file path.
     */
    private synchronized void persistCurrentRunEntries() {
        if (currentRunEntries.isEmpty()) {
            return;
        }
        try {
            List<FlakyEntry> existing = loadHistory();
            existing.addAll(currentRunEntries);
            MAPPER.writeValue(HISTORY_FILE.toFile(), existing);
            logger.debug("[FlakyListener] Persisted {} total entries to {}",
                    existing.size(), HISTORY_FILE);
        } catch (IOException e) {
            logger.error("[FlakyListener] Failed to persist flaky history", e);
        }
    }

    /**
     * Loads all historical entries from disk.
     * Returns empty list if file doesn't exist yet.
     */
    @SuppressWarnings("unchecked")
    static List<FlakyEntry> loadHistory() {
        if (!Files.exists(HISTORY_FILE)) {
            return new ArrayList<>();
        }
        try {
            List<?> raw = MAPPER.readValue(HISTORY_FILE.toFile(), List.class);
            // Re-deserialize as typed list
            List<FlakyEntry> entries = new ArrayList<>();
            for (Object obj : raw) {
                FlakyEntry e = MAPPER.convertValue(obj, FlakyEntry.class);
                entries.add(e);
            }
            return entries;
        } catch (IOException e) {
            logger.warn("[FlakyListener] Could not read history file — starting fresh", e);
            return new ArrayList<>();
        }
    }

    /**
     * Adds Allure labels and a warning attachment to flag a flaky test.
     * Allure picks up labels if the test is still in the current run's context.
     */
    private void decorateAllure(FlakyAnalyzer.FlakyTestReport report) {
        try {
            // Add "flaky" label — shows in Allure as a tag
            Allure.label("flaky", "true");
            Allure.label("flaky_rate",
                    String.format("%.0f%%", report.failureRate() * 100));

            // Attach a flaky warning text block
            String attachment = buildAllureAttachment(report);
            Allure.addAttachment(
                    "⚠️ Flaky Test Report — " + report.testName(),
                    "text/plain",
                    attachment,
                    ".txt"
            );

            logger.warn("[FlakyListener] Marked as flaky in Allure: {} (rate: {:.0f}%)",
                    report.testName(), report.failureRate() * 100);
        } catch (Exception e) {
            // Never let Allure decoration break the test run
            logger.warn("[FlakyListener] Could not decorate Allure for {}: {}",
                    report.testName(), e.getMessage());
        }
    }

    private String buildAllureAttachment(FlakyAnalyzer.FlakyTestReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("  ⚠️  FLAKY TEST DETECTED\n");
        sb.append("═══════════════════════════════════════════\n");
        sb.append("Test     : ").append(report.testName()).append("\n");
        sb.append("Failures : ").append(report.failureCount())
                .append(" / ").append(report.totalRuns()).append(" runs\n");
        sb.append("Rate     : ")
                .append(String.format("%.0f%%", report.failureRate() * 100)).append("\n");
        sb.append("First seen: ").append(report.firstSeen()).append("\n");
        sb.append("Last seen : ").append(report.lastSeen()).append("\n");
        sb.append("\nTop Error Messages:\n");
        report.topErrors().forEach(err ->
                sb.append("  • ").append(err).append("\n"));
        sb.append("\nRecommendation: ").append(report.recommendation()).append("\n");
        sb.append("═══════════════════════════════════════════\n");
        return sb.toString();
    }

    /**
     * Writes a console-friendly summary and persists it to
     * {@code target/flaky-summary.txt}.
     */
    private void writeSummary(List<FlakyAnalyzer.FlakyTestReport> reports, String suiteName) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════╗\n");
        sb.append("║           FLAKY TEST ANALYSIS SUMMARY            ║\n");
        sb.append("╚══════════════════════════════════════════════════╝\n");
        sb.append("Suite  : ").append(suiteName).append("\n");
        sb.append("Run at : ").append(LocalDateTime.now().format(TS_FMT)).append("\n");
        sb.append("─────────────────────────────────────────────────────\n");

        if (reports.isEmpty()) {
            sb.append("✅  No flaky tests detected.\n");
        } else {
            sb.append(String.format("⚠️  %d flaky test(s) found:\n\n", reports.size()));
            for (int i = 0; i < reports.size(); i++) {
                FlakyAnalyzer.FlakyTestReport r = reports.get(i);
                sb.append(String.format("  %d. %s\n", i + 1, r.testName()));
                sb.append(String.format("     Failures : %d / %d runs (%.0f%%)\n",
                        r.failureCount(), r.totalRuns(), r.failureRate() * 100));
                sb.append(String.format("     Last seen: %s\n", r.lastSeen()));
                sb.append(String.format("     Fix hint : %s\n", r.recommendation()));
                sb.append("\n");
            }
        }
        sb.append("─────────────────────────────────────────────────────\n");
        sb.append("Full history: ").append(HISTORY_FILE.toAbsolutePath()).append("\n");

        String summary = sb.toString();

        // Print to console
        logger.info("\n{}", summary);

        // Persist to file
        try {
            Files.writeString(SUMMARY_FILE, summary, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("[FlakyListener] Summary written to {}", SUMMARY_FILE);
        } catch (IOException e) {
            logger.warn("[FlakyListener] Could not write summary file", e);
        }
    }

    // -------------------------------------------------------------------------
    // Utility methods
    // -------------------------------------------------------------------------

    private static String getFullTestName(ITestResult result) {
        return result.getMethod().getRealClass().getName()
                + "#" + result.getMethod().getMethodName();
    }

    private static String extractErrorMessage(ITestResult result) {
        Throwable t = result.getThrowable();
        if (t == null) return "No exception captured";
        String msg = t.getMessage();
        return (msg != null && !msg.isBlank())
                ? msg.lines().findFirst().orElse(msg).trim()
                : t.getClass().getSimpleName();
    }

    private static String extractStackTrace(ITestResult result) {
        Throwable t = result.getThrowable();
        if (t == null) return "";
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        // Trim to first 20 lines to keep history file manageable
        return sw.toString().lines()
                .limit(20)
                .collect(Collectors.joining("\n"));
    }

    private static List<String> getGroups(ITestResult result) {
        String[] groups = result.getMethod().getGroups();
        return groups != null ? Arrays.asList(groups) : Collections.emptyList();
    }

    private static String getParameters(ITestResult result) {
        Object[] params = result.getParameters();
        if (params == null || params.length == 0) return "";
        return Arrays.stream(params)
                .map(p -> p != null ? p.toString() : "null")
                .collect(Collectors.joining(", "));
    }

    private static void ensureTargetDirectory() {
        try {
            Files.createDirectories(HISTORY_FILE.getParent());
        } catch (IOException e) {
            logger.warn("[FlakyListener] Could not create target directory", e);
        }
    }

    // =========================================================================
    // Inner classes
    // =========================================================================

    /**
     * Immutable data record for a single test failure event.
     * Serialized to / deserialized from {@code flaky-history.json}.
     */
    public static class FlakyEntry {
        public String testName;
        public String className;
        public String methodName;
        public String failureType;   // FAILED | TIMED_OUT | FAILED_WITH_SUCCESS_PERCENTAGE
        public String errorMessage;
        public String stackTrace;
        public long timestampEpoch;
        public String timestampHuman;
        public long durationMs;
        public List<String> groups;
        public String parameters;

        /**
         * Required by Jackson for deserialization.
         */
        public FlakyEntry() {
        }

        public FlakyEntry(String testName, String className, String methodName,
                          String failureType, String errorMessage, String stackTrace,
                          long timestampEpoch, String timestampHuman, long durationMs,
                          List<String> groups, String parameters) {
            this.testName = testName;
            this.className = className;
            this.methodName = methodName;
            this.failureType = failureType;
            this.errorMessage = errorMessage;
            this.stackTrace = stackTrace;
            this.timestampEpoch = timestampEpoch;
            this.timestampHuman = timestampHuman;
            this.durationMs = durationMs;
            this.groups = groups;
            this.parameters = parameters;
        }
    }

    // =========================================================================

    /**
     * Analyses the rolling {@code flaky-history.json} and identifies
     * tests that exceed the failure threshold.
     *
     * <p>Algorithm:
     * <ul>
     *   <li>Consider the last {@value #HISTORY_WINDOW} recorded runs per test</li>
     *   <li>If failures exceed {@value #FLAKY_THRESHOLD} → mark as flaky</li>
     *   <li>Generate a human-readable recommendation based on failure patterns</li>
     * </ul>
     */
    public static class FlakyAnalyzer {

        /**
         * How many of the most recent entries to consider per test.
         */
        static final int HISTORY_WINDOW = 5;

        /**
         * Minimum failures within the window to be considered flaky.
         */
        static final int FLAKY_THRESHOLD = 2;

        private static final Logger log = LogManager.getLogger(FlakyAnalyzer.class);

        /**
         * Reads history file and returns a report for every test that
         * exceeds the flaky threshold.
         *
         * @return list of flaky reports, empty if none detected
         */
        public List<FlakyTestReport> analyze() {
            List<FlakyEntry> history = loadHistory();

            if (history.isEmpty()) {
                log.debug("[FlakyAnalyzer] No history found — nothing to analyze");
                return Collections.emptyList();
            }

            // Group entries by test name, sorted oldest→newest
            Map<String, List<FlakyEntry>> byTest = history.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.testName,
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> {
                                        list.sort(Comparator.comparingLong(e -> e.timestampEpoch));
                                        return list;
                                    }
                            )
                    ));

            List<FlakyTestReport> flakyReports = new ArrayList<>();

            for (Map.Entry<String, List<FlakyEntry>> entry : byTest.entrySet()) {
                String testName = entry.getKey();
                List<FlakyEntry> entries = entry.getValue();

                // Take only the last HISTORY_WINDOW entries
                List<FlakyEntry> window = entries.size() > HISTORY_WINDOW
                        ? entries.subList(entries.size() - HISTORY_WINDOW, entries.size())
                        : entries;

                int failuresInWindow = window.size(); // every entry IS a failure
                int totalRuns = HISTORY_WINDOW; // assume window size = total runs

                if (failuresInWindow >= FLAKY_THRESHOLD) {
                    double failureRate = (double) failuresInWindow / totalRuns;

                    // Collect unique top error messages
                    List<String> topErrors = window.stream()
                            .map(e -> e.errorMessage)
                            .distinct()
                            .limit(3)
                            .collect(Collectors.toList());

                    String firstSeen = window.get(0).timestampHuman;
                    String lastSeen = window.get(window.size() - 1).timestampHuman;

                    String recommendation = generateRecommendation(window);

                    flakyReports.add(new FlakyTestReport(
                            testName, failuresInWindow, totalRuns,
                            failureRate, firstSeen, lastSeen,
                            topErrors, recommendation
                    ));

                    log.warn("[FlakyAnalyzer] Flaky: {} ({}/{} failures, {:.0f}%)",
                            testName, failuresInWindow, totalRuns, failureRate * 100);
                }
            }

            // Sort by failure rate descending — worst offenders first
            flakyReports.sort(Comparator.comparingDouble(FlakyTestReport::failureRate).reversed());
            return flakyReports;
        }

        /**
         * Generates a contextual fix recommendation based on failure patterns.
         */
        private String generateRecommendation(List<FlakyEntry> entries) {
            long timeouts = entries.stream()
                    .filter(e -> "TIMED_OUT".equals(e.failureType)
                            || (e.errorMessage != null && e.errorMessage.toLowerCase()
                            .contains("timeout")))
                    .count();

            long noSuchElement = entries.stream()
                    .filter(e -> e.errorMessage != null && (
                            e.errorMessage.toLowerCase().contains("nosuchelement") ||
                                    e.errorMessage.toLowerCase().contains("no such element") ||
                                    e.errorMessage.toLowerCase().contains("unable to locate")))
                    .count();

            long staleElement = entries.stream()
                    .filter(e -> e.errorMessage != null &&
                            e.errorMessage.toLowerCase().contains("stale"))
                    .count();

            long assertionErrors = entries.stream()
                    .filter(e -> e.errorMessage != null && (
                            e.errorMessage.toLowerCase().contains("assert") ||
                                    e.errorMessage.toLowerCase().contains("expected") ||
                                    e.errorMessage.toLowerCase().contains("but was")))
                    .count();

            // Return most likely root cause
            if (timeouts > entries.size() / 2) {
                return "Likely timing issue — increase explicit wait or check network latency";
            } else if (noSuchElement > entries.size() / 2) {
                return "Locator instability — enable Healenium healing or update locator strategy";
            } else if (staleElement > entries.size() / 2) {
                return "Stale element — add retry logic or re-fetch element before interaction";
            } else if (assertionErrors > entries.size() / 2) {
                return "Data/state dependency — isolate test data; check experiment variant pinning";
            } else {
                return "Mixed failures — review test isolation, parallel thread safety, and waits";
            }
        }

        // ---------------------------------------------------------------------
        // FlakyTestReport record
        // ---------------------------------------------------------------------

        /**
         * Immutable report for a single flaky test.
         *
         * @param testName       fully qualified test name (ClassName#methodName)
         * @param failureCount   number of failures in the history window
         * @param totalRuns      total runs considered in the window
         * @param failureRate    0.0–1.0
         * @param firstSeen      human-readable timestamp of earliest failure
         * @param lastSeen       human-readable timestamp of most recent failure
         * @param topErrors      up to 3 distinct error messages
         * @param recommendation contextual fix suggestion
         */
        public record FlakyTestReport(
                String testName,
                int failureCount,
                int totalRuns,
                double failureRate,
                String firstSeen,
                String lastSeen,
                List<String> topErrors,
                String recommendation
        ) {
        }
    }
}