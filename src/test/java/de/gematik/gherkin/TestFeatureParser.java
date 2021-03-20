package de.gematik.gherkin;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.gherkin.model.Feature;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class TestFeatureParser {

    @Test
    public void parseDiscDoc() {
        final FeatureParser fp = new FeatureParser();
        final Feature f = fp
            .parseFeatureFile(Paths.get("src", "test", "resources", "bdd", "discoveryDocument.feature").toFile());

        assertThat(f.getScenarios()).hasSize(4);
    }


    @Test
    public void parsegetTokenWIthSignedChallenge() {
        final FeatureParser fp = new FeatureParser();
        final Feature f = fp
            .parseFeatureFile(
                Paths.get("src", "test", "resources", "bdd", "getTokenWithSignedChallenge.feature").toFile());

        assertThat(f.getScenarios()).hasSize(14);
    }

}
