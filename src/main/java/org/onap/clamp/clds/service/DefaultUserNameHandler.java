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

package org.onap.clamp.clds.service;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

public class DefaultUserNameHandler implements UserNameHandler {

    public DefaultUserNameHandler() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.onap.clamp.clds.service.PrincipalNameHandler#handleName(javax.ws.rs.
     * core.SecurityContext)
     */
    @Override
    public String retrieveUserName(SecurityContext securityContext) {
        Principal p = securityContext.getUserPrincipal();
        return (p == null ? "Not found" : p.getName());
    }
}
