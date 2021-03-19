package de.gematik.idp.tests.aforeport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestAfoCucumberTestParser {

    @Test
    public void parseCucumberFeaturesOK() {
        final AfoCucumberTestParser parser = new AfoCucumberTestParser();

        parser.parseDirectory(Paths.get("src", "test", "resources", "bdd").toFile());
        final Map<String, List<Testcase>> tcs = parser.getParsedTestcasesPerAfo();

        assertThat(tcs).containsOnlyKeys("A_19874", "A_20688", "A_20457", "A_20623", "A_20668", "A_20614", "A_20591");
        assertThat(tcs.get("A_20623")).hasSize(1);
        assertThat(tcs.get("A_20623").get(0).getMethod())
            .isEqualTo("disc---discovery-dokument-muss-signiert-sein");
    }

    @Test
    public void parseCucumberFeaturesInvalidRoot() {
        final AfoCucumberTestParser parser = new AfoCucumberTestParser();

        parser.parseDirectory(Paths.get("src", "test", "resources", "bdd-NonExisting").toFile());
        final Map<String, List<Testcase>> tcs = parser.getParsedTestcasesPerAfo();

        assertThat(tcs.keySet()).hasSize(0);
    }
}
