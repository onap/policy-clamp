/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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
 * ============LICENSE_END=========================================================
 * Modifications copyright (c) 2018 Nokia
 * ================================================================================
 *
 */

package org.onap.clamp.clds.model.sdc;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class SdcResourceBasicInfoTest {

    @Test
    public void testHashCode() {
        SdcResourceBasicInfo sdc1a = new SdcResourceBasicInfo();
        sdc1a.setName("test1");
        sdc1a.setVersion("1.0");

        SdcResourceBasicInfo sdc2 = new SdcResourceBasicInfo();
        sdc2.setName("test2");
        sdc2.setVersion("2.0");

        SdcResourceBasicInfo sdc1b = new SdcResourceBasicInfo();
        sdc1b.setName("test1");
        sdc1b.setVersion("2.0");

        Assertions.assertThat(sdc1a.hashCode()).isNotEqualTo(sdc1b.hashCode());
        Assertions.assertThat(sdc1b.hashCode()).isNotEqualTo(sdc2.hashCode());
        sdc1b.setVersion("1.0");
        Assertions.assertThat(sdc1a.hashCode()).isEqualTo(sdc1b.hashCode());

    }

    @Test
    public void testCompareTo() {
        SdcResourceBasicInfo sdc1a = new SdcResourceBasicInfo();
        sdc1a.setName("test1");
        sdc1a.setVersion("1.0");

        SdcResourceBasicInfo sdc1b = new SdcResourceBasicInfo();
        sdc1b.setName("test1");
        sdc1b.setVersion("2.0");

        SdcResourceBasicInfo sdc2 = new SdcResourceBasicInfo();
        sdc2.setName("test2");
        sdc2.setVersion("2.0");

        Assertions.assertThat(sdc1a.compareTo(sdc1b)).isEqualTo(-1);
        Assertions.assertThat(sdc1b.compareTo(sdc1a)).isEqualTo(1);
        Assertions.assertThat(sdc1a.compareTo(sdc1a)).isEqualTo(0);
        Assertions.assertThat(sdc1a.compareTo(sdc2)).isEqualTo(-1);
    }

    @Test
    public void testEquals() {
        SdcResourceBasicInfo sdc1a = new SdcResourceBasicInfo();
        sdc1a.setName("test1");
        sdc1a.setVersion("1.0");

        SdcResourceBasicInfo sdc1b = new SdcResourceBasicInfo();
        sdc1b.setName("test1");
        sdc1b.setVersion("2.0");

        SdcResourceBasicInfo sdc2 = new SdcResourceBasicInfo();
        sdc2.setName("test2");
        sdc2.setVersion("2.0");

        Assertions.assertThat(sdc1a.equals(sdc1a)).isTrue();
        Assertions.assertThat(sdc1a.equals(sdc1b)).isFalse();

        sdc1b.setVersion(null);
        Assertions.assertThat(sdc1a.equals(sdc1b)).isFalse();
        sdc1b.setVersion("1.0");
        Assertions.assertThat(sdc1a.equals(sdc1b)).isTrue();
        sdc1a.setVersion(null);
        sdc1b.setVersion(null);
        Assertions.assertThat(sdc1a.equals(sdc1b)).isTrue();

        sdc1b.setName(null);
        Assertions.assertThat(sdc1a.equals(sdc1b)).isFalse();
        sdc1b.setName("test1");
        Assertions.assertThat(sdc1a.equals(sdc1b)).isTrue();
        sdc1a.setName(null);
        sdc1b.setName(null);
        Assertions.assertThat(sdc1a.equals(sdc1b)).isTrue();
    }

}
