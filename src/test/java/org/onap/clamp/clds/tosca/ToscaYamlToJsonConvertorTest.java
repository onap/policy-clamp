/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
 *                             reserved.
 * Modifications Copyright (C) 2019 Huawei Technologies Co., Ltd.
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

package org.onap.clamp.clds.tosca;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.skyscreamer.jsonassert.JSONAssert;

public class ToscaYamlToJsonConvertorTest {

    /**
     * This Test validates TOSCA yaml to JSON Schema conversion based on JSON Editor
     * Schema.
     *
     * @throws IOException In case of issue when opening the tosca yaml file and
     *                     converted json file
     */
    @Test
    public final void testParseToscaYaml() throws IOException {
        String toscaModelYaml = ResourceFileUtil.getResourceAsString("tosca/tosca_example.yaml");
        ToscaYamlToJsonConvertor convertor = new ToscaYamlToJsonConvertor();

        String parsedJsonSchema = convertor.parseToscaYaml(toscaModelYaml,
                "onap.policies.monitoring.cdap.tca.hi.lo.app");
        assertNotNull(parsedJsonSchema);
        JSONAssert.assertEquals(ResourceFileUtil.getResourceAsString("tosca/policy-yaml-to-json.json"),
                parsedJsonSchema, true);
    }

    /**
     * This Test validates TOSCA yaml with constraints to JSON Schema conversion
     * based on JSON Editor Schema.
     *
     * @throws IOException In case of issue when opening the tosca yaml file and
     *                     converted json file
     */
    @Test
    public final void testParseToscaYamlWithConstraints() throws IOException {
        String toscaModelYaml = ResourceFileUtil.getResourceAsString("tosca/tosca-with-constraints.yaml");
        ToscaYamlToJsonConvertor convertor = new ToscaYamlToJsonConvertor();

        String parsedJsonSchema = convertor.parseToscaYaml(toscaModelYaml, "onap.policies.monitoring.example.app");
        assertNotNull(parsedJsonSchema);
        JSONAssert.assertEquals(ResourceFileUtil.getResourceAsString("tosca/policy-yaml-to-json-with-constraints.json"),
                parsedJsonSchema, true);
    }

    /**
     * This Test validates TOSCA yaml with different datatypes to JSON Schema
     * conversion based on JSON Editor Schema.
     *
     * @throws IOException In case of issue when opening the tosca yaml file and
     *                     converted json file
     */
    @Test
    public final void testParseToscaYamlWithTypes() throws IOException {
        String toscaModelYaml = ResourceFileUtil.getResourceAsString("tosca/tosca-with-datatypes.yaml");
        ToscaYamlToJsonConvertor convertor = new ToscaYamlToJsonConvertor();

        String parsedJsonSchema = convertor.parseToscaYaml(toscaModelYaml, "onap.policies.monitoring.example.app");
        assertNotNull(parsedJsonSchema);
        JSONAssert.assertEquals(ResourceFileUtil.getResourceAsString("tosca/policy-yaml-to-json-with-datatypes.json"),
                parsedJsonSchema, true);
    }
}
