package org.ccci.deployment;

import org.apache.log4j.Logger;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.deployment.spi.WebappControlInterface;

import java.util.List;


/**
 * @author Matt Drees
 */
public class InitialAppCheck {


    private Logger log = Logger.getLogger(getClass());
    private final DeploymentConfiguration configuration;

    public InitialAppCheck(DeploymentConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Verifies we don't deploy to the first node when the second node is dead.
     * (unless the first node is also dead, in which case we're already experiencing downtime)
     */
    public void check() {
        List<Node> nodes = configuration.listNodes();

        if (nodes.size() < 2)
            return;

        boolean first = true;
        Exception nodeProblem = null;
        for (Node node : nodes) {
            log.info("checking app is up on " + node.getName());
            nodeProblem = checkNode(node);
            if (first) {
                if (nodeProblem != null) {
                    log.warn(node + " is down. Hopefully the deployment will resolve this.", nodeProblem);
                    return;
                }
                first = false;
            } else {
                if (nodeProblem == null)
                    return;
            }
        }

        throw new RuntimeException(
            "The first node is up, but the rest are down!  Better not deploy right now.",
            nodeProblem);
    }

    //TODO: this could be better.  No need to wait 2m or whatever if the node is down.
    private Exception checkNode(Node node) {
        try {
            WebappControlInterface webappControlInterface = configuration.buildWebappControlInterface(node);
            webappControlInterface.verifyNewDeploymentActive();
            return null;
        } catch (Exception e) {
            return e;
        }

    }
}
