/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.tosca.update;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonObject;
import java.io.IOException;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.tosca.update.parser.metadata.ToscaMetadataParser;
import org.onap.clamp.clds.tosca.update.parser.metadata.ToscaMetadataParserWithDictionarySupport;
import org.onap.clamp.clds.tosca.update.templates.JsonTemplateManager;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.loop.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ToscaConverterWithDictionarySupport {

    private static final EELFLogger logger =
            EELFManager.getInstance().getLogger(ToscaConverterWithDictionarySupport.class);

    private ClampProperties clampProperties;
    private ToscaMetadataParser metadataParser;

    /**
     * Constructor with Spring support.
     *
     * @param clampProperties Clamp Spring properties
     * @param metadataParser  Metadata parser
     */
    @Autowired
    public ToscaConverterWithDictionarySupport(ClampProperties clampProperties,
                                               ToscaMetadataParserWithDictionarySupport metadataParser) {
        this.clampProperties = clampProperties;
        this.metadataParser = metadataParser;
    }

    /**
     * This method converts a tosca file to a json schema.
     * It uses some parameters specified in the application.properties.
     *
     * @param toscaFile          The tosca file as String
     * @param policyTypeToDecode The policy type to decode
     * @param serviceModel       The service model associated so that the clamp enrichment could be done if required by
     *                           the tosca model
     * @return A json object being a json schema
     */
    public JsonObject convertToscaToJsonSchemaObject(String toscaFile, String policyTypeToDecode,
                                                     Service serviceModel) {
        try {
            return new JsonTemplateManager(toscaFile,
                    clampProperties.getFileContent("tosca.converter.default.datatypes"),
                    clampProperties.getFileContent("tosca.converter.json.schema.templates"))
                    .getJsonSchemaForPolicyType(policyTypeToDecode, Boolean.parseBoolean(clampProperties.getStringValue(
                            "tosca.converter.dictionary.support.enabled")) ? metadataParser : null, serviceModel);
        } catch (IOException | UnknownComponentException e) {
            logger.error("Unable to convert the tosca properly, exception caught during the decoding",
                    e);
            return new JsonObject();
        }
    }

    /**
     * This method converts a tosca file to a json schema.
     * It uses some parameters specified in the application.properties.
     *
     * @param toscaFile          The tosca file as String
     * @param policyTypeToDecode The policy type to decode
     * @param serviceModel       The service Model so that clamp enrichment could be done if required by tosca model
     * @return A String containing the json schema
     */
    public String convertToscaToJsonSchemaString(String toscaFile, String policyTypeToDecode, Service serviceModel) {
        return JsonUtils.GSON.toJson(this.convertToscaToJsonSchemaObject(toscaFile, policyTypeToDecode, serviceModel));
    }
}
