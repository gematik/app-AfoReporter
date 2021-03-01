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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AfoData {

    private String id;
    private String title;
    private Result status;
    private List<TestResult> results;
    private AfoStatus afoStatus;
    private String refName;
    private String refURL;


    // default ctor needed for Jackson object mapper
    public AfoData() {
        afoStatus = AfoStatus.NOTSET;
    }

    public AfoData(final String id, final String title) {
        this.id = id;
        this.title = title;
    }

    public void merge(final AfoData afo) {
        afoStatus = afo.getAfoStatus();
        refName = afo.getRefName();
        refURL = afo.getRefURL();
        if (afo.getAfoStatus() == AfoStatus.DELETED) {
            status = Result.UNKNOWN;
        }
    }

    public AfoStatus getAfoStatus() {
        return afoStatus != null ? afoStatus : AfoStatus.NOTSET;
    }
}
