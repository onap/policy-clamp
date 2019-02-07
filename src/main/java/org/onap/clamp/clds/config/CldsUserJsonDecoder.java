/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.config;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import java.util.List;
import org.apache.commons.io.IOUtils;
import org.onap.clamp.clds.exception.CldsUsersException;
import org.onap.clamp.clds.service.CldsUser;
import org.onap.clamp.clds.util.JsonUtils;

public class CldsUserJsonDecoder {

    /**
     * This method decodes the JSON file provided to a CldsUser Array. The
     * stream is closed after this call, this is not possible to reuse it.
     * 
     * @param cldsUsersFile
     *            The inputStream containing the users json file
     * @return CldsUser[] Array containing a list of the user defined in the
     *         JSON file
     */
    public static CldsUser[] decodeJson(InputStream cldsUsersFile) {
        try {
            return decodeJson(IOUtils.toString(cldsUsersFile, StandardCharsets.UTF_8.name()));
        } catch (IOException e) {
            throw new CldsUsersException("Exception occurred during the decoding of the clds-users.json", e);
        }
    }

    public static CldsUser[] decodeJson(String cldsUsersString) {
        try {
            // the ObjectMapper readValue method closes the stream no need to do
            // it
            return JsonUtils.GSON.fromJson(cldsUsersString, CldsUser[].class);
        } catch (JsonParseException e) {
            throw new CldsUsersException("Exception occurred during the decoding of the clds-users.json", e);
        }
    }

    private CldsUserJsonDecoder() {
    }
}
