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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import org.onap.clamp.authorization.AuthorizationController;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * This class handles the special info that appear in the log, like RequestID,
 * time context, ...
 */
public class LoggingUtils {
    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(LoggingUtils.class);

    private static final DateFormat DATE_FORMAT = createDateFormat();

    /** String constant for messages <tt>ENTERING</tt>, <tt>EXITING</tt>, etc. */
    private static final String EMPTY_MESSAGE = "";

    /** Logger delegate. */
    private EELFLogger mlogger;

    /** Automatic UUID, overrideable per adapter or per invocation. */
    private static UUID sInstanceUUID = UUID.randomUUID();

    /**
     * Constructor.
     */
    public LoggingUtils(final EELFLogger loggerP) {
        this.mlogger = checkNotNull(loggerP);
    }

    /**
     * Set request related logging variables in thread local data via MDC.
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
        MDC.put("EntryTimestamp", generateTimestampStr(beginTimeStamp));
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
        MDC.put("StatusCode", "0".equals(code) ? "COMPLETE" : "ERROR");
        MDC.put("ResponseDescription", description != null ? description : "");
        MDC.put("ClassName", className != null ? className : "");
    }

    /**
     * Set target related logging variables in thread local data via MDC.
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
        String requestId = MDC.get(OnapLogConstants.Mdcs.REQUEST_ID);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
            MDC.put(OnapLogConstants.Mdcs.REQUEST_ID, requestId);
        }
        return requestId;
    }

    private static DateFormat createDateFormat() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
    }

    /*********************************************************************************************
     * Method for ONAP Application Logging Specification v1.2
     ********************************************************************************************/

    /**
     * Report <tt>ENTERING</tt> marker.
     *
     * @param request non-null incoming request (wrapper)
     * @param serviceName service name
     */
    public void entering(HttpServletRequest request, String serviceName) {
        MDC.clear();
        checkNotNull(request);
        // Extract MDC values from standard HTTP headers.
        final String requestId = defaultToUuid(request.getHeader(OnapLogConstants.Headers.REQUEST_ID));
        final String invocationId = defaultToUuid(request.getHeader(OnapLogConstants.Headers.INVOCATION_ID));
        final String partnerName = defaultToEmpty(request.getHeader(OnapLogConstants.Headers.PARTNER_NAME));

        // Default the partner name to the user name used to login to clamp
        if (partnerName.equalsIgnoreCase(EMPTY_MESSAGE)) {
            MDC.put(OnapLogConstants.Mdcs.PARTNER_NAME,
                    AuthorizationController.getPrincipalName(SecurityContextHolder.getContext()));
        }

        // Set standard MDCs. Override this entire method if you want to set
        // others, OR set them BEFORE or AFTER the invocation of #entering,
        // depending on where you need them to appear, OR extend the
        // ServiceDescriptor to add them.
        MDC.put(OnapLogConstants.Mdcs.ENTRY_TIMESTAMP,
            ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT));
        MDC.put(OnapLogConstants.Mdcs.REQUEST_ID, requestId);
        MDC.put(OnapLogConstants.Mdcs.INVOCATION_ID, invocationId);
        MDC.put(OnapLogConstants.Mdcs.CLIENT_IP_ADDRESS, defaultToEmpty(request.getRemoteAddr()));
        MDC.put(OnapLogConstants.Mdcs.SERVER_FQDN, defaultToEmpty(request.getServerName()));
        MDC.put(OnapLogConstants.Mdcs.INSTANCE_UUID, defaultToEmpty(sInstanceUUID));

        // Default the service name to the requestURI, in the event that
        // no value has been provided.
        if (serviceName == null
                || serviceName.equalsIgnoreCase(EMPTY_MESSAGE)) {
            MDC.put(OnapLogConstants.Mdcs.SERVICE_NAME, request.getRequestURI());
        } else {
            MDC.put(OnapLogConstants.Mdcs.SERVICE_NAME, serviceName);
        }

