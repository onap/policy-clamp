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
 * Modifications copyright (c) 2018 Nokia
 * ===================================================================
 * 
 */

package org.onap.clamp.clds.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.junit.Test;
import org.onap.clamp.clds.service.CldsUser;

public class CldsUserJsonDecoderTest {

    private String user1 = "admin1";
    private String user2 = "admin2";
    private String password = "5f4dcc3b5aa765d61d8327deb882cf99";
    private String[] normalPermissionsArray = {
        "permission-type-cl|dev|read", "permission-type-cl|dev|update", "permission-type-cl-manage|dev|*",
        "permission-type-filter-vf|dev|*", "permission-type-template|dev|read",
        "permission-type-template|dev|update"
    };
    private String[] incompletePermissionsArray = {
        "permission-type-cl|dev|*", "permission-type-cl|dev|*", "permission-type-cl-manage|dev|*",
        "permission-type-filter-vf|dev|*", "permission-type-template|dev|read",
        "permission-type-template|dev|update"
    };

    @Test
    public void testDecodingDoubleUsers() {

        //when
        CldsUser[] usersArray = CldsUserJsonDecoder
            .decodeJson(CldsUserJsonDecoderTest.class.getResourceAsStream("/clds/clds-users-two-users.json"));

        //then
        assertThat(usersArray).hasSize(2);
        assertThat(usersArray[0])
            .extracting(CldsUser::getUser, CldsUser::getPassword, CldsUser::getPermissionsString)
            .containsExactly(user1, password, normalPermissionsArray);

        assertThat(usersArray[1])
            .extracting(CldsUser::getUser, CldsUser::getPassword, CldsUser::getPermissionsString)
            .containsExactly(user2, password, normalPermissionsArray);

    }

    @Test
    public void testDecodingNoPermission() {
        // when
        CldsUser[] usersArray = CldsUserJsonDecoder
                .decodeJson(this.getClass().getResourceAsStream("/clds/clds-users-no-permission.json"));

        //then
        assertThat(usersArray).hasSize(1);
        CldsUser user = usersArray[0];
        assertThat(user.getUser()).isEqualTo(user1);
        assertThat(user.getPassword()).isEqualTo(null);
        assertThat(user.getPermissionsString()).isEmpty();
    }

    @Test
    public void testDecodingIncompletePermissions() {

        //when
        CldsUser[] usersArray = CldsUserJsonDecoder
                .decodeJson(this.getClass().getResourceAsStream("/clds/clds-users-incomplete-permissions.json"));

        //then
        assertThat(usersArray).hasSize(1);
        CldsUser user = usersArray[0];
        assertThat(user.getUser()).isEqualTo(user1);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getPermissionsString()).isEqualTo(incompletePermissionsArray);
    }
}
