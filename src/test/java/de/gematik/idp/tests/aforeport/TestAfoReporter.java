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

import static de.gematik.idp.tests.aforeport.Result.ERROR;
import static de.gematik.idp.tests.aforeport.Result.FAILED;
import static de.gematik.idp.tests.aforeport.Result.PASSED;
import static de.gematik.idp.tests.aforeport.Result.SKIPPED;
import static de.gematik.idp.tests.aforeport.Result.UNKNOWN;
import static de.gematik.idp.tests.aforeport.Result.values;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

//@ExtendWith(TestResultLoggerExtension.class)
@Slf4j
class TestAfoReporter {

    public AfoReporter sut;

    @BeforeEach
    public void beforeEach(final TestInfo info) {
        info.getTestMethod().ifPresentOrElse(
            m -> log.debug(m.getName() + ": " + info.getDisplayName()),
            () -> log.debug("UKNOWN METHOD: " + info.getDisplayName()));
        sut = new AfoReporter();
    }

    public static Object[][] allResults() {
        final Object[][] params = new Object[5][1];
        int i = 0;
        for (final Result r : values()) {
            params[i++] = new Object[]{r};
        }
        return params;
    }

    /**
     * pairwise combination with order respected
     */
    public static Object[][] allButOneResults() {
        return new Object[][]{
            {PASSED, ERROR, ERROR},
            {PASSED, FAILED, FAILED},
            {PASSED, SKIPPED, PASSED},
            {PASSED, UNKNOWN, PASSED},
            {ERROR, PASSED, ERROR},
            {ERROR, FAILED, ERROR},
            {ERROR, SKIPPED, ERROR},
            {ERROR, UNKNOWN, ERROR},
            {FAILED, PASSED, FAILED},
            {FAILED, ERROR, ERROR},
            {FAILED, SKIPPED, FAILED},
            {FAILED, UNKNOWN, FAILED},
            {SKIPPED, PASSED, PASSED},
            {SKIPPED, ERROR, ERROR},
            {SKIPPED, FAILED, FAILED},
            {SKIPPED, UNKNOWN, SKIPPED},
            {UNKNOWN, PASSED, PASSED},
            {UNKNOWN, ERROR, ERROR},
            {UNKNOWN, FAILED, FAILED},
            {UNKNOWN, SKIPPED, SKIPPED},
        };
    }

    /**
     * threewise combination with order respected
     */
    public static Object[][] allButTwoResults() {
        return new Object[][]{
            {PASSED, ERROR, FAILED, ERROR},
            {PASSED, ERROR, SKIPPED, ERROR},
            {PASSED, ERROR, UNKNOWN, ERROR},

            {PASSED, FAILED, ERROR, ERROR},
            {PASSED, FAILED, SKIPPED, FAILED},
            {PASSED, FAILED, UNKNOWN, FAILED},

            {PASSED, SKIPPED, ERROR, ERROR},
            {PASSED, SKIPPED, FAILED, FAILED},
            {PASSED, SKIPPED, UNKNOWN, PASSED},

            {PASSED, UNKNOWN, ERROR, ERROR},
            {PASSED, UNKNOWN, FAILED, FAILED},
            {PASSED, UNKNOWN, SKIPPED, PASSED},

            // Skipping Result.ERROR as it overruled all single instances already

            {FAILED, PASSED, ERROR, ERROR},
            {FAILED, PASSED, SKIPPED, FAILED},
            {FAILED, PASSED, UNKNOWN, FAILED},

            {FAILED, ERROR, PASSED, ERROR},
            {FAILED, ERROR, SKIPPED, ERROR},
            {FAILED, ERROR, UNKNOWN, ERROR},

            {FAILED, SKIPPED, ERROR, ERROR},
            {FAILED, SKIPPED, PASSED, FAILED},
            {FAILED, SKIPPED, UNKNOWN, FAILED},

            {FAILED, UNKNOWN, ERROR, ERROR},
            {FAILED, UNKNOWN, SKIPPED, FAILED},
            {FAILED, UNKNOWN, PASSED, FAILED},

            {SKIPPED, PASSED, ERROR, ERROR},
            {SKIPPED, PASSED, FAILED, FAILED},
            {SKIPPED, PASSED, UNKNOWN, PASSED},

            {SKIPPED, ERROR, PASSED, ERROR},
            {SKIPPED, ERROR, FAILED, ERROR},
            {SKIPPED, ERROR, UNKNOWN, ERROR},

            {SKIPPED, FAILED, ERROR, ERROR},
            {SKIPPED, FAILED, PASSED, FAILED},
            {SKIPPED, FAILED, UNKNOWN, FAILED},

            {SKIPPED, UNKNOWN, ERROR, ERROR},
            {SKIPPED, UNKNOWN, FAILED, FAILED},
            {SKIPPED, UNKNOWN, PASSED, PASSED},

            {UNKNOWN, PASSED, ERROR, ERROR},
            {UNKNOWN, PASSED, FAILED, FAILED},
            {UNKNOWN, PASSED, SKIPPED, PASSED},

            {UNKNOWN, ERROR, FAILED, ERROR},
            {UNKNOWN, ERROR, PASSED, ERROR},
            {UNKNOWN, ERROR, SKIPPED, ERROR},

            {UNKNOWN, FAILED, ERROR, ERROR},
            {UNKNOWN, FAILED, PASSED, FAILED},
            {UNKNOWN, FAILED, SKIPPED, FAILED},

            {UNKNOWN, SKIPPED, ERROR, ERROR},
            {UNKNOWN, SKIPPED, PASSED, PASSED},
            {UNKNOWN, SKIPPED, FAILED, FAILED},
        };
    }