        this.mlogger.info(OnapLogConstants.Markers.ENTRY);
    }

    /**
     * Report <tt>EXITING</tt> marker.
     *

     * @param code response code
     * @param descrption response description
     * @param severity response severity
     * @param status response status code
     */
    public void exiting(String code, String descrption, Level severity, OnapLogConstants.ResponseStatus status) {
        try {
            MDC.put(OnapLogConstants.Mdcs.RESPONSE_CODE, defaultToEmpty(code));
            MDC.put(OnapLogConstants.Mdcs.RESPONSE_DESCRIPTION, defaultToEmpty(descrption));
            MDC.put(OnapLogConstants.Mdcs.RESPONSE_SEVERITY, defaultToEmpty(severity));
            MDC.put(OnapLogConstants.Mdcs.RESPONSE_STATUS_CODE, defaultToEmpty(status));

            ZonedDateTime startTime = ZonedDateTime.parse(MDC.get(OnapLogConstants.Mdcs.ENTRY_TIMESTAMP),
                DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC));
            ZonedDateTime endTime = ZonedDateTime.now(ZoneOffset.UTC);
            MDC.put(OnapLogConstants.Mdcs.END_TIMESTAMP, endTime.format(DateTimeFormatter.ISO_INSTANT));
            long duration = ChronoUnit.MILLIS.between(startTime, endTime);
            MDC.put(OnapLogConstants.Mdcs.ELAPSED_TIMESTAMP, String.valueOf(duration)); 
            this.mlogger.info(OnapLogConstants.Markers.EXIT);
        }
        finally {
            MDC.clear();
        }
    }

    /**
     * Get the property value.
     *
     * @param name The name of the property
     * @return The value of the property
     */
    public String getProperties(String name) {
        return MDC.get(name);
    }

    /**
     * Report pending invocation with <tt>INVOKE</tt> marker,
     * setting standard ONAP logging headers automatically.
     *
     * @param con The HTTP url connection
     * @param targetEntity The target entity
     * @param targetServiceName The target service name
     * @return The HTTP url connection
     */
    public HttpURLConnection invoke(final HttpURLConnection con, String targetEntity, String targetServiceName) {
        return this.invokeGeneric(con, targetEntity, targetServiceName);
    }

    /**
     * Report pending invocation with <tt>INVOKE</tt> marker,
     * setting standard ONAP logging headers automatically.
     *
     * @param targetEntity The target entity
     * @param targetServiceName The target service name
     */
    public void invoke(String targetEntity, String targetServiceName) {
        final String invocationId = UUID.randomUUID().toString();

        invokeContext(targetEntity, targetServiceName, invocationId);

        // Log INVOKE*, with the invocationID as the message body.
        // (We didn't really want this kind of behavior in the standard,
        // but is it worse than new, single-message MDC?)
        this.mlogger.info(OnapLogConstants.Markers.INVOKE);
        this.mlogger.info(OnapLogConstants.Markers.INVOKE_SYNC + "{" + invocationId + "}");
    }

    /**
     * Report pending invocation with <tt>INVOKE</tt> marker,
     * setting standard ONAP logging headers automatically.
     *
     * @param con The HTTPS url connection
     * @param targetEntity The target entity
     * @param targetServiceName The target service name
     * @return The HTTPS url connection
     */
    public HttpsURLConnection invokeHttps(final HttpsURLConnection con, String targetEntity, String targetServiceName) {
        return this.invokeGeneric(con, targetEntity, targetServiceName);
    }

    /**
     * Report pending invocation with <tt>INVOKE-RETURN</tt> marker.
     */
    public void invokeReturn() {
        // Add the Invoke-return marker and clear the needed MDC
        this.mlogger.info(OnapLogConstants.Markers.INVOKE_RETURN);
        invokeReturnContext();
    }

    /**
     * Dependency-free nullcheck.
     *
     * @param in to be checked
     * @param <T> argument (and return) type
     * @return input arg
     */
    private static <T> T checkNotNull(final T in) {
        if (in == null) {
            throw new NullPointerException();
        }
        return in;
    }

    /**
     * Dependency-free string default.
     *
     * @param in to be filtered
     * @return input string or null
     */
    private static String defaultToEmpty(final Object in) {
        if (in == null) {
            return "";
        }
        return in.toString();
    }

    /**
     * Dependency-free string default.
     *
     * @param in to be filtered
     * @return input string or null
     */
    private static String defaultToUuid(final String in) {
        if (in == null) {
            return UUID.randomUUID().toString();
        }
        return in;
    }

    /**
     * Set target related logging variables in thread local data via MDC.
     *
     * @param targetEntity Target entity (an external/sub component, for ex. "sdc")
     * @param targetServiceName Target service name (name of API invoked on target)
     * @param invocationId The invocation ID
     */
    private void invokeContext(String targetEntity, String targetServiceName, String invocationId) {
        MDC.put(OnapLogConstants.Mdcs.TARGET_ENTITY, defaultToEmpty(targetEntity));
        MDC.put(OnapLogConstants.Mdcs.TARGET_SERVICE_NAME, defaultToEmpty(targetServiceName));
        MDC.put(OnapLogConstants.Mdcs.INVOCATIONID_OUT, invocationId);
        MDC.put(OnapLogConstants.Mdcs.INVOKE_TIMESTAMP,
            ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT));
    }

    /**
     * Clear target related logging variables in thread local data via MDC.
     */
    private void invokeReturnContext() {
        MDC.remove(OnapLogConstants.Mdcs.TARGET_ENTITY);
        MDC.remove(OnapLogConstants.Mdcs.TARGET_SERVICE_NAME);
        MDC.remove(OnapLogConstants.Mdcs.INVOCATIONID_OUT);
        MDC.remove(OnapLogConstants.Mdcs.INVOKE_TIMESTAMP);
    }

    private <T extends URLConnection> T invokeGeneric(final T con, String targetEntity, String targetServiceName) {
        final String invocationId = UUID.randomUUID().toString();

        // Set standard HTTP headers on (southbound request) builder.
        con.setRequestProperty(OnapLogConstants.Headers.REQUEST_ID,
                defaultToEmpty(MDC.get(OnapLogConstants.Mdcs.REQUEST_ID)));
        con.setRequestProperty(OnapLogConstants.Headers.INVOCATION_ID,
                invocationId);
        con.setRequestProperty(OnapLogConstants.Headers.PARTNER_NAME,
                defaultToEmpty(MDC.get(OnapLogConstants.Mdcs.PARTNER_NAME)));

        invokeContext(targetEntity, targetServiceName, invocationId);

        // Log INVOKE*, with the invocationID as the message body.
        // (We didn't really want this kind of behavior in the standard,
        // but is it worse than new, single-message MDC?)
        this.mlogger.info(OnapLogConstants.Markers.INVOKE);
        this.mlogger.info(OnapLogConstants.Markers.INVOKE_SYNC + "{" + invocationId + "}");
        return con;
    }
}
