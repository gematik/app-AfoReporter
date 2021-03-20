package de.gematik.gherkin.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Scenario extends GherkinStruct {

    private Feature feature;
    private List<Step> steps = new ArrayList<>();

    @Override
    public String toString() {
        return super.toString() + " , Feature=" + feature.getName();
    }
}
