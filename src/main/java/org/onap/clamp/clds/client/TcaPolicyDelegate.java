package org.onap.clamp.clds.client;

import java.util.UUID;
import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;

import org.onap.clamp.clds.client.req.TcaMPolicyReq;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.prop.Tca;
import org.onap.clamp.clds.model.refprop.RefProp;


/**
 * Send Tca info to policy api. 
 * 
 *
 */
public class TcaPolicyDelegate implements JavaDelegate {
	// currently uses the java.util.logging.Logger like the Camunda engine
	private static final Logger logger = Logger.getLogger(TcaPolicyDelegate.class.getName());
	
	@Autowired
	private RefProp refProp;
	
	@Autowired PolicyClient policyClient;
	
	/**
	 * Perform activity.  Send Tca info to policy api.
	 * 
	 * @param execution
	 */
	public void execute(DelegateExecution execution) throws Exception {		
		String tcaPolicyRequestUuid = UUID.randomUUID().toString();
		execution.setVariable("tcaPolicyRequestUuid", tcaPolicyRequestUuid);

		ModelProperties prop = ModelProperties.create(execution);
		Tca tca = prop.getTca();
		if(tca.isFound()){
			String policyJson = TcaMPolicyReq.formatTca(refProp, prop);
			String responseMessage = policyClient.sendMicroService(policyJson, prop, tcaPolicyRequestUuid);
			if(responseMessage != null)
			{
				execution.setVariable("tcaPolicyResponseMessage", responseMessage.getBytes());		
			}
		}
	}
	
	
	
}
