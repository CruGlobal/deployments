package org.ccci.deployment.basic;

import org.ccci.deployment.Node;
import org.ccci.util.mail.EmailAddress;

import java.util.List;
import java.util.Set;

/**
 * @author Matt Drees
 */
public class BasicEnvironment {

    public final String serviceName;
    public final int port;
    public final String appserverBasePath;
    private final List<Node> nodes;
    private final Set<EmailAddress> deploymentSubscribers;

    public BasicEnvironment(
        String serviceName,
        int port,
        String appserverBasePath,
        List<Node> nodes,
        Set<EmailAddress> deploymentSubscribers) {

        this.serviceName = serviceName;
        this.port = port;
        this.appserverBasePath = appserverBasePath;
        this.nodes = nodes;
        this.deploymentSubscribers = deploymentSubscribers;
    }

    public List<Node> listNodes() {
        return nodes;
    }

    public Set<EmailAddress> getDeploymentSubscribers() {
        return deploymentSubscribers;
    }
}
