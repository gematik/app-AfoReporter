package de.gematik.gherkin.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ScenarioOutline extends Scenario {

    private Step examples;

}
