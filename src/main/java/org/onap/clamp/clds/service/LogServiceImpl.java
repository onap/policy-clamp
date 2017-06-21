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

package org.onap.clamp.clds.service;

import com.att.ajsc.camunda.core.AttCamundaHistoryEvent;
import com.att.ajsc.camunda.core.AttCamundaService;
import com.att.ajsc.logging.AjscEelfManager;
import com.att.eelf.configuration.EELFLogger;
import com.google.gson.Gson;
import org.onap.clamp.clds.common.LogMessages;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.impl.history.event.HistoricActivityInstanceEventEntity;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.Context;
import java.util.*;

@Service
public class LogServiceImpl implements LogService {
    private static final EELFLogger logger = AjscEelfManager.getInstance().getLogger(LogServiceImpl.class);

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Context
    private MessageContext context;

    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public LogServiceImpl() {
        // needed for instantiation
    }

    @Override
    public String logMessage(String logMessageText, String javamail, String springmail, String commonsmail) {
        logger.info("Value of contexxt : " + context);
        String convId = null;
        if (context != null) {
            convId = context.getHttpServletRequest().getHeader("X-CSI-ConversationId");
            if (convId == null) {
                convId = (String) context.getHttpServletRequest().getAttribute("X-CSI-ConversationId");
            }
            context.getHttpServletRequest().setAttribute("CALL_TYPE", "Testing");
            AttCamundaService.setHttpRequest(context.getHttpServletRequest());
        }
        // input variables to example camunda process
        Map<String, Object> variables = new HashMap<>();
        variables.put("logMessageText", logMessageText);
        if (convId != null) {
            variables.put("conversationId", convId);
        }

        // BEGIN - added for send mail testing
        // also added the following to the method signature: , @QueryParam("javamail") String javamail, @QueryParam("springmail") String springmail, @QueryParam("commonsmail") String commonsmail
        // if javamail parameter provided, assume it contains an email address.
        // use Java Mail to send an email from that address, to that address
        if (javamail != null && javamail.length() > 0) {
            variables.put("javamail", javamail);
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.sbc.com"); // eMail.setHostName
                Session session = Session.getInstance(props);
                MimeMessage msg = new MimeMessage(session);

                msg.setFrom(new InternetAddress(javamail)); //eMail.setFrom

                InternetAddress[] fromAddresses = {new InternetAddress(javamail)};
                msg.setReplyTo(fromAddresses); //eMail.addReplyTo
                msg.setSubject("test message using javax.mail"); //eMail.setSubject
                msg.setText(logMessageText); // eMail.setMsg

                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(javamail)); // eMail.addTo
                Transport.send(msg);
            } catch (MessagingException e) {
                logger.error(LogMessages.LOGSERVICE_EMAIL_ERROR, e);
            }
        }

        // if springmail parameter provided, assume it contains an email address.
        // use Spring Mail to send an email from that address, to that address
        if (springmail != null && springmail.length() > 0) {
            variables.put("springmail", springmail);
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            SimpleMailMessage smsg = new SimpleMailMessage();

            try {
                sender.setHost("smtp.sbc.com"); // eMail.setHostName
                smsg.setFrom(springmail); //eMail.setFrom
                smsg.setReplyTo(springmail); //eMail.addReplyTo
                smsg.setSubject("test message using spring mail"); //eMail.setSubject
                smsg.setText(logMessageText); // eMail.setMsg
                smsg.setTo(springmail); // eMail.addTo
                sender.send(smsg);
            } catch (MailException e) {
                logger.error(LogMessages.LOGSERVICE_EMAIL_ERROR, e);
            }
        }

        // if commonsmail parameter provided, assume it contains an email address.
        // use Apache Commons Mail to send an email from that address, to that address
        if (commonsmail != null && commonsmail.length() > 0) {
            variables.put("commonsmail", commonsmail);
            Email eMail = new SimpleEmail();
            try {
                eMail.setHostName("smtp.sbc.com");
                eMail.setFrom(commonsmail);
                eMail.addReplyTo(commonsmail);
                eMail.setSubject("test message using commons mail");
                eMail.setMsg(logMessageText);
                eMail.addTo(commonsmail);
                java.net.URL classUrl = this.getClass().getResource("com.sun.mail.util.TraceInputStream");
                if (classUrl != null) {
                    logger.info(LogMessages.LOGSERVICE_EMAIL_CLASS, classUrl.getFile());
                } else {
                    logger.info(LogMessages.LOGSERVICE_EMAIL_CLASS, classUrl.getFile());
                    logger.info(LogMessages.LOGSERVICE_EMAIL_CLASS_NULL);
                }
                eMail.send();
            } catch (Exception e) {
                logger.error(LogMessages.LOGSERVICE_EMAIL_ERROR, e);
            }
        }
        // END - added for send mail testing

        // execute example camunda process, log-message-wf
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("log-message-wf", variables);
        AttCamundaService.setHttpRequest(null);
        // return text message of what was done
        return "Started processDefinitionId=" + pi.getProcessDefinitionId() + ", processInstanceId=" + pi.getProcessInstanceId() + ", to log message: " + logMessageText;
    }

    @Override
    public String postLogMessage(String histEventList) {
        String message = "no logs Created";
        logger.info("value of history events:" + histEventList);
        Gson gson = new Gson();
        AttCamundaHistoryEvent attCamundaHistoryEvent = gson.fromJson(histEventList, AttCamundaHistoryEvent.class);
        if (attCamundaHistoryEvent != null && attCamundaHistoryEvent.getProcInstId() != null) {
            logger.info(LogMessages.PROCESS_INSTANCE_ID, attCamundaHistoryEvent.getProcInstId());
            if (context != null && context.getHttpServletRequest() != null && context.getHttpServletRequest().getAttribute("PERFORMANCE_TRACKER_BEAN") != null) {
                context.getHttpServletRequest().setAttribute("CALL_TYPE", "Testing");
                List<HistoricActivityInstance> histActInstList = historyService.createHistoricActivityInstanceQuery().processInstanceId(attCamundaHistoryEvent.getProcInstId()).list();

                if (histActInstList != null && histActInstList.size() > 0) {
                    for (HistoricActivityInstance currHistoricActivityInstance : histActInstList) {
                        if (currHistoricActivityInstance != null && currHistoricActivityInstance.getActivityName() != null && currHistoricActivityInstance.getStartTime() != null
                                && currHistoricActivityInstance.getEndTime() != null) {
                            logger.info("value of serviceTrack:" + currHistoricActivityInstance);
                            message = "Log Entry Created";
                            logger.info(message);
                        }
                    }
                }
                if (attCamundaHistoryEvent.getHistoryEventList() != null && attCamundaHistoryEvent.getHistoryEventList().size() > 0) {
                    List<HistoricActivityInstanceEventEntity> historyEventList = attCamundaHistoryEvent.getHistoryEventList();
                    for (HistoricActivityInstanceEventEntity actiEvent : historyEventList) {
                        //  resolve null pointer exception if actiEvent.getActivityName()
                        message = "Log Entry Created";
                    }
                }
            }
        }
        return message;
    }

    @Override
    public String createLogMessage(String startTime, String endTime, String serviceName) {
        String message = "no logs Created";

        if (context != null && context.getHttpServletRequest() != null && context.getHttpServletRequest().getAttribute("PERFORMANCE_TRACKER_BEAN") != null) {
            context.getHttpServletRequest().setAttribute("X-CSI-ClientApp", "AJSC-CSI~sdsds");
    		/*PerformanceTrackingBean trackingBean =(PerformanceTrackingBean) context.getHttpServletRequest().getAttribute("PERFORMANCE_TRACKER_BEAN");
    		PerformanceTracking.addInvokeServiceTrack(trackingBean,
					serviceName, Long.valueOf(startTime), Long.valueOf(endTime), "Completed",
					500, 1000) ;*/
            message = "Log Entry Created";
        }
        // return text message of what was done
        return message;
    }

    @Override
    public String createLogMessageUsingHistory(String procInstId, String histEventList) {
        String message = "no logs Created";
        logger.info("value of history events:" + histEventList);
        logger.info("value of events:" + histEventList + ":" + histEventList);
        if (context != null && context.getHttpServletRequest() != null && context.getHttpServletRequest().getAttribute("PERFORMANCE_TRACKER_BEAN") != null) {
            context.getHttpServletRequest().setAttribute("CALL_TYPE", "Testing");
            List<HistoricActivityInstance> histActInstList = historyService.createHistoricActivityInstanceQuery().processInstanceId(procInstId).list();

            if (histActInstList != null && histActInstList.size() > 0) {
                for (HistoricActivityInstance currHistoricActivityInstance : histActInstList) {
                    if (currHistoricActivityInstance != null && currHistoricActivityInstance.getActivityName() != null && currHistoricActivityInstance.getStartTime() != null
                            && currHistoricActivityInstance.getEndTime() != null) {
                        logger.info("value of serviceTrack:" + currHistoricActivityInstance);
                        message = "Log Entry Created";
                        logger.info(message);
                    }
                }
            }
        }
        return message;
    }

    @Override
    public String CreateHistLog(String procInstId) {
        String message = "no logs Created";
        if (context != null && context.getHttpServletRequest() != null && context.getHttpServletRequest().getAttribute("PERFORMANCE_TRACKER_BEAN") != null) {
            List<HistoricActivityInstance> histActInstList = historyService.createHistoricActivityInstanceQuery().processInstanceId(procInstId).list();

            if (histActInstList != null && histActInstList.size() > 0) {
                for (HistoricActivityInstance currHistoricActivityInstance : histActInstList) {
                    if (currHistoricActivityInstance != null && currHistoricActivityInstance.getActivityName() != null && currHistoricActivityInstance.getStartTime() != null
                            && currHistoricActivityInstance.getEndTime() != null) {
                        logger.info("value of serviceTrack:" + currHistoricActivityInstance);
                        context.getHttpServletRequest().setAttribute("X-CSI-ClientApp", "AJSC-CSI~sdsds");
                        message = "Log Entry Created";
                    }
                }
            }
        }
        return message;
    }

    private String getActivityInstanceState(int activityInstanceState) {
        String activityState = "Default";
        if (activityInstanceState == 1) {
            activityState = "Complete";
        } else if (activityInstanceState == 2) {
            activityState = "Cancelled";
        }
        return activityState;
    }
}
