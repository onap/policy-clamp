/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import org.slf4j.MDC;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 * This class handles the special info that appear in the log, like RequestID,
 * time context, ...
 */
public final class LoggingUtils {
	
	protected static final EELFLogger logger = EELFManager.getInstance().getLogger(LoggingUtils.class);

    private static final DateFormat DATE_FORMAT = createDateFormat();

    /**
     * Private constructor to avoid creating instances of util class.
     */
    private LoggingUtils() {
    }

    /**
     * Set request related logging variables in thread local data via MDC
     *
     * @param service Service Name of API (ex. "PUT template")
     * @param partner Partner name (client or user invoking API)
     */
    public static void setRequestContext(String service, String partner) {
        MDC.put("RequestId", UUID.randomUUID().toString());
        MDC.put("ServiceName", service);
        MDC.put("PartnerName", partner);
        //Defaulting to HTTP/1.1 protocol
        MDC.put("Protocol", "HTTP/1.1");
        try {
        	MDC.put("ServerFQDN", InetAddress.getLocalHost().getCanonicalHostName());
        	MDC.put("ServerIPAddress", InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
        	logger.error("Failed to initiate setRequestContext", e);
		}
    }

    /**
     * Set time related logging variables in thread local data via MDC.
     *
     * @param beginTimeStamp Start time
     * @param endTimeStamp End time
     */
    public static void setTimeContext(@NotNull Date beginTimeStamp, @NotNull Date endTimeStamp) {
        MDC.put("BeginTimestamp", generateTimestampStr(beginTimeStamp));
        MDC.put("EndTimestamp", generateTimestampStr(endTimeStamp));
        MDC.put("ElapsedTime", String.valueOf(endTimeStamp.getTime() - beginTimeStamp.getTime()));
    }

    /**
     * Set response related logging variables in thread local data via MDC.
     *
     * @param code Response code ("0" indicates success)
     * @param description Response description
     * @param className class name of invoking class
     */
    public static void setResponseContext(String code, String description, String className) {
        MDC.put("ResponseCode", code);
        MDC.put("StatusCode", code.equals("0") ? "COMPLETE" : "ERROR");
        MDC.put("ResponseDescription", description != null ? description : "");
        MDC.put("ClassName", className != null ? className : "");
    }

    /**
     * Set target related logging variables in thread local data via MDC
     *
     * @param targetEntity Target entity (an external/sub component, for ex. "sdc")
     * @param targetServiceName Target service name (name of API invoked on target)
     */
    public static void setTargetContext(String targetEntity, String targetServiceName) {
        MDC.put("TargetEntity", targetEntity != null ? targetEntity : "");
        MDC.put("TargetServiceName", targetServiceName != null ? targetServiceName : "");
    }

    /**
     * Set error related logging variables in thread local data via MDC.
     *
     * @param code Error code
     * @param description Error description
     */
    public static void setErrorContext(String code, String description) {
        MDC.put("ErrorCode", code);
        MDC.put("ErrorDescription", description != null ? description : "");
    }

    private static String generateTimestampStr(Date timeStamp) {
        return DATE_FORMAT.format(timeStamp);
    }

    /**
     * Get a previously stored RequestID for the thread local data via MDC. If
     * one was not previously stored, generate one, store it, and return that
     * one.
     *
     * @return A string with the request ID
     */
    public static String getRequestId() {
        String requestId;

        requestId = (String) MDC.get("RequestID");
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
            MDC.put("RequestId", requestId);
        }
        return requestId;
    }

    private static DateFormat createDateFormat() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
    }

}
