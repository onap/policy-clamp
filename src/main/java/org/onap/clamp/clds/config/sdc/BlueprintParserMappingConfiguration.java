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


import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.onap.clamp.clds.util.JsonUtils;

/**
 * This class is used to decode the configuration found in
 * application.properties, this is related to the blueprint mapping
 * configuration that is used to create data in database, according to the
 * blueprint content coming from SDC.
 */
public class BlueprintParserMappingConfiguration {

    private static final Type BLUEPRINT_MAP_CONF_TYPE = new TypeToken<List<BlueprintParserMappingConfiguration>>() {
    }.getType();
    private String blueprintKey;
    private boolean dcaeDeployable;
    private BlueprintParserFilesConfiguration files;

    public String getBlueprintKey() {
        return blueprintKey;
    }

    public void setBlueprintKey(String blueprintKey) {
        this.blueprintKey = blueprintKey;
    }

    public BlueprintParserFilesConfiguration getFiles() {
        return files;
    }

    public void setFiles(BlueprintParserFilesConfiguration filesConfig) {
        this.files = filesConfig;
    }

    public boolean isDcaeDeployable() {
        return dcaeDeployable;
    }

    public static List<BlueprintParserMappingConfiguration> createFromJson(InputStream json) {
        return JsonUtils.GSON.fromJson(new InputStreamReader(json, StandardCharsets.UTF_8), BLUEPRINT_MAP_CONF_TYPE);
    }
}
