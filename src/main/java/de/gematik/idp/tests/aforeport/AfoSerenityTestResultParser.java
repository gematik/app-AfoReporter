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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;

@Slf4j
public class AfoSerenityTestResultParser implements ITestResultParser {

    @Override
    public void parseDirectoryForResults(final Map<String, TestResult> results, final File rootDir) {
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
                    .filter(f -> f.isFile() && f.getName().endsWith(".json"))
                    .forEach(f -> inspectFileForResults(f, results));
            }
        }
    }

    // TO DO move to ctor for Testcase with JSONObject as param
    private void inspectFileForResults(final File f, final Map<String, TestResult> results) {
        if (f.getName().equals("requirements.json")) {
            return;
        }
        try {
            final String gherkin = Files.readString(f.toPath());
            final JSONObject jso = new JSONObject(gherkin);
            final TestResult tr = new TestResult();
            setTestCaseClassNMethod(jso, tr);
            tr.setStatus(mapSerenityStatus(jso.getString("result")));
            if (tr.status == Result.ERROR || tr.status == Result.FAILED) {
                final JSONObject jsoErr;
                if (jso.has("exception")) {
                    jsoErr = jso.getJSONObject("exception");
                } else if (jso.has("testFailureCause")) {
                    jsoErr = jso.getJSONObject("testFailureCause");
                } else {
                    throw new AfoReporterException("Unable to find failure/error details in " + jso.toString(2));
                }
                tr.setErrmessage(jsoErr.getString("message"));
                tr.setErrdetails(""); // TO DO add stacktrace here
                tr.setErrtype(jsoErr.getString("errorType"));
            }
            results.put(tr.getClazz() + ":" + tr.getMethod(), tr);

        } catch (final IOException | JSONException ioe) {
            log.error("Failed to parse BDD file " + f.getAbsolutePath(), ioe);
        }
    }

    private void setTestCaseClassNMethod(final JSONObject jso, final Testcase tc) {
        final String id = jso.getString("id");
        final String[] idarr = id.split(";");
        tc.setClazz(String.join(".", Arrays.copyOf(idarr, idarr.length - 1)));
        tc.setMethod(idarr[idarr.length - 1]);
        tc.setFeatureName(jso.getJSONObject("userStory").getString("storyName"));
        tc.setScenarioName(jso.getString("title"));
        tc.setPath(jso.getJSONObject("userStory").getString("path"));
    }

    private Result mapSerenityStatus(final String serenityStatus) {
        switch (serenityStatus) {
            case "FAILURE":
                return Result.FAILED;
            case "ERROR":
                return Result.ERROR;
            case "SUCCESS":
                return Result.PASSED;
            case "SKIPPED":
                return Result.SKIPPED;
            default:
                return Result.UNKNOWN;
        }
    }
}
