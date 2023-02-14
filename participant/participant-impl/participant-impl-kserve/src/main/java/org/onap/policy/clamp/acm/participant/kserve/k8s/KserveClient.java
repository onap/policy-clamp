/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.kserve.k8s;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import okhttp3.Response;
import org.onap.policy.clamp.acm.participant.kserve.parameters.CustomResourceDefinitionParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KserveClient {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ApiClient apiClient;

    private final CustomResourceDefinitionParameters crdParams;

    CustomObjectsApi customObjectsApi;

    @PostConstruct
    void initialize() {
        apiClient.setDebugging(logger.isDebugEnabled());
        customObjectsApi = new CustomObjectsApi(apiClient);
    }

    /**
     * Deploy the inference service.
     *
     * @param namespace   k8s namespace
     * @param jsonContent k8s payload
     * @throws ApiException exception
     */
    public boolean deployInferenceService(String namespace, String jsonContent) throws ApiException, IOException {
        var httpCall = customObjectsApi.createNamespacedCustomObjectCall(crdParams.getGroup(), crdParams.getVersion(),
                namespace, crdParams.getPlural(), jsonContent.getBytes(), null, null, null, null);
        try (Response httpResponse = httpCall.execute()) {
            logger.debug("Response of inference service deploy in namespace {} is {}", namespace, httpResponse);
            return httpResponse.isSuccessful();
        }
    }

    /**
     * Undeploy inference service.
     *
     * @param namespace            k8s namespace
     * @param inferenceServiceName name of the inference service
     * @throws ApiException exception
     */
    public boolean undeployInferenceService(String namespace, String inferenceServiceName)
            throws ApiException, IOException {
        var httpCall = customObjectsApi.deleteNamespacedCustomObjectCall(crdParams.getGroup(), crdParams.getVersion(),
                namespace, crdParams.getPlural(), inferenceServiceName, crdParams.getGracePeriod(), false, null, null,
                null, null);
        try (Response httpResponse = httpCall.execute()) {
            logger.debug("Response of inference service undeploy in namespace {} is {}", namespace, httpResponse);
            return httpResponse.isSuccessful();
        }
    }

    /**
     * Get the status of Inference service.
     *
     * @param namespace            k8s namespace
     * @param inferenceServiceName name of the inference service
     * @return State of the inference service
     * @throws ApiException exception on k8s client
     * @throws IOException  exception
     */
    public String getInferenceServiceStatus(String namespace, String inferenceServiceName)
            throws ApiException, IOException {
        var httpCall =
                customObjectsApi.getNamespacedCustomObjectCall(crdParams.getGroup(), crdParams.getVersion(), namespace,
                        crdParams.getPlural(), inferenceServiceName, null);
        try (Response httpResponse = httpCall.execute()) {
            logger.debug("Response of getting inference service in {} is {}", namespace, httpResponse);
            if (httpResponse.isSuccessful() && httpResponse.body() != null) {
                JsonNode jsonNode = new ObjectMapper().readTree(httpResponse.body().string());
                return jsonNode.at("/status/conditions/2/status").asText();
            }
        }
        return Boolean.FALSE.toString();
    }
}
