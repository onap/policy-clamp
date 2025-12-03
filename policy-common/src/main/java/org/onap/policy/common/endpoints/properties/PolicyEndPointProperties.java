/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2021 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2022,2023-2024 Nordix Foundation.
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
 */

package org.onap.policy.common.endpoints.properties;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PolicyEndPointProperties {

    /* Generic property suffixes */

    public static final String PROPERTY_MANAGED_SUFFIX = ".managed";
    public static final String PROPERTY_ALLOW_SELF_SIGNED_CERTIFICATES_SUFFIX = ".selfSignedCertificates";

    /* HTTP Server Properties */

    public static final String PROPERTY_HTTP_SERVER_SERVICES = "http.server.services";

    public static final String PROPERTY_HTTP_HOST_SUFFIX = ".host";
    public static final String PROPERTY_HTTP_PORT_SUFFIX = ".port";
    public static final String PROPERTY_HTTP_CONTEXT_URIPATH_SUFFIX = ".contextUriPath";

    public static final String PROPERTY_HTTP_AUTH_USERNAME_SUFFIX = ".userName";
    public static final String PROPERTY_HTTP_AUTH_PASSWORD_SUFFIX = ".password"; //NOSONAR
    public static final String PROPERTY_HTTP_AUTH_URIPATH_SUFFIX = ".authUriPath";

    public static final String PROPERTY_HTTP_FILTER_CLASSES_SUFFIX = ".filterClasses";
    public static final String PROPERTY_HTTP_REST_CLASSES_SUFFIX = ".restClasses";
    public static final String PROPERTY_HTTP_REST_PACKAGES_SUFFIX = ".restPackages";
    public static final String PROPERTY_HTTP_REST_URIPATH_SUFFIX = ".restUriPath";

    public static final String PROPERTY_HTTP_SERVLET_URIPATH_SUFFIX = ".servletUriPath";
    public static final String PROPERTY_HTTP_SERVLET_CLASS_SUFFIX = ".servletClass";
    public static final String PROPERTY_HTTP_PROMETHEUS_SUFFIX = ".prometheus";

    public static final String PROPERTY_HTTP_HTTPS_SUFFIX = ".https";
    public static final String PROPERTY_HTTP_SWAGGER_SUFFIX = ".swagger";
    public static final String PROPERTY_HTTP_SNI_HOST_CHECK_SUFFIX = ".sniHostCheck";

    public static final String PROPERTY_HTTP_SERIALIZATION_PROVIDER = ".serialization.provider";

    /* HTTP Client Properties */

    public static final String PROPERTY_HTTP_CLIENT_SERVICES = "http.client.services";

    public static final String PROPERTY_HTTP_URL_SUFFIX = PROPERTY_HTTP_CONTEXT_URIPATH_SUFFIX;

}
