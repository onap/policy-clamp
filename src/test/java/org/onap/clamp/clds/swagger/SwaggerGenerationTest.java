/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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
 */
package org.onap.clamp.clds.swagger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.nio.file.Path;
import java.nio.file.Paths;
import io.github.swagger2markup.Swagger2MarkupConverter;
import org.onap.clamp.clds.Application;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {Application.class, SwaggerConfig.class})
public class SwaggerGenerationTest {

    @Test
    public void convertRemoteSwaggerToAsciiDoc() {
        Path localSwaggerFile = Paths.get("docs/swagger/swagger.json");
        Swagger2MarkupConverter.from(localSwaggerFile).build();
    }
}
