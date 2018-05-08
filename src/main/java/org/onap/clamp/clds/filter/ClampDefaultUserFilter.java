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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.clamp.clds.filter;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.config.ClampUserWrap;
import org.onap.clamp.clds.config.CldsUserJsonDecoder;
import org.onap.clamp.clds.exception.CldsUsersException;
import org.onap.clamp.clds.service.CldsUser;


public class ClampDefaultUserFilter  implements Filter {
    private CldsUser defaultUser;
    @Autowired
    private ClampProperties refProp;

    // Load the default user
    public void init(FilterConfig cfg) throws ServletException {
        try { 
            CldsUser[] users = CldsUserJsonDecoder.decodeJson(refProp.getFileContent("files.cldsUsers"));
            defaultUser = users[0];
        } catch (IOException e) {
            // not able to load default user
        	throw new CldsUsersException("Exception occurred during the decoding of the clds-users.json", e);
        }
  }

    // Call the ClampUserWrapper
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest hreq = (HttpServletRequest)req;	
        chain.doFilter(new ClampUserWrap(hreq, defaultUser.getUser(), Arrays.asList(defaultUser.getPermissionsString())), res);
    }

    public void destroy() {
    }
}
