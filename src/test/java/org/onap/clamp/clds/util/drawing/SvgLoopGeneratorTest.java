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

package org.onap.clamp.clds.util.drawing;

import static org.assertj.core.api.Assertions.assertThat;
import com.google.gson.JsonObject;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.onap.clamp.loop.Loop;
import org.onap.clamp.loop.template.PolicyModel;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;
import org.xml.sax.SAXException;

public class SvgLoopGeneratorTest {
    private Loop getLoop() {
        MicroServicePolicy ms1 =
                new MicroServicePolicy("ms1", new PolicyModel("org.onap.ms1", "", "1.0.0", "short.ms1"),
                        false,
                        null);
        MicroServicePolicy ms2 =
                new MicroServicePolicy("ms2", new PolicyModel("org.onap.ms2", "", "1.0.0", "short.ms2"),
                        false, null);
        OperationalPolicy opPolicy = new OperationalPolicy("OperationalPolicy", new Loop(), new JsonObject(),
                new PolicyModel("org.onap.opolicy", null, "1.0.0", "short.OperationalPolicy"), null);
        Loop loop = new Loop();
        loop.addMicroServicePolicy(ms1);
        loop.addMicroServicePolicy(ms2);
        loop.addOperationalPolicy(opPolicy);
        return loop;
    }

    /**
     * Test a Svg rendering with all objects.
     *
     * @throws IOException In case of isssues
     * @throws ParserConfigurationException In case of isssues
     * @throws SAXException In case of isssues
     */
    @Test
    public void getAsSvgTest() throws IOException, ParserConfigurationException, SAXException {
        String xml = SvgLoopGenerator.getSvgImage(getLoop());
        assertThat(xml).contains("data-element-id=\"VES\"");
        assertThat(xml).contains(">VES<");
        assertThat(xml).contains("data-element-id=\"ms1\"");
        assertThat(xml).contains("data-element-id=\"ms2\"");
        assertThat(xml).contains(">short.ms1<");
        assertThat(xml).contains(">short.ms2<");
        assertThat(xml).contains("data-element-id=\"OperationalPolicy\"");
        assertThat(xml).contains(">short.OperationalPolicy<");

    }
}
