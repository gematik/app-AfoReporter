package de.gematik.gherkin.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Feature extends GherkinStruct {

    private String fileName;
    private Background background;
    private List<GherkinStruct> scenarios = new ArrayList<>();
}
