/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

/**
 * This class is useful to store the information concerning
 * blueprint artifact extracted from SDC CSAR
 */
package org.onap.clamp.clds.sdc.controller.installer;

public class BlueprintArtifact {

    private String dcaeBlueprint;
    private String blueprintArtifactName;
    private String blueprintInvariantResourceUuid;
    private String blueprintInvariantServiceUuid;
    private String blueprintResourceInstanceName;

    public String getDcaeBlueprint() {
        return dcaeBlueprint;
    }

    public void setDcaeBlueprint(String dcaeBlueprint) {
        this.dcaeBlueprint = dcaeBlueprint;
    }

    public String getBlueprintArtifactName() {
        return blueprintArtifactName;
    }

    public void setBlueprintArtifactName(String blueprintArtifactName) {
        this.blueprintArtifactName = blueprintArtifactName;
    }

    public String getBlueprintInvariantResourceUuid() {
        return blueprintInvariantResourceUuid;
    }

    public void setBlueprintInvariantResourceUuid(String blueprintInvariantResourceUuid) {
        this.blueprintInvariantResourceUuid = blueprintInvariantResourceUuid;
    }

    public String getBlueprintInvariantServiceUuid() {
        return blueprintInvariantServiceUuid;
    }

    public void setBlueprintInvariantServiceUuid(String blueprintInvariantServiceUuid) {
        this.blueprintInvariantServiceUuid = blueprintInvariantServiceUuid;
    }

    public String getBlueprintResourceInstanceName() {
        return blueprintResourceInstanceName;
    }

    public void setBlueprintResourceInstanceName(String blueprintResourceInstanceName) {
        this.blueprintResourceInstanceName = blueprintResourceInstanceName;
    }
}
