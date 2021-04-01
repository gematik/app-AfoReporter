package de.gematik.gherkin;

import de.gematik.gherkin.model.Feature;
import de.gematik.gherkin.model.Scenario;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public class TestPolarionJSON {

    @Test
    public void testJSONString() throws IOException {
        final FeatureParser fp = new FeatureParser();
        final Feature f = fp
            .parseFeatureFile(Paths.get("..", "idp-global", "idp-testsuite", "src", "test",
                "resources", "features", "coreFeatures", "authorizationWithSignedChallenge.feature").toFile());

        final String s = "[\n" +
            f.getScenarios().stream()
                .map(Scenario.class::cast)
                .map(scenario -> scenario.toPolarionJSON().toString(2))
                .collect(Collectors.joining(",\n")) + "]";
        final FileWriter fw = new FileWriter("IDP_REF_AUTH.json", StandardCharsets.UTF_8);
        IOUtils.write(s, fw);
        fw.close();
        System.out.println("JSON:" + s);
    }

}
