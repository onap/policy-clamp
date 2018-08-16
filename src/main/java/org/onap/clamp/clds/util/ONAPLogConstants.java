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
 *
 */

package org.onap.clamp.clds.util;

/**
 * Constants for standard ONAP headers, MDCs, etc.
 */
public final class ONAPLogConstants {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Constructors.
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Hide and forbid construction.
     */
    private ONAPLogConstants() {
        throw new UnsupportedOperationException();
    }


    /**
     * Marker constants.
     */
    public static final class Markers {

        /** Marker reporting invocation. */
        public static final String INVOKE = "INVOKE";

        /** Marker reporting synchronous invocation. */
        public static final String INVOKE_RETURN = "INVOKE-RETURN";

        /** Marker reporting synchronous invocation. */
        public static final String INVOKE_SYNC = "INVOKE-SYNCHRONOUS";

        /** Marker reporting entry into a component. */
        public static final String ENTRY = "ENTRY";

        /** Marker reporting exit from a component. */
        public static final String EXIT = "EXIT";

        /**
         * Hide and forbid construction.
         */
        private Markers() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * MDC name constants.
     */
    public static final class MDCs {

        // Tracing. ////////////////////////////////////////////////////////////

        /** MDC correlating messages for an invocation. */
        public static final String INVOCATION_ID = "InvocationID";

        /** MDC correlating messages for a logical transaction. */
        public static final String REQUEST_ID = "RequestID";

        /** MDC recording calling service. */
        public static final String PARTNER_NAME = "PartnerName";

        /** MDC recording current service. */
        public static final String SERVICE_NAME = "ServiceName";

        /** MDC recording target service. */
        public static final String TARGET_SERVICE_NAME = "TargetServiceName";

        /** MDC recording InvocationID Out. */
        public static final String INVOCATIONID_OUT = "InvocationIDOut";

        /** MDC recording target entity. */
        public static final String TARGET_ENTITY = "TargetEngity";

        /** MDC recording current service instance. */
        public static final String INSTANCE_UUID = "InstanceUUID";

        // Network. ////////////////////////////////////////////////////////////

        /** MDC recording caller address. */
        public static final String CLIENT_IP_ADDRESS = "ClientIPAddress";

        /** MDC recording server address. */
        public static final String SERVER_FQDN = "ServerFQDN";

        /**
         * MDC recording timestamp at the start of the current request,
         * with the same scope as {@link #REQUEST_ID}.
         *
         * <p>Open issues:
         * <ul>
         *     <ul>Easily confused with {@link #INVOKE_TIMESTAMP}.</ul>
         *     <ul>No mechanism for propagation between components, e.g. via HTTP headers.</ul>
         *     <ul>Whatever mechanism we define, it's going to be costly.</ul>
         * </ul>
         * </p>
         * */
        public static final String ENTRY_TIMESTAMP = "EntryTimestamp";

        /** MDC recording timestamp at the start of the current invocation. */
        public static final String INVOKE_TIMESTAMP = "InvokeTimestamp";

        // Outcomes. ///////////////////////////////////////////////////////////

        /** MDC reporting outcome code. */
        public static final String RESPONSE_CODE = "ResponseCode";

        /** MDC reporting outcome description. */
        public static final String RESPONSE_DESCRIPTION = "ResponseDescription";

        /** MDC reporting outcome error level. */
        public static final String RESPONSE_SEVERITY = "Severity";

        /** MDC reporting outcome error level. */
        public static final String RESPONSE_STATUS_CODE = "StatusCode";

        // Unsorted. ///////////////////////////////////////////////////////////

        /**
         * Hide and forbid construction.
         */
        private MDCs() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Header name constants.
     */
    public static final class Headers {

        /** HTTP <tt>X-ONAP-RequestID</tt> header. */
        public static final String REQUEST_ID = "X-ONAP-RequestID";

        /** HTTP <tt>X-ONAP-InvocationID</tt> header. */
        public static final String INVOCATION_ID = "X-ONAP-InvocationID";

        /** HTTP <tt>X-ONAP-PartnerName</tt> header. */
        public static final String PARTNER_NAME = "X-ONAP-PartnerName";

        /**
         * Hide and forbid construction.
         */
        private Headers() {
            throw new UnsupportedOperationException();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Enums.
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Response success or not, for setting <tt>StatusCode</tt>.
     */
    public enum ResponseStatus {

        /** Success. */
        COMPLETED,

        /** Not. */
        ERROR,
    }
}
