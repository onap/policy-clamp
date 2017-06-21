package org.onap.clamp.clds;

import org.junit.BeforeClass;
import org.onap.clamp.clds.client.PolicyClient;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

/**
 * Created by j on 6/16/17.
 */
@ActiveProfiles("clamp-default")
public abstract class AbstractIT {

    @Autowired
    protected RefProp refProp;
    @Autowired
    protected PolicyClient policyClient;

    @BeforeClass
    public static void oneTimeSetUp() {
        System.setProperty("AJSC_CONF_HOME", System.getProperty("user.dir") + "/src/it/resources/");
        System.setProperty("CLDS_DCAE_URL", "http://localhost:13786/cl-dcae-services");
    }
}
