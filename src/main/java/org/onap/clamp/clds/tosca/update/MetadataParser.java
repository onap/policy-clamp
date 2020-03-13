
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

import com.google.gson.JsonObject;
import org.onap.clamp.tosca.DictionaryService;

public class MetadataParser {

    /**
     * This method is used to start the processing of the metadata field.
     *
     * @param property          The property metadata as Json Object
     * @param dictionaryService the Dictionary service, if null nothing will be done
     * @return The jsonObject structure that must be added to the json schema
     */
    public static JsonObject processAllMetadataElement(Property property, DictionaryService dictionaryService) {
        if (dictionaryService != null) {
            return null;
        } else {
            return null;
        }
    }
}
