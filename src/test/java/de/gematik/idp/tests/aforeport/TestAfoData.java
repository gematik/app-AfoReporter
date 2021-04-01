package de.gematik.idp.tests.aforeport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class TestAfoData {

    @Test
    void testMergeOK() {
        final AfoData afo1 = new AfoData("1", "afo1");
        afo1.setAfoStatus(AfoStatus.DELETED);
        afo1.setResults(new ArrayList<>());
        final AfoData afo2 = new AfoData();
        afo2.setId("2");
        afo2.setTitle("afo2");
        afo2.setAfoStatus(AfoStatus.ADDED);
        afo2.setRefName("ref2");
        afo2.setRefURL("url2");
        afo2.setResults(Collections.singletonList(new TestResult()));

        afo1.merge(afo2);

        assertThat(afo1.getId()).isEqualTo("1");
        assertThat(afo1.getResults()).hasSize(0);
        assertThat(afo1.getTitle()).isEqualTo("afo1");
        assertThat(afo1.getAfoStatus()).isEqualTo(AfoStatus.ADDED);
        assertThat(afo1.getRefName()).isEqualTo("ref2");
        assertThat(afo1.getRefURL()).isEqualTo("url2");
    }

    @Test
    void testMergeStatusDeleted() {
        final AfoData afo1 = new AfoData("1", "afo1");
        final AfoData afo2 = new AfoData("2", "afo2");
        afo1.setAfoStatus(AfoStatus.ADDED);
        afo1.setStatus(Result.PASSED);
        afo2.setAfoStatus(AfoStatus.DELETED);

        afo1.merge(afo2);

        assertThat(afo1.getStatus()).isEqualTo(Result.UNKNOWN);
    }
}
