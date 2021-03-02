/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.idp.tests.aforeport;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * <p>This class allows to create requirement coverage statistics out of polarion requirements (AKA afos),
 * Junit test cases and test result xml files or Serenity bdd test result json files.
 * <p>
 * JUnit Test cases must be annotated with Test and Afo annotation. The value in the Afo annotation must be referencing
 * the AFO-ID custom field of the polarion work item.
 * <p>
 * For Bdd the scenarios must be annotated with @Afo:Afo-ID</p>
 *
 * <p>Command line parameters:
 * <ul>
 *     <li>-f: file containing requirements</li>
 *     <li>-tr: root folder for test source code, to parse for test methods. Can be used multiple times for multiple folders</li>
 *     <li>-rr: root folder for test result files, to parse the results from. Can be used multiple times for multiple folders</li>
 *     <li>-bdd: root folder for Serenity test result files, to parse the results from. Can be used multiple times for multiple folders</li>
 *     <li>-tpl: folder containing html template files to be used when generating the report</li>
 * </ul>
 * <p>
 * Presence of bdd argument overrules tr or rr parameters (you can only parse Serenity OR Junit)
 *
 * <p><b>Class dependencies</b></p>
 *
 * <img src="doc-files/aforeporter.svg" style="max-width:100%;" alt="class dependency">
 *
 * <p><b>Workflow of the program</b></p>
 *
 * <img src="doc-files/workflow.svg" style="max-width:100%;" alt="workflow">
 *
 * @see <a href="https://llg.cubic.org/docs/junit/">Junit test result xml declaration</a>
 */
@Slf4j
public class AfoReporter {

    /**
     * list of folders to parse for Serenity test result files.
     */
    @Parameter(names = {"-bdd", "-b"})
    List<String> bdd = Collections.singletonList(
        Paths.get("..", "idp-global", "idp-testsuite", "target", "site", "serenity").toAbsolutePath().toString());
    /**
     * name of the file containing the requirements.
     */
    @Parameter(names = {"-file", "-f"})
    String afofile = "requirements.json";
    /**
     * Folder containing the HTML template files used to generate the report.
     */
    @Parameter(names = {"-templates", "-tpl"})
    String templatesFolder = null;
    /**
     * list of folders to parse for Java files containing JUnit tests.
     */
    @Parameter(names = {"-testroot", "-tr"})
    List<String> testRoot = Collections
        .singletonList(Paths.get("..", "idp-global", "idp-server", "src", "test").toAbsolutePath().toString());
    /**
     * list of folders to parse for JUnit XML test result files.
     */
    @Parameter(names = {"-resultroot", "-rr"})
    List<String> resultRoot = Collections
        .singletonList(
            Paths.get("..", "idp-global", "idp-server", "target", "surefire-reports").toAbsolutePath().toString());

    /**
     * memorizes any exception happening in any of the threads so that we can abort execution in the main thread if
     * anything happened.
     */
    private Exception threadException = null;

    public static void main(final String[] args) {
        log.info("STARTING AfoReporter...");
        final AfoReporter main = new AfoReporter();
        log.info("  parsing cmd line...");
        JCommander.newBuilder().addObject(main).build().parse(args);
        try {
            main.run();
        } catch (final Exception e) {
            log.error("Exiting", e);
            System.exit(1);
        }
    }

    /**
     * @param e exception to memorize for main thread aborting
     * @see #threadException
     */
    private void reportException(final Exception e) {
        threadException = e;
    }

    /**
     * main method performing all tasks from reading the afos to creating the HTML report.
     *
     * @throws AfoReporterException in case there is any failure
     */
    private void run() {
        final List<AfoData> afos = new ArrayList<>();
        final Map<String, TestResult> results = new HashMap<>();
        final Map<String, List<Testcase>> afotcs = new HashMap<>();

        log.info("  collecting all data...");
        final Thread parseTestcases = initThreadToParseTestcases(afotcs);
        final Thread parseResults = initThreadToParseTestResults(results);
        parseTestcases.start();
        parseResults.start();

        final File localAfoFile = new File(afofile);
        if (!localAfoFile.exists()) {
            throw new AfoReporterException("Unable to find file " + localAfoFile.getAbsolutePath());
        }
        readAfos(afos, localAfoFile);
        // if no exception happened and afos have been returned its all fine else we send an error exit code
        if (afos.isEmpty()) {
            throw new AfoReporterException("No afos were found!");
        }
        if (threadException != null) {
            throw new AfoReporterException(threadException);
        }
        joinWorkerThreadAndRethrowAnyThreadException(parseTestcases);
        joinWorkerThreadAndRethrowAnyThreadException(parseResults);

        log.info("  merging afos, tcs, results...");
        // walk through all test cases of all afos, look the test case up in results and replace it with the result
        // if no test case is found in the results map create UNKNOWN test result and replace it with that
        // merge test case parser data with test results and attach it to the afo
        afos.stream()
            .filter(afo -> !"deleted".equals(afo.getAfoStatus().toString()))
            .forEach(afo -> determineRequirementResult(afo, afotcs.get(afo.getId()), results));
        createHTMLReport(afos, results);
    }

