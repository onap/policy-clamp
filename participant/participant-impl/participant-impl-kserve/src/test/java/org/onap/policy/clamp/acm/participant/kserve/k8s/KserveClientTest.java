/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KserveClientTest {

    @MockitoSpyBean
    private KserveClient kserveClient;

    String namespace = "kserve-test";

    String inferenceServiceName = "sklearn-iris";

    final Call remoteCall = mock(Call.class);

    final CustomObjectsApi customObjectsApi = mock(CustomObjectsApi.class);

    @MockitoBean
    ApiClient apiClient;

    @BeforeAll
    void initialize() {
        kserveClient.customObjectsApi = customObjectsApi;
    }


    @Test
    void test_deployInferenceServiceValidResponse() throws IOException, ApiException {
        String jsonContent =
                "{\"apiVersion\": \"serving.kserve.io/v1beta1\",\"kind\": \"InferenceService\",\"metadata\": "
                        + "{\"name\": \"" + inferenceServiceName
                        + "\"},\"spec\": {\"predictor\": {\"model\":{\"modelFormat\": "
                        + "{\"name\": \"sklearn\"},\"storageUri\": "
                        + "\"gs://kfserving-examples/models/sklearn/1.0/model\"}}}}";

        var response = getResponse(HttpStatus.SC_OK);
        when(remoteCall.execute()).thenReturn(response);
        when(customObjectsApi.createNamespacedCustomObjectCall(any(), any(), any(), any(), any(), any(), any(), any(),
                any())).thenReturn(remoteCall);
        assertTrue(kserveClient.deployInferenceService(namespace, jsonContent));
    }

    @Test
    void test_deployInferenceServiceInvalidResponse() throws IOException, ApiException {
        String jsonContent =
                "{\"apiVersion\": \"serving.kserve.io/v1beta1\",\"kind\": \"InferenceService\",\"metadata\": "
                        + "{\"name\": \"" + inferenceServiceName
                        + "\"},\"spec\": {\"predictor\": {\"model\":{\"modelFormat\": "
                        + "{\"name\": \"sklearn\"},\"storageUri\": "
                        + "\"gs://kfserving-examples/models/sklearn/1.0/model\"}}}}";

        var response = getResponse(HttpStatus.SC_BAD_REQUEST);
        when(remoteCall.execute()).thenReturn(response);
        when(customObjectsApi.createNamespacedCustomObjectCall(any(), any(), any(), any(), any(), any(), any(), any(),
                any())).thenReturn(remoteCall);
        assertFalse(kserveClient.deployInferenceService(namespace, jsonContent));
    }

    @Test
    void test_deployInvalidInferenceService() throws IOException, ApiException {
        doThrow(new ApiException("Error in deploying the service")).when(kserveClient)
                .deployInferenceService(any(), any());
        assertThatThrownBy(() -> kserveClient.deployInferenceService(namespace, "")).isInstanceOf(ApiException.class);
    }

    @Test
    void test_undeployInferenceServiceValidResponse() throws IOException, ApiException {

        var response = getResponse(HttpStatus.SC_OK);
        when(remoteCall.execute()).thenReturn(response);
        when(customObjectsApi.deleteNamespacedCustomObjectCall(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(remoteCall);
        assertTrue(kserveClient.undeployInferenceService(namespace, inferenceServiceName));
    }

    @Test
    void test_undeployInferenceServiceInvalidResponse() throws IOException, ApiException {

        var response = getResponse(HttpStatus.SC_BAD_REQUEST);
        when(remoteCall.execute()).thenReturn(response);
        when(customObjectsApi.deleteNamespacedCustomObjectCall(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(remoteCall);
        assertFalse(kserveClient.undeployInferenceService(namespace, inferenceServiceName));
    }

    @Test
    void test_getInferenceServiceStatusValidResponse() throws IOException, ApiException {

        var response = getResponse(HttpStatus.SC_OK, getInferenceServiceResponseBody("True"));
        when(remoteCall.execute()).thenReturn(response);
        when(customObjectsApi.getNamespacedCustomObjectCall(any(), any(), any(), any(), any(), any())).thenReturn(
                remoteCall);
        assertEquals("True", kserveClient.getInferenceServiceStatus(namespace, inferenceServiceName));
    }

    @Test
    void test_getInferenceServiceStatusFalseResponse() throws IOException, ApiException {

        var response = getResponse(HttpStatus.SC_OK, getInferenceServiceResponseBody("False"));
        when(remoteCall.execute()).thenReturn(response);
        when(customObjectsApi.getNamespacedCustomObjectCall(any(), any(), any(), any(), any(), any())).thenReturn(
                remoteCall);
        assertEquals("False", kserveClient.getInferenceServiceStatus(namespace, inferenceServiceName));
    }

    @Test
    void test_getInferenceServiceStatusInvalidResponse() throws IOException, ApiException {

        var response = getResponse(HttpStatus.SC_BAD_REQUEST, "");
        when(remoteCall.execute()).thenReturn(response);
        when(customObjectsApi.getNamespacedCustomObjectCall(any(), any(), any(), any(), any(), any())).thenReturn(
                remoteCall);
        assertEquals("false", kserveClient.getInferenceServiceStatus(namespace, inferenceServiceName));
    }

    Response getResponse(int code) {
        return getResponse(code, "{}");
    }

    Response getResponse(int code, String body) {
        return new Response.Builder().request(new Request.Builder().url("http://test").build())
                       .protocol(Protocol.HTTP_1_1).code(code).message("")
                       .body(ResponseBody.create(body, MediaType.parse("application/json"))).build();
    }

    String getInferenceServiceResponseBody(String status) {
        return "{ \"apiVersion\": \"serving.kserve.io/v1beta1\", \"kind\": \"InferenceService\", \"spec\": "
                       + "{ \"predictor\": { \"model\": { \"modelFormat\": { \"name\": \"sklearn\" }, \"name\": \"\", "
                       + "\"resources\": {}, \"storageUri\": \"gs://kfserving-examples/models/sklearn/1.0/model\" } } "
                       + "}, \"status\": { \"address\": { \"url\": \"http://sklearn-iris.kserve-test.svc.cluster.local\""
                       + " }, \"components\": { \"predictor\": { \"latestCreatedRevision\": \"1\", \"url\": "
                       + "\"http://sklearn-iris-predictor-default-kserve-test.example.com\" } }, \"conditions\": [ "
                       + "{ \"lastTransitionTime\": \"2023-02-15T13:39:16Z\", \"status\": \"" + status
                       + "\", \"type\": "
                       + "\"IngressReady\" }, { \"lastTransitionTime\": \"2023-02-15T13:39:16Z\", \"status\": " + "\""
                       + status + "\", \"type\": \"PredictorReady\" }, { \"lastTransitionTime\": "
                       + "\"2023-02-15T13:39:16Z\", \"status\": \"" + status + "\", \"type\": \"Ready\" } ], "
                       + "\"modelStatus\": { \"copies\": { \"failedCopies\": 0, \"totalCopies\": 1 }, \"states\": "
                       + "{ \"activeModelState\": \"Loaded\", \"targetModelState\": \"Loaded\" }, \"transitionStatus\":"
                       + "\"UpToDate\" }, \"observedGeneration\": 1, \"url\": "
                       + " \"http://sklearn-iris-kserve-test.example.com\" } }";
    }

}
