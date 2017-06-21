package org.onap.clamp.clds.client;

import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;

import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.prop.Tca;
import org.onap.clamp.clds.model.refprop.RefProp;


/**
 * Delete Tca Policy via policy api. 
 * 
 *
 */
public class TcaPolicyDeleteDelegate implements JavaDelegate {
	// currently uses the java.util.logging.Logger like the Camunda engine
	private static final Logger logger = Logger.getLogger(TcaPolicyDeleteDelegate.class.getName());
	
	@Autowired
	private PolicyClient policyClient;
	
	/**
	 * Perform activity.  Delete Tca Policy via policy api.
	 * 
	 * @param execution
	 */
	public void execute(DelegateExecution execution) throws Exception {		

		ModelProperties prop = ModelProperties.create(execution);
		Tca tca = prop.getTca();
		if(tca.isFound()){
			prop.setCurrentModelElementId(tca.getId());
	
			String responseMessage = policyClient.deleteMicrosService(prop); 
			if(responseMessage != null)
			{
				execution.setVariable("tcaPolicyDeleteResponseMessage", responseMessage.getBytes());
			}
		}
	}
	
	
	
}
