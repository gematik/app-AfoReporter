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
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

@Slf4j
public class AfoJUnitTestResultParser implements ITestResultParser {

    @Override
    public void parseDirectoryForResults(final Map<String, TestResult> results, final File rootDir) {
        if (rootDir == null) {
            log.warn("Invalid NULL test result root dir");
        } else {
            if (rootDir.listFiles() == null) {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("Invalid test result root dir %s", rootDir.getAbsolutePath()));
                }
            } else {
                Arrays.asList(Objects.requireNonNull(rootDir.listFiles())).forEach(f -> {
                    if (f.getName().startsWith("TEST-") && f.getName().endsWith(".xml")) {
                        parseJunitXMLResult(f, results);
                    }
                });
            }
        }
    }

    private void parseJunitXMLResult(final File file, final Map<String, TestResult> results) {
        try {
            final DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
            df.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            df.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            final Document doc = df.newDocumentBuilder().parse(file);

            final NodeList suites = doc.getElementsByTagName("testsuite");
            for (int i = 0; i < suites.getLength(); i++) {
                parseTestSuite((Element) suites.item(i), results);
            }
        } catch (final Exception e) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Failure while parsing result file %s", file.getAbsolutePath()), e);
            }
        }
    }

    private void parseTestSuite(final Element suite, final Map<String, TestResult> results) {
        final NodeList tcs = suite.getChildNodes();
        for (int i = 0; i < tcs.getLength(); i++) {
            final Node tc = tcs.item(i);
            if (tc.getNodeName().equals("testcase")) {
                final TestResult tr = parseTestCase((Element) tc);
                tr.suite = suite.getAttribute("name");
                results.put(tr.getClazz() + ":" + tr.getMethod(), tr);
            }
        }
    }

    private TestResult parseTestCase(final Element tc) {
        final TestResult tr = new TestResult();
        tr.clazz = tc.getAttribute("classname");
        tr.method = tc.getAttribute("name");
        final NodeList details = tc.getChildNodes();
        if (details.getLength() == 0) {
            tr.status = Result.PASSED;
        } else {
            for (int i = 0; i < details.getLength(); i++) {
                if (details.item(i) instanceof Text) {
                    continue;
                }
                final Element detail = (Element) details.item(i);
                boolean parseAttr = false;
                switch (detail.getNodeName()) {
                    case "failure":
                        tr.status = Result.FAILED;
                        parseAttr = true;
                        break;
                    case "error":
                        tr.status = Result.ERROR;
                        parseAttr = true;
                        break;
                    case "skipped":
                        tr.status = Result.SKIPPED;
                        parseAttr = true;
                        break;
                    case "system-out":
                        tr.errsysout = detail.getTextContent();
                        break;
                    case "system-err":
                        tr.errsyserr = detail.getTextContent();
                        break;
                    default:
                        tr.status = Result.UNKNOWN;
                        break;
                }
                if (parseAttr) {
                    tr.errmessage = detail.getAttribute("message");
                    tr.errtype = detail.getAttribute("type");
                    tr.errdetails = detail.getTextContent();
                }
            }
        }
        return tr;
    }
}
