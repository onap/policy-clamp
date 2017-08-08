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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.onap.clamp.clds.service.CldsUser;

public class CldsUserJsonDecoderTest {

    private String   user1                      = "admin1";
    private String   user2                      = "admin2";

    private String   password                   = "5f4dcc3b5aa765d61d8327deb882cf99";
    private String[] normalPermissionsArray     = { "permission-type-cl|dev|read", "permission-type-cl|dev|update",
            "permission-type-cl-manage|dev|*", "permission-type-filter-vf|dev|*", "permission-type-template|dev|read",
            "permission-type-template|dev|update" };

    private String[] incompletePermissionsArray = { "permission-type-cl|dev|*", "permission-type-cl|dev|*",
            "permission-type-cl-manage|dev|*", "permission-type-filter-vf|dev|*", "permission-type-template|dev|read",
            "permission-type-template|dev|update" };

    @Test
    public void testDecodingDoubleUsers() throws Exception {
        CldsUser[] usersArray = CldsUserJsonDecoder
                .decodeJson(CldsUserJsonDecoderTest.class.getResourceAsStream("/clds/clds-users-two-users.json"));

        assertEquals(usersArray.length, 2);

        assertEquals(usersArray[0].getUser(), user1);
        assertEquals(usersArray[1].getUser(), user2);

        assertEquals(usersArray[0].getPassword(), password);
        assertEquals(usersArray[1].getPassword(), password);

        assertArrayEquals(usersArray[0].getPermissionsString(), normalPermissionsArray);
        assertArrayEquals(usersArray[1].getPermissionsString(), normalPermissionsArray);
    }

    @Test
    public void testDecodingNoPermission() throws Exception {
        CldsUser[] usersArray = CldsUserJsonDecoder
                .decodeJson(this.getClass().getResourceAsStream("/clds/clds-users-no-permission.json"));

        assertEquals(usersArray.length, 1);
        assertEquals(usersArray[0].getUser(), user1);
        assertEquals(usersArray[0].getPassword(), null);
        assertArrayEquals(usersArray[0].getPermissionsString(), new String[0]);
    }

    @Test
    public void testDecodingIncompletePermissions() throws Exception {
        CldsUser[] usersArray = CldsUserJsonDecoder
                .decodeJson(this.getClass().getResourceAsStream("/clds/clds-users-incomplete-permissions.json"));

        assertEquals(usersArray.length, 1);
        assertEquals(usersArray[0].getUser(), user1);
        assertEquals(usersArray[0].getPassword(), password);
        assertArrayEquals(usersArray[0].getPermissionsString(), incompletePermissionsArray);
    }

}
