package org.ccci.deployment.spi;

import java.util.List;
import java.util.Set;

import org.ccci.deployment.Node;
import org.ccci.deployment.RestartType;
import org.ccci.deployment.WebappDeployment;
import org.ccci.util.mail.EmailAddress;

/**
 * This is conceptually similar to {@link Application}.  These might be merged.
 * 
 * @author Matt Drees
 */
public interface DeploymentConfiguration
{

    public AppserverInterface buildAppserverInterface(Node node);
    
    public DeploymentTransferInterface connectDeploymentTransferInterface(Node node);
    
    public LocalDeploymentStorage buildLocalDeploymentStorage();
    
    public WebappControlInterface buildWebappControlInterface(Node node);
    
    public WebappDeployment buildWebappDeployment();
    
    public RestartType getDefaultRestartType();

    /**
     * It's important to return a list of the *same* nodes, since they are used as keys in
     * maps
     */
    public List<Node> listNodes();

    public boolean supportsCautiousShutdown();

    public Set<EmailAddress> listDeploymentNotificationRecipients();

    public LoadbalancerInterface buildLoadBalancerInterface();

    void closeResources();

    public int getWaitTimeBetweenNodes();
}
