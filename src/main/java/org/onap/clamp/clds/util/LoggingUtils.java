/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.jboss.logging.MDC;

public class LoggingUtils {

    /**
     * Set request related logging variables in thread local data via MDC
     * 
     * @param service
     *            Service Name of API (ex. "PUT template")
     * @param partner
     *            Partner name (client or user invoking API)
     */
    public static void setRequestContext(String service, String partner) {
        MDC.put("RequestId", "clds-" + UUID.randomUUID().toString());
        MDC.put("ServiceName", service);
        MDC.put("PartnerName", partner);
    }

    /**
     * Set time related logging variables in thread local data via MDC
     * 
     * @param beginTimeStamp
     *            Start time
     * @param endTimeStamp
     *            End time
     */
    public static void setTimeContext(Date beginTimeStamp, Date endTimeStamp) {
        String beginTime = "";
        String endTime = "";
        String elapsedTime = "";

        if (beginTimeStamp != null && endTimeStamp != null) {
            elapsedTime = String.valueOf(endTimeStamp.getTime() - beginTimeStamp.getTime());
            beginTime = generateTimestampStr(beginTimeStamp);
            endTime = generateTimestampStr(endTimeStamp);
        }

        MDC.put("BeginTimestamp", beginTime);
        MDC.put("EndTimestamp", endTime);
        MDC.put("ElapsedTime", elapsedTime);
    }

    /**
     * Set response related logging variables in thread local data via MDC
     * 
     * @param code
     *            Response code ("0" indicates success)
     * @param description
     *            Response description
     * @param className
     *            class name of invoking class
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
     * @param targetEntity
     *            Target entity (an external/sub component, for ex. "sdc")
     * @param targetServiceName
     *            Target service name (name of API invoked on target)
     */
    public static void setTargetContext(String targetEntity, String targetServiceName) {
        MDC.put("TargetEntity", targetEntity != null ? targetEntity : "");
        MDC.put("TargetServiceName", targetServiceName != null ? targetServiceName : "");
    }

    /**
     * Set error related logging variables in thread local data via MDC
     * 
     * @param code
     *            Error code
     * @param description
     *            Error description
     */
    public static void setErrorContext(String code, String description) {
        MDC.put("ErrorCode", code);
        MDC.put("ErrorDescription", description != null ? description : "");
    }

    private static String generateTimestampStr(Date timeStamp) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        TimeZone tz = TimeZone.getTimeZone("UTC");
        df.setTimeZone(tz);
        return df.format(timeStamp);
    }

}
