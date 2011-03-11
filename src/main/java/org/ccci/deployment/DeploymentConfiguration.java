package org.ccci.deployment;

import java.util.List;
import java.util.Set;

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

    public List<Node> listNodes();

    public boolean supportsCautiousShutdown();

    public Set<EmailAddress> listDeploymentNotificationRecipients();
}
