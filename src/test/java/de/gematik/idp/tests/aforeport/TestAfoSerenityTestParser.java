package de.gematik.idp.tests.aforeport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestAfoSerenityTestParser {

    @Test
    public void parseSerenityResultsOK() {
        final AfoSerenityTestResultParser parser = new AfoSerenityTestResultParser();

        final Map<String, TestResult> results = new HashMap<>();
        parser.parseDirectoryForResults(results, Paths.get("src", "test", "resources", "bdd").toFile());

        assertThat(results).containsOnlyKeys(
            "fordere-access-token-mittels-sso-token-an:gettoken-mit-sso-token---veralteter-sso-token-wird-abgelehnt",
            "fordere-access-token-mit-einer-signierten-challenge-an:gettoken-signierte-challenge---veralteter-token-code-wird-abgelehnt");
    }

    @Test
    public void parseSerenityResultsInvalidRoot() {
        final AfoSerenityTestResultParser parser = new AfoSerenityTestResultParser();

        final Map<String, TestResult> results = new HashMap<>();
        parser.parseDirectoryForResults(results, Paths.get("src", "test", "resources", "bdd-NonExisting").toFile());

        assertThat(results.keySet()).hasSize(0);
    }
}
