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

package org.onap.clamp.clds;

import org.junit.BeforeClass;
import org.onap.clamp.clds.client.PolicyClient;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("clamp-default")
public abstract class AbstractIT {

    @Autowired
    protected RefProp      refProp;
    @Autowired
    protected PolicyClient policyClient;

    @BeforeClass
    public static void oneTimeSetUp() {
        System.setProperty("AJSC_CONF_HOME", System.getProperty("user.dir") + "/src/it/resources/");
        System.setProperty("CLDS_DCAE_URL", "http://localhost:13786/cl-dcae-services");
    }
}