    @ParameterizedTest
    @MethodSource("allResults")
    void testRequirementStatusAllIdentical(final Result result) {
        log.debug("Creating list of " + result + " test results...");
        final List<TestResult> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final TestResult tr = new TestResult();
            tr.status = result;
            results.add(tr);
        }
        final Result aforesult = sut.getRequirementStatusFrom(results);
        assertThat(aforesult).isEqualTo(result);
        log.debug("Test PASSED");
    }

    @ParameterizedTest
    @MethodSource("allButOneResults")
    void testRequirementStatusAllIdenticalButOneFirst(final Result almostAllResults, final Result otherResult,
        final Result oracle) {
        log.debug("Creating list of " + almostAllResults + " test results with one " + otherResult + "...");
        final List<TestResult> results = new ArrayList<>();
        TestResult tr = new TestResult();
        tr.status = otherResult;
        results.add(tr);
        for (int i = 0; i < 10; i++) {
            tr = new TestResult();
            tr.status = almostAllResults;
            results.add(tr);
        }

        final Result aforesult = sut.getRequirementStatusFrom(results);
        assertThat(aforesult).isEqualTo(oracle);
        log.debug("Test PASSED");
    }

    @ParameterizedTest
    @MethodSource("allButOneResults")
    void testRequirementStatusAllIdenticalButOneMiddle(final Result almostAllResults, final Result otherResult,
        final Result oracle) {
        log.debug("Creating list of " + almostAllResults + " test results with one " + otherResult + "...");
        final List<TestResult> results = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            final TestResult tr = new TestResult();
            if (i == 6) {
                tr.status = otherResult;
            } else {
                tr.status = almostAllResults;
            }
            results.add(tr);
        }

        final Result aforesult = sut.getRequirementStatusFrom(results);
        assertThat(aforesult).isEqualTo(oracle);
        log.debug("Test PASSED");
    }

    @ParameterizedTest
    @MethodSource("allButOneResults")
    void testRequirementStatusAllIdenticalButOneLast(final Result almostAllResults, final Result otherResult,
        final Result oracle) {
        log.debug("Creating list of " + almostAllResults + " test results with one " + otherResult + "...");
        final List<TestResult> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final TestResult tr = new TestResult();
            tr.status = almostAllResults;
            results.add(tr);
        }
        final TestResult tr = new TestResult();
        tr.status = otherResult;
        results.add(tr);

        final Result aforesult = sut.getRequirementStatusFrom(results);
        assertThat(aforesult).isEqualTo(oracle);
        log.debug("Test PASSED");
    }

    @ParameterizedTest
    @MethodSource("allButTwoResults")
    void testRequirementStatusAllIdenticalButFirstLast(
        final Result almostAllResults,
        final Result otherResult1,
        final Result otherResult2,
        final Result oracle) {
        log.debug("Creating list of " + almostAllResults + " test results with one " + otherResult1 + " and one "
            + otherResult2 + "...");
        final List<TestResult> results = new ArrayList<>();
        TestResult tr = new TestResult();
        tr.status = otherResult1;
        results.add(tr);
        for (int i = 0; i < 10; i++) {
            tr = new TestResult();
            tr.status = almostAllResults;
            results.add(tr);
        }
        tr = new TestResult();
        tr.status = otherResult2;
        results.add(tr);

        final Result aforesult = sut.getRequirementStatusFrom(results);
        assertThat(aforesult).isEqualTo(oracle);
        log.debug("Test PASSED");
    }

    @ParameterizedTest
    @MethodSource("allButTwoResults")
    void testRequirementStatusAllIdenticalButLastFirst(
        final Result almostAllResults,
        final Result otherResult1,
        final Result otherResult2,
        final Result oracle) {
        log.debug("Creating list of " + almostAllResults + " test results with one " + otherResult1 + " and one "
            + otherResult2 + "...");
        final List<TestResult> results = new ArrayList<>();
        TestResult tr = new TestResult();
        tr.status = otherResult2;
        results.add(tr);
        for (int i = 0; i < 10; i++) {
            tr = new TestResult();
            tr.status = almostAllResults;
            results.add(tr);
        }
        tr = new TestResult();
        tr.status = otherResult1;
        results.add(tr);

        final Result aforesult = sut.getRequirementStatusFrom(results);
        assertThat(aforesult).isEqualTo(oracle);
        log.debug("Test PASSED");
    }

    @ParameterizedTest
    @MethodSource("allButTwoResults")
    void testRequirementStatusAllIdenticalButFirstMiddle(
        final Result almostAllResults,
        final Result otherResult1,
        final Result otherResult2,
        final Result oracle) {
        log.debug("Creating list of " + almostAllResults + " test results with one " + otherResult1 + " and one "
            + otherResult2 + "...");
        final List<TestResult> results = new ArrayList<>();
        TestResult tr = new TestResult();
        tr.status = otherResult1;
        results.add(tr);
        for (int i = 0; i < 11; i++) {
            tr = new TestResult();
            if (i == 6) {
                tr.status = otherResult2;
            } else {
                tr.status = almostAllResults;
            }
            results.add(tr);
        }

        final Result aforesult = sut.getRequirementStatusFrom(results);
        assertThat(aforesult).isEqualTo(oracle);
        log.debug("Test PASSED");
    }

    @ParameterizedTest
    @MethodSource("allButTwoResults")
    void testRequirementStatusAllIdenticalButMiddleFirst(
        final Result almostAllResults,
        final Result otherResult1,
        final Result otherResult2,
        final Result oracle) {
        log.debug("Creating list of " + almostAllResults + " test results with one " + otherResult1 + " and one "
            + otherResult2 + "...");
        final List<TestResult> results = new ArrayList<>();
        TestResult tr = new TestResult();
        tr.status = otherResult2;
        results.add(tr);
        for (int i = 0; i < 11; i++) {
            tr = new TestResult();
            if (i == 6) {
                tr.status = otherResult1;
            } else {
                tr.status = almostAllResults;
            }
            results.add(tr);
        }

        final Result aforesult = sut.getRequirementStatusFrom(results);
        assertThat(aforesult).isEqualTo(oracle);
        log.debug("Test PASSED");
    }

    @ParameterizedTest
    @MethodSource("allButTwoResults")
    void testRequirementStatusAllIdenticalButLastMiddle(
        final Result almostAllResults,
        final Result otherResult1,
        final Result otherResult2,
        final Result oracle) {
        log.debug("Creating list of " + almostAllResults + " test results with one " + otherResult1 + " and one "
            + otherResult2 + "...");
        final List<TestResult> results = new ArrayList<>();
        TestResult tr;
        for (int i = 0; i < 11; i++) {
            tr = new TestResult();
            if (i == 6) {
                tr.status = otherResult2;
            } else {
                tr.status = almostAllResults;
            }
            results.add(tr);
        }
        tr = new TestResult();
        tr.status = otherResult1;
        results.add(tr);

        final Result aforesult = sut.getRequirementStatusFrom(results);
        assertThat(aforesult).isEqualTo(oracle);
        log.debug("Test PASSED");
    }

    @ParameterizedTest
    @MethodSource("allButTwoResults")
    void testRequirementStatusAllIdenticalButMiddleLast(
        final Result almostAllResults,
        final Result otherResult1,
        final Result otherResult2,
        final Result oracle) {
        log.debug(String.format("Creating list of %s test results with one %s and one %s...",
            almostAllResults, otherResult1, otherResult2));
        final List<TestResult> results = new ArrayList<>();
        TestResult tr;
        for (int i = 0; i < 11; i++) {
            tr = new TestResult();
            if (i == 6) {
                tr.status = otherResult1;
            } else {
                tr.status = almostAllResults;
            }
            results.add(tr);
        }
        tr = new TestResult();
        tr.status = otherResult2;
        results.add(tr);

        final Result aforesult = sut.getRequirementStatusFrom(results);
        assertThat(aforesult).isEqualTo(oracle);
        log.debug("Test PASSED");
    }


    @Test
    public void testBDDHTMLReportGeneratorOK() {
        final AfoReporter reporter = new AfoReporter();
        reporter.dump = true;
        reporter.bdd = true;
        reporter.testRoot = Collections
            .singletonList(Paths.get("src", "test", "resources", "bdd").toFile().getAbsolutePath());
        reporter.resultRoot = reporter.testRoot;
        reporter.afofile = Paths.get("src", "test", "resources", "requirements.json").toFile().getAbsolutePath();

        reporter.run();

        assertThat(Paths.get("target", "site", "serenity", "aforeport.html").toFile()).exists();
    }

    @Test
    public void testJavaCodeHTMLReportGeneratorOK() {
        final AfoReporter reporter = new AfoReporter();
        reporter.dump = true;
        reporter.testRoot = Collections
            .singletonList(Paths.get("src", "test", "java").toFile().getAbsolutePath());
        reporter.afofile = Paths.get("src", "test", "resources", "requirements.json").toFile().getAbsolutePath();

        reporter.run();

        assertThat(Paths.get("target", "site", "serenity", "aforeport.html").toFile()).exists();
    }

    @Test
    public void testJUnitHTMLReportGeneratorOK() {
        final AfoReporter reporter = new AfoReporter();
        reporter.dump = true;
        reporter.resultRoot = Collections
            .singletonList(Paths.get("src", "test", "resources", "junit").toFile().getAbsolutePath());
        reporter.afofile = Paths.get("src", "test", "resources", "requirements.json").toFile().getAbsolutePath();

        reporter.run();

        assertThat(Paths.get("target", "site", "serenity", "aforeport.html").toFile()).exists();
    }
}
