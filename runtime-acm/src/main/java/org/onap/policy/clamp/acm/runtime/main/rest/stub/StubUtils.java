/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.main.rest.stub;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.slf4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Profile("stub")
public class StubUtils {

    private static final StandardYamlCoder YAML_TRANSLATOR = new StandardYamlCoder();
    private static final Gson JSON_TRANSLATOR = new Gson();
    private static final String YAML = "application/yaml";
    private static final String ACCEPT = "Accept";

    <T> ResponseEntity<T> getResponse(String path, Class<T> clazz,
            HttpServletRequest request, Logger log) {
        String accept = request.getHeader(ACCEPT);
        final ClassPathResource resource = new ClassPathResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            if (accept.contains(YAML)) {
                var targetObject = YAML_TRANSLATOR.decode(inputStream, clazz);
                return new ResponseEntity<>(targetObject, HttpStatus.OK);
            } else {
                final String string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                var targetObject = JSON_TRANSLATOR.fromJson(string, clazz);
                return new ResponseEntity<>(targetObject, HttpStatus.OK);
            }
        } catch (IOException | CoderException exception) {
            log.error("Error reading the file.", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
