package org.ccci.deployment;

import org.apache.log4j.Logger;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.deployment.spi.WebappControlInterface;


/**
 * @author Matt Drees
 */
public class InitialAppCheck {


    private Logger log = Logger.getLogger(getClass());
    private final DeploymentConfiguration configuration;

    public InitialAppCheck(DeploymentConfiguration configuration) {
        this.configuration = configuration;
    }

    public void check() {
        for (Node node : configuration.listNodes())
        {
            log.info("checking app is up on " + node.getName());

            WebappControlInterface webappControlInterface = configuration.buildWebappControlInterface(node);
            webappControlInterface.verifyNewDeploymentActive();
        }
    }
}
