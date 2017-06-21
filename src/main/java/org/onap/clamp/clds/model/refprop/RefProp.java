/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.model.refprop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Holds reference properties.
 */
public class RefProp {
    private static final Logger logger = Logger.getLogger(RefProp.class.getName());

    @Autowired
    private ApplicationContext appContext;
    
    private Properties prop;

    @Value("${org.onap.clamp.config.files.cldsReference:'classpath:/clds/clds-reference.properties'}")
    private String cldsReferenceValuesFile;

    /**
     * Load reference properties via null constructor
     *
     * @throws IOException
     */
    public RefProp() throws IOException {
    }
    
    @PostConstruct
    public void loadConfig () throws IOException {
        prop = new Properties();
        Resource resource = appContext.getResource(cldsReferenceValuesFile);
        prop.load(resource.getInputStream());
    }

    /**
     * get property value
     *
     * @param key
     * @return
     */
    public String getStringValue(String key) {
        return prop.getProperty(key);
    }

    /**
     * get property value for a combo key (key1 + "." + key2).  If not found just use key1.
     *
     * @param key1
     * @param key2
     * @return
     */
    public String getStringValue(String key1, String key2) {
        String value = getStringValue(key1 + "." + key2);
        if (value == null || value.length() == 0) {
            value = getStringValue(key1);
        }
        return value;
    }

    /**
     * Return json as objects that can be updated
     *
     * @param key
     * @return
     * @throws IOException
     */
    public JsonNode getJsonTemplate(String key) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(getStringValue(key), JsonNode.class);
    }

    /**
     * Return json as objects that can be updated.  First try with combo key (key1 + "." + key2), otherwise default to just key1.
     *
     * @param key1
     * @param key2
     * @return
     * @throws IOException
     */
    public JsonNode getJsonTemplate(String key1, String key2) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(getStringValue(key1, key2), JsonNode.class);
    }

    /**
     * Get list of values for a property field containing json and a field/keyword within that json.
     *
     * @param fieldName
     * @param value
     * @return
     * @throws IOException
     */
    public List<String> decodeToList(String fieldName, String value) throws IOException {
        JsonNode decode = getJsonTemplate(fieldName);
        Iterator<JsonNode> itr = decode.path(value).elements();
        ArrayList<String> al = new ArrayList<>();
        while (itr.hasNext()) {
            JsonNode node = itr.next();
            al.add(node.asText());
        }
        return al;
    }

}
