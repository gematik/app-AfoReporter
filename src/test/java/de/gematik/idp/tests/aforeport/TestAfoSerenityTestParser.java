package de.gematik.idp.tests.aforeport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestAfoSerenityTestParser {

    @Test
    public void parseSerenityResultsOK() {
        final AfoSerenityTestParser parser = new AfoSerenityTestParser();

        parser.parseDirectory(Paths.get("src", "test", "resources", "bdd").toFile());
        final Map<String, List<Testcase>> tcs = parser.getParsedTestcasesPerAfo();

        assertThat(tcs).containsOnlyKeys("A_20315-01", "A_20314");
        assertThat(tcs.get("A_20315-01")).hasSize(2);
        assertThat(tcs.get("A_20315-01").get(0).getMethod())
            .isEqualTo("gettoken-mit-sso-token---veralteter-sso-token-wird-abgelehnt");
    }

    @Test
    public void parseSerenityResultsInvalidRoot() {
        final AfoSerenityTestParser parser = new AfoSerenityTestParser();

        parser.parseDirectory(Paths.get("src", "test", "resources", "bdd-NonExisting").toFile());
        final Map<String, List<Testcase>> tcs = parser.getParsedTestcasesPerAfo();

        assertThat(tcs.keySet()).hasSize(0);
    }
}
