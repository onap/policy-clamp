/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuthorizationTest {

    private static String USERNAME = "username";
    private static String PASSWORD = "p4ssw0rd";

    @Test
    void cldsUserTest() {
        var cldsUser = new CldsUser();
        cldsUser.setUser(USERNAME);
        cldsUser.setPassword(PASSWORD);
        var permissions = new SecureServicePermission("type", "instance", "action");
        SecureServicePermission[] p = {permissions};
        cldsUser.setPermissions(p);

        assertEquals(USERNAME, cldsUser.getUser());
        assertEquals(PASSWORD, cldsUser.getPassword());
        assertEquals("type|instance|action", cldsUser.getPermissionsString()[0]);
    }

    @Test
    void secureServicePermission() {
        var permission = new SecureServicePermission("*", "*", "*");
        permission.setType("type");
        permission.setInstance("instance");
        permission.setAction("action");
        assertEquals("type", permission.getType());
        assertEquals("instance", permission.getInstance());
        assertEquals("action", permission.getAction());
    }

    @Test
    void authorizationControllertest() {
        var auth = new AuthorizationController();
        assertThat(auth.getClampInformation().getAllPermissions()).isEmpty();
    }

}