    /**
     * tries to join given thread and checks if any thread exception has been reported, will exit system with code -1
     * if.
     *
     * @param thread thread to join
     * @see #threadException
     */
    private void joinWorkerThreadAndRethrowAnyThreadException(final Thread thread) {
        if (thread == null) {
            log.debug("Thread not initialized, no need to wait");
            return;
        }
        try {
            if (thread.isAlive()) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Joining thread %s", thread.getName()));
                }
                thread.join();
            }
        } catch (final InterruptedException e) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Interrupted exception while joining %s thread!", thread.getName()), e);
            }
            thread.interrupt();
        }
        if (threadException != null) {
            throw new AfoReporterException(threadException);
        }
    }


    /**
     * read afos from given JSON file into given list.
     *
     * @param afos    list to fill the read afos in
     * @param afofile file to read the afos from
     */
    @SneakyThrows
    private void readAfos(final List<AfoData> afos, final File afofile) {
        if (log.isInfoEnabled()) {
            log.info(String.format("    reading afos from json file %s...", afofile.getAbsolutePath()));
        }
        final ObjectMapper mapper = new ObjectMapper();
        final List<AfoData> jsonAfos;
        jsonAfos = mapper.readValue(afofile, new TypeReference<>() {
        });
        afos.addAll(jsonAfos);
        if (log.isInfoEnabled()) {
            log.info(String.format("    read %d afos", afos.size()));
        }
    }

    /**
     * initializes thread to parse JUnit test result xml or Serenity result json  files.
     *
     * @param results list to fill test results into.
     * @return thread instance
     */
    private Thread initThreadToParseTestResults(final Map<String, TestResult> results) {
        final Thread parseResults;
        parseResults = new Thread(() -> {
            try {
                final ITestResultParser resultParser;
                final List<String> folders;
                final String logmsg;

                if (bdd.isEmpty()) {
                    resultParser = new AfoJUnitTestResultParser();
                    folders = resultRoot;
                    logmsg = "    parsing test rsults in  %s...";
                } else {
                    resultParser = new AfoSerenityTestParser();
                    folders = bdd;
                    logmsg = "    parsing serenity results in  %s...";
                }
                for (final String rootdir : folders) {
                    if (log.isInfoEnabled()) {
                        log.info(String.format(logmsg, rootdir));
                    }
                    resultParser.parseDirectoryForResults(results, new File(rootdir));
                }
                if (log.isInfoEnabled()) {
                    log.info(String.format("    %d test results parsed...", results.size()));
                }
                for (final TestResult tr : results.values()) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("      RES %s %s:%s", tr.getStatus(), tr.getClazz(), tr.getMethod()));
                    }
                }
            } catch (final Exception e) {
                log.error("Failure while parsing test results", e);
                reportException(e);
            }
        }, "res");
        return parseResults;
    }

    /**
     * initializes thread to parse JUnit test cases from source or scenarios from Serenity result files.
     *
     * @param afotcs map of testcases per afo to fill test cases into.
     * @return thread instance
     */
    private Thread initThreadToParseTestcases(final Map<String, List<Testcase>> afotcs) {
        final Thread parseTestcases;
        parseTestcases = new Thread(() -> {
            try {
                if (bdd.isEmpty()) {
                    for (final String rootdir : testRoot) {
                        if (log.isInfoEnabled()) {
                            log.info(String.format("    parsing test source code in  %s...", rootdir));
                        }
                        // TODO refactor ctor out of loop and simplify if branches
                        // FIRST check if reusing the parser is ok and doe snot have any side effects
                        final AfoJavaTestParser testParser = new AfoJavaTestParser();
                        testParser.parseDirectory(new File(rootdir));
                        // merge afotcs with parsed tcs per afo
                        mergeAfotcsWithParsedTcsPerAfo(afotcs, testParser);
                    }
                } else {
                    for (final String rootdir : bdd) {
                        if (log.isInfoEnabled()) {
                            log.info(String.format("    parsing test source code in  %s...", rootdir));
                        }
                        final AfoSerenityTestParser testParser = new AfoSerenityTestParser();
                        testParser.parseDirectory(new File(rootdir));
                        // merge afotcs with parsed tcs per afo
                        mergeAfotcsWithParsedTcsPerAfo(afotcs, testParser);
                    }
                }
                int tcs = 0;
                for (final List<Testcase> tclist : afotcs.values()) {
                    if (tclist != null) {
                        tcs += tclist.size();
                    }
                }

                logResults(afotcs, tcs);
            } catch (final Exception e) {
                log.error("Failure while parsing test source code", e);
                reportException(e);
            }
        }, "tcs");
        return parseTestcases;
    }

    private void logResults(final Map<String, List<Testcase>> afotcs, final int tcs) {
        if (log.isInfoEnabled()) {
            log.info(
                String.format("    test code parsed, found %d referenced afos and %d test cases", afotcs.size(), tcs));
        }
        if (log.isDebugEnabled()) {
            for (final Map.Entry<String, List<Testcase>> entry : afotcs.entrySet()) {
                log.debug(String.format("      AFO %s", entry.getKey()));
                for (final Testcase tc : entry.getValue()) {
                    log.debug(String.format("        %s:%s", tc.getClazz(), tc.getMethod()));
                }
            }
        }
    }

    private void mergeAfotcsWithParsedTcsPerAfo(final Map<String, List<Testcase>> afotcs,
        final ITestParser testParser) {
        final Map<String, List<Testcase>> parsedMap = testParser.getParsedTestcasesPerAfo();
        for (final Map.Entry<String, List<Testcase>> entry : parsedMap.entrySet()) {
            final String afoid = entry.getKey();
            if (afotcs.containsKey(afoid)) {
                afotcs.get(afoid).addAll(entry.getValue());
            } else {
                afotcs.put(afoid, entry.getValue());
            }
        }
        testParser.resetMap();
    }

    /**
     * creates html with header + overview section + list of afos (requirements) with each added a collapsable test case
     * list.
     *
     * @param afos    list of requirements
     * @param results map of test results per afo
     */
    private void createHTMLReport(final List<AfoData> afos, final Map<String, TestResult> results) {
        log.info("  creating HTML report...");
        final File aforeport = checkTargetFolderNReportFile();
        try {
            final String header;
            String body;
            if (templatesFolder == null) {
                log.info("    Using internal templates...");
                header = getUTF8Resource("/de/gematik/idp/tests/aforeport/header.html");
                body = getUTF8Resource("/de/gematik/idp/tests/aforeport/body.html");
            } else {
                log.info("    Using templates from '" + templatesFolder + "'...");
                header = FileUtils
                    .readFileToString(new File(templatesFolder + File.separator + "header.html"),
                        StandardCharsets.UTF_8);
                body = FileUtils
                    .readFileToString(new File(templatesFolder + File.separator + "body.html"), StandardCharsets.UTF_8);
            }
            final AfoStatistics stats = new AfoStatistics(afos);

            // overview section
            body = body.replace("${ReportDate}",
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")))
                .replace("${AfoNum}", String.valueOf(afos.size()))
                .replace("${TCAfoNum}", String.valueOf(stats.getValue("tcs")))
                .replace("${TCNum}", String.valueOf(results.size()));
            final String[] replaceTokens = {
                "passed", "failed", "skipped", "error", "unknown"
            };
            for (final String token : replaceTokens) {
                body = body.replace("${" + token + "Afo}", String.valueOf(stats.getValue(token)));
            }

            // ${Slices} for pie chart
            // based upon https://medium.com/hackernoon/a-simple-pie-chart-in-svg-dbdd653b6936
            final String slices = String.format(
                "{ percent: %s, color: '#28a745' },\n" +
                    "  { percent: %s, color: 'yellow' },\n" +
                    "  { percent: %s, color: 'orangered' },\n" +
                    "  { percent: %s, color: 'darkred' },\n" +
                    "  { percent: %s, color: '#ACB4BA' }\n",
                stats.getPercentage("passed"),
                stats.getPercentage("skipped"),
                stats.getPercentage("failed"),
                stats.getPercentage("error"),
                stats.getPercentage("unknown"));
            // list of afos that have tests associated
            final String afoTestedListHTML = createHTMLAfoList(afos.stream()
                .filter(afo -> afo.getResults() != null && !afo.getResults().isEmpty())
                .collect(Collectors.toList()));
            // untested afos
            final String afoNoTestsListHTML = createHTMLAfoList(afos.stream()
                .filter(afo -> afo.getResults() == null || afo.getResults().isEmpty())
                .collect(Collectors.toList()));
            body = body.replace("${Slices}", slices)
                .replace("${AfosTested}", afoTestedListHTML)
                .replace("${AfosUnTested}", afoNoTestsListHTML);

            FileUtils.writeStringToFile(aforeport, header + "\n" + body + "\n</html>", StandardCharsets.UTF_8);
            log.info("  HTML report created as " + aforeport.getAbsolutePath());
        } catch (final IOException | IllegalAccessException | NoSuchFieldException e) {
            throw new AfoReporterException("Failure while creating HTML report", e);
        }
    }

    /**
     * checks whether target fodler exists and creates it if not. Also checks if there is a report file and if deletes
     * it.
     *
     * @return "target/aforeport.html" file
     */
    private File checkTargetFolderNReportFile() {
        final File target = Paths.get("target", "site", "serenity").toFile();
        if (!target.exists() && !target.mkdirs()) {
            throw new AfoReporterException("Unable to create a target folder " + target.getAbsolutePath() + "!");
        }
        final File aforeport = new File(target, "aforeport.html");
        if (aforeport.exists()) {
            try {
                Files.delete(aforeport.toPath());
            } catch (final IOException ioe) {
                throw new AfoReporterException("Unable to delete " + aforeport.getAbsolutePath() + "!", ioe);
            }
        }
        return aforeport;
    }

    /**
     * return string as UTF8 read from the resource with given name
     *
     * @param resname name of resource to read
     * @return UTF8 string representation
     * @throws IOException if reading the resource fails
     */
    private String getUTF8Resource(final String resname) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(resname), StandardCharsets.UTF_8);
    }

    /**
     * creates HTML list of afos, with title, test result bar shown and a collapsable section with detailed test results
     * list.
     *
     * @param afos list of requirements to create HTML section for
     * @return HTML code for list of afos
     * @throws IOException if readingthe template files fails
     */
    private String createHTMLAfoList(final List<AfoData> afos) throws IOException {

        final String afoentry;
        final String tcentry;

        if (templatesFolder == null) {
            afoentry = getUTF8Resource("/de/gematik/idp/tests/aforeport/afoentry.html");
            tcentry = getUTF8Resource("/de/gematik/idp/tests/aforeport/tcentry.html");
        } else {
            afoentry = FileUtils
                .readFileToString(new File(templatesFolder + File.separator + "afoentry.html"), StandardCharsets.UTF_8);
            tcentry = FileUtils
                .readFileToString(new File(templatesFolder + File.separator + "tcentry.html"), StandardCharsets.UTF_8);
        }

        final StringBuilder afolist = new StringBuilder();
        final String HIDDEN = "hidden";

        afos.sort(Comparator.comparing(AfoData::getId));
        for (final AfoData afo : afos) {
            final StringBuilder tclist = new StringBuilder();
            final String resultbar = createResultBarNTestCaseList(tcentry, afo, tclist);
            afolist.append(afoentry.replace("${status}", afo.getStatus().toString().toLowerCase())
                .replace("${AfoID}", afo.getId()).replace("${AfoTitle}", afo.getTitle())
                .replace("${Testresults}", tclist).replace("${AfoResultBar}", resultbar)
                .replace("${AfoStatus}", afo.getAfoStatus().toString())
                .replace("${AfoHasRef}", afo.getRefName() == null ? HIDDEN : "")
                .replace("${AfoRefName}", Objects.requireNonNullElse(afo.getRefName(), "Undefiniert"))
                .replace("${AfoRefURL}", afo.getRefURL() != null ? "href=\"" + afo.getRefURL() + "\"" : "")
                .replace("${AfoRefHasURL}", afo.getRefURL() == null ? HIDDEN : "")
                .replace("${AfoAddedManually}", afo.getAfoStatus() == AfoStatus.ADDED ? "" : HIDDEN));
        }
        return afolist.toString();
    }

    private String createResultBarNTestCaseList(
        final String tcentry,
        final AfoData afo,
        final StringBuilder tclist) {
        final String resultbar;
        if (afo.getResults() != null && !afo.getResults().isEmpty()) {
            final StringBuilder bardata = new StringBuilder();
            for (final TestResult tr : afo.getResults()) {
                tclist.append(tcentry.replace("${TCStatus}", tr.getStatus().toString().toLowerCase())
                    .replace("${TCMethod}", tr.getMethod()).replace("${TCClass}", tr.getClazz()));
                bardata.append(tr.getStatus().toString().charAt(0));
            }
            resultbar = "<div class=\"resultbar\" data-value=\"" + bardata.toString() + "\"></div> "
                + "<span class=\"right small text-muted\">("
                + Objects.requireNonNullElseGet(afo.getResults(),
                () -> new ArrayList<>(0)).size()
                + ")</span>";
        } else {
            tclist.append("<h4 class=\"text-muted\">No Test cases / test results found</h4>");
            resultbar = "";
        }
        return resultbar;
    }

    /**
     * iterates through all test cases and if test result exists, add it to the given afo. At the end determines the
     * overall result status of the afo based on all found test results.
     *
     * @param afo       requirement to look up results for
     * @param testcases list of test cases knwon to be linked to given afo
     * @param results   list of all test results found
     */
    private void determineRequirementResult(final AfoData afo, final List<Testcase> testcases,
        final Map<String, TestResult> results) {
        if (testcases == null || testcases.isEmpty()) {
            afo.setStatus(Result.UNKNOWN);
            afo.setResults(new ArrayList<>());
            return;
        }
        final List<TestResult> aforesults = new ArrayList<>();
        for (final Testcase tc : Objects.requireNonNull(testcases)) {
            final TestResult tr = results.get(tc.getClazz() + ":" + tc.getMethod());
            if (tr != null) {
                aforesults.add(tr);
            }
        }
        afo.setResults(aforesults);
        afo.setStatus(getRequirementStatusFrom(aforesults));
    }

    /**
     * mapping overall result of requirement as follows basically its unknown, but if there is any failed or error tc
     * the overall status is failed/error if there is only unknown tcs but a few or one skipped its skipped if there are
     * only passed, skipped and unknown its passed
     */
    Result getRequirementStatusFrom(final List<TestResult> aforesults) {
        Result aforesult = Result.UNKNOWN;
        for (final TestResult tr : aforesults) {
            if (tr.status == Result.PASSED && (aforesult == Result.UNKNOWN || aforesult == Result.SKIPPED)) {
                aforesult = Result.PASSED;
            } else if (tr.status == Result.SKIPPED && aforesult == Result.UNKNOWN) {
                aforesult = Result.SKIPPED;
            } else if (tr.status == Result.FAILED) {
                aforesult = Result.FAILED;
            } else if (tr.status == Result.ERROR) {
                return Result.ERROR;
            }
        }
        return aforesult;
    }

    /**
     * Helper class to provide statistic numbers for all requirements found.
     */
    static class AfoStatistics {

        int passed = 0;
        int skipped = 0;
        int failed = 0;
        int error = 0;
        int unknown = 0;
        int tcs = 0;

        AfoStatistics(final List<AfoData> afos) {
            afos.stream()
                .filter(afo -> afo.getStatus() != null)
                .forEach(afo -> {
                    switch (afo.getStatus()) {
                        case PASSED:
                            passed++;
                            break;
                        case SKIPPED:
                            skipped++;
                            break;
                        case FAILED:
                            failed++;
                            break;
                        case ERROR:
                            error++;
                            break;
                        default:
                        case UNKNOWN:
                            unknown++;
                            break;
                    }
                    if (afo.getResults() != null) {
                        tcs += afo.getResults().size();
                    }
                });
        }

        /**
         * returns the sum of all results.
         *
         * @return sum of all results
         */
        int getSum() {
            return passed + skipped + failed + error + unknown;
        }

        /**
         * returns the absolute value of given type.
         *
         * @param type one of [passed|skipped|faile|error|unknown|tcs]
         * @return int value of given type
         * @throws NoSuchFieldException   if not one of the supported types
         * @throws IllegalAccessException should never happen
         */
        int getValue(final String type) throws NoSuchFieldException, IllegalAccessException {
            return getClass().getDeclaredField(type).getInt(this);
        }

        /**
         * returns the percentage value in range [0-1] of given type.
         *
         * @param type one of [passed|skipped|faile|error|unknown|tcs]
         * @return double percentage value of given type
         * @throws NoSuchFieldException   if not one of the supported types
         * @throws IllegalAccessException should never happen
         */
        double getPercentage(final String type) throws NoSuchFieldException, IllegalAccessException {
            return ((double) getValue(type)) / ((double) getSum());
        }
    }
}