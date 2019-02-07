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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onap.clamp.clds.exception.sdc.controller.SdcParametersException;
import org.onap.clamp.clds.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * This class maps the SDC config JSON file. This JSON can have multiple
 * sdc-controller config. So the json is loaded in a static way and the instance
 * must specify the controller name that it represents.
 */
public class SdcControllersConfiguration {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(SdcControllersConfiguration.class);
    public static final String CONTROLLER_SUBTREE_KEY = "sdc-connections";
    @Autowired
    protected ApplicationContext appContext;
    /**
     * The file name that will be loaded by Spring.
     */
    @Value("${clamp.config.files.sdcController:'classpath:/clds/sdc-controllers-config.json'}")
    protected String sdcControllerFile;
    /**
     * The root of the JSON.
     */
    private JsonObject jsonRootNode;

    @PostConstruct
    public void loadConfiguration() throws IOException {
        Resource resource = appContext.getResource(sdcControllerFile);
        // Try to load json tree
        jsonRootNode = JsonUtils.GSON.fromJson(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8), JsonObject.class);
    }

    public SdcSingleControllerConfiguration getSdcSingleControllerConfiguration(String controllerName) {
        return getAllDefinedControllers().get(controllerName);
    }

    /**
     * This method reads all Controllers configurations and returns them.
     *
     * @return A list of controller Names defined in the config
     */
    public Map<String, SdcSingleControllerConfiguration> getAllDefinedControllers() {
        Map<String, SdcSingleControllerConfiguration> result = new HashMap<>();
        if (jsonRootNode.get(CONTROLLER_SUBTREE_KEY) != null) {
            jsonRootNode.get(CONTROLLER_SUBTREE_KEY).getAsJsonObject().entrySet().forEach(
                entry -> result.put(entry.getKey(),
                    new SdcSingleControllerConfiguration(entry.getValue().getAsJsonObject(), entry.getKey())));
        } else {
            throw new SdcParametersException(
                    CONTROLLER_SUBTREE_KEY + " key not found in the file: " + sdcControllerFile);
        }
        return result;
    }
}
