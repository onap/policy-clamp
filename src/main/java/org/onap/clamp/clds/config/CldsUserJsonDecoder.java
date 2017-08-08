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

package org.onap.clamp.clds.config;

import java.io.IOException;
import java.io.InputStream;

import org.onap.clamp.clds.service.CldsUser;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CldsUserJsonDecoder {

    /**
     * This method decodes the JSON file provided to a CldsUser Array. The
     * stream is closed after this call, this is not possible to reuse it.
     * 
     * @return CldsUser[] Array containing a list of the user defined in the
     *         JSON file
     */
    public static CldsUser[] decodeJson(InputStream cldsUsersFile) throws IOException {
        // the ObjectMapper readValue method closes the stream no need to do it
        return new ObjectMapper().readValue(cldsUsersFile, CldsUser[].class);
    }
}
