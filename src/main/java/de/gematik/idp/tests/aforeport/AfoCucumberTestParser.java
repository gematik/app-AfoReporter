/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.idp.tests.aforeport;

import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.FeatureLoader;
import gherkin.ast.Background;
import gherkin.ast.Scenario;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.Tag;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class AfoCucumberTestParser implements ITestParser {

    private static final String AFO_TOKEN = "@Afo:";
    private final Map<String, List<Testcase>> parsedTestcasesPerAfo = new HashMap<>();
    private final Map<String, Testcase> parsedTestcases = new HashMap<>();

    public void parseDirectory(final File rootDir) {
        if (rootDir == null) {
            log.warn("Invalid test source NULL root dir");
        } else {
            final File[] files = rootDir.listFiles();
            if (files == null) {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("Invalid test source root dir %s", rootDir.getAbsolutePath()));
                }
            } else {
                Arrays.stream(files)
                    .filter(f -> f.isFile() && f.getName().endsWith(".feature"))
                    .forEach(this::inspectFile);
                Arrays.stream(files)
                    .filter((File::isDirectory))
                    .forEach(this::parseDirectory);
            }
        }
    }

    private List<Tag> getTags(final ScenarioDefinition sd) {
        if (sd instanceof Scenario) {
            return ((Scenario) sd).getTags();
        } else if (sd instanceof ScenarioOutline) {
            return ((ScenarioOutline) sd).getTags();
        } else {
            return List.of();
        }
    }

    private void inspectFile(final File f) {
        final FeatureLoader fl = new FeatureLoader(new MultiLoader(null));
        final List<CucumberFeature> features = fl.load(Collections.singletonList(f.toURI()));

        final CucumberFeature cucFeature = features.get(0);

        cucFeature.getGherkinFeature().getFeature().getChildren().stream()
            .filter(ch -> !(ch instanceof Background))
            .forEach(ch -> getTags(ch).stream()
                .filter(tag -> tag.getName().startsWith(AFO_TOKEN))
                .forEach(afotag -> {
                    final String afoid = afotag.getName().substring(AFO_TOKEN.length());
                    parsedTestcasesPerAfo.computeIfAbsent(afoid, k -> new ArrayList<>());

                    final Testcase tc = new Testcase();
                    tc.setFeatureName(cucFeature.getName());
                    tc.setScenarioName(ch.getName());
                    tc.setClazz(convertToId(cucFeature.getName()));
                    tc.setMethod(convertToId(ch.getName()));
                    tc.setPath(f.getAbsolutePath());

                    parsedTestcases.putIfAbsent(tc.getClazz() + ":" + tc.getMethod(), tc);
                    parsedTestcasesPerAfo.get(afoid).add(tc);

                }));
    }

    private String convertToId(String name) {
        final String chars = " ;,.+*~\\/!$()[]{}";
        for (int i = 0; i < chars.length(); i++) {
            name = name.replace(chars.charAt(i), '-');
        }
        return name.toLowerCase();
    }
}



























