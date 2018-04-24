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
 * 
 */

package org.onap.clamp.clds.config.sdc;

/**
 * This class is used to decode the configuration found in
 * application.properties, this is related to the blueprint mapping
 * configuration that is used to create data in database, according to the
 * blueprint content coming from SDC.
 */
public class BlueprintParserFilesConfiguration {

    private String svgXmlFilePath;
    private String bpmnXmlFilePath;

    public String getBpmnXmlFilePath() {
        return bpmnXmlFilePath;
    }

    public void setBpmnXmlFilePath(String bpmnXmlFilePath) {
        this.bpmnXmlFilePath = bpmnXmlFilePath;
    }

    public String getSvgXmlFilePath() {
        return svgXmlFilePath;
    }

    public void setSvgXmlFilePath(String svgXmlFilePath) {
        this.svgXmlFilePath = svgXmlFilePath;
    }
}
