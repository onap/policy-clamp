/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022, 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Profile("stub")
public class StubUtils {

    private static final Logger log = LoggerFactory.getLogger(StubUtils.class);
    private final ObjectMapper objectMapper;

    <T> ResponseEntity<T> getResponse(String path, Class<T> clazz) {
        final var resource = new ClassPathResource(path);
        try (var inputStream = resource.getInputStream()) {
            var targetObject = objectMapper.readValue(inputStream, clazz);
            return new ResponseEntity<>(targetObject, HttpStatus.OK);
        } catch (IOException exception) {
            log.error("Error reading the file.", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    <T> ResponseEntity<List<T>> getResponseList(String path) {
        final var resource = new ClassPathResource(path);
        try (var inputStream = resource.getInputStream()) {
            var targetObject = objectMapper.readValue(inputStream, new TypeReference<List<T>>() {});
            return new ResponseEntity<>(targetObject, HttpStatus.OK);
        } catch (IOException exception) {
            log.error("Error reading the file.", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
