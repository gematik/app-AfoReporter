package de.gematik.idp.tests.aforeport;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.idp.tests.Afo;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestAfoJavaTestParser {

    @Test
    @Afo("A_20315-01")
    void testParseTestSourcesOK() {
        final AfoJavaTestParser parser = new AfoJavaTestParser();

        parser.parseDirectory(Paths.get("src", "test").toFile());
        final Map<String, List<Testcase>> tcs = parser.getParsedTestcasesPerAfo();

        assertThat(tcs).containsOnlyKeys("A_20315-01");
        assertThat(tcs.get("A_20315-01")).hasSize(1);
        assertThat(tcs.get("A_20315-01").get(0).getMethod()).isEqualTo("testParseTestSourcesOK");
    }

}
