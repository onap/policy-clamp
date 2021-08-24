/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.loop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.onap.policy.clamp.loop.components.external.ExternalComponentState;

public class ExternalComponentStateTest {
    private ExternalComponentState state =  new ExternalComponentState("NOT_SENT",
            "The policies defined have NOT yet been created on the policy engine", 90);

    @Test
    public void generalTest() {
        assertThat(state).hasToString("NOT_SENT");
        state.setLevel(70);
        assertThat(state.getLevel()).isEqualTo(70);
    }

    @Test
    public void equalsTest() {
        assertThat(state.equals(null)).isFalse();

        ExternalComponentState state2 =  new ExternalComponentState("NOT_SENT",
               "The policies defined have NOT yet been created on the policy engine", 90);
        assertThat(state.equals(state2)).isTrue();

        assertThat(state.equals(12)).isFalse();

        state2.setLevel(70);
        assertThat(state.equals(state2)).isTrue();

        ExternalComponentState state3 =  new ExternalComponentState("SENT",
                "The policies defined have NOT yet been created on the policy engine", 90);
        assertThat(state.equals(state3)).isFalse();

        ExternalComponentState state4 =  new ExternalComponentState(null,
                "The policies defined have NOT yet been created on the policy engine", 90);
        ExternalComponentState state5 =  new ExternalComponentState(null,
                "The policies defined have NOT yet been", 50);
        assertThat(state4.equals(state3)).isFalse();
        assertThat(state4.equals(state5)).isTrue();
    }

    @Test
    public void compareToTest() {
        ExternalComponentState state2 =  new ExternalComponentState("NOT_SENT",
               "The policies defined have NOT yet been created on the policy engine", 90);
        assertThat(state.compareTo(state2)).isEqualTo(0);

        ExternalComponentState state3 =  new ExternalComponentState("SENT",
                "The policies defined have NOT yet been created on the policy engine", 50);
        assertThat(state.compareTo(state3)).isEqualTo(1);

        ExternalComponentState state4 =  new ExternalComponentState(null,
                "The policies defined have NOT yet been created on the policy engine", 100);
        assertThat(state.compareTo(state4)).isEqualTo(-1);

    }
}