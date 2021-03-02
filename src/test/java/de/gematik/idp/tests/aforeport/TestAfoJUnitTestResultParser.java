package de.gematik.idp.tests.aforeport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestAfoJUnitTestResultParser {

    @Test
    void testJunitResultParseOK() {
        final AfoJUnitTestResultParser parser = new AfoJUnitTestResultParser();

        final Map<String, TestResult> results = new HashMap<>();
        parser.parseDirectoryForResults(results, Paths.get("src", "test", "resources", "junit").toFile());

        assertThat(results.keySet()).hasSize(357);
        assertThat(
            results.get("de.gematik.idp.tests.aforeport.TestAfoJUnitTestResultParser:testJunitResultParseOK").status)
            .isEqualTo(Result.FAILED);
    }

    @Test
    public void parseJUnitResultsInvalidRoot() {
        final AfoJUnitTestResultParser parser = new AfoJUnitTestResultParser();

        final Map<String, TestResult> results = new HashMap<>();
        parser.parseDirectoryForResults(results, Paths.get("src", "test", "resources", "bdd-NonExisting").toFile());

        assertThat(results.keySet()).hasSize(0);
    }
}
