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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class TestResult extends Testcase {

    public static TestResult fromTestcase(final Testcase tc) {
        final TestResult tr = new TestResult();
        tr.setClazz(tc.getClazz());
        tr.setMethod(tc.getMethod());
        tr.setFeatureName(tc.getFeatureName());
        tr.setScenarioName(tc.getScenarioName());
        tr.setPath(tc.getPath());
        tr.status = Result.UNKNOWN;
        return tr;
    }

    String suite;
    Result status;
    String errmessage;
    String errtype;
    String errdetails;
    String errsysout;
    String errsyserr;
}
