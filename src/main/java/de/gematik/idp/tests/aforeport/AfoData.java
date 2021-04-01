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

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AfoData {

    private String id;
    private String version;
    private String title;
    private Result status;
    private List<TestResult> results;
    private AfoStatus afoStatus;
    private String refName;
    private String refURL;
    private String description;
    private String petStatus;

    // default ctor needed for Jackson object mapper
    public AfoData() {
        afoStatus = AfoStatus.NOTSET;
    }

    public AfoData(final String id, final String title) {
        this.id = id;
        this.title = title;
    }

    public void merge(final AfoData localAfo) {
        afoStatus = localAfo.getAfoStatus();
        refName = localAfo.getRefName();
        refURL = localAfo.getRefURL();
        if (localAfo.getAfoStatus() == AfoStatus.DELETED) {
            status = Result.UNKNOWN;
        }
        if (version == null) {
            if (localAfo.getVersion() != null) {
                version = localAfo.getVersion();
            }
        } else if (localAfo.getVersion() != null) {
            final int v1 = Integer.parseInt(version.replaceFirst("^0+", ""));
            final int v2 = Integer.parseInt(localAfo.getVersion().replaceFirst("^0+", ""));
            if (v1 > v2) {
                version = localAfo.getVersion();
            }
        }
        // else keep version !
    }

    public AfoStatus getAfoStatus() {
        return afoStatus != null ? afoStatus : AfoStatus.NOTSET;
    }

    // used by Polarion Toolbox
    public void sanitizeAfoId() {
        final String afoid = getId();
        final int underScore = afoid.indexOf("_");
        final int dash = afoid.indexOf('-', underScore + 1);
        if (dash != -1) {
            setVersion(afoid.substring(dash + 1));
            setId(afoid.substring(0, dash));
        }
    }

    public String getIdAndVersion() {
        if (version != null) {
            return id + "-" + version;
        } else {
            return id;
        }
    }
}
