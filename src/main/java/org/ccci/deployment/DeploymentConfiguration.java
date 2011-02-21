package org.ccci.deployment;

import java.util.List;

public interface DeploymentConfiguration
{

    public AppserverInterface buildAppserverInterface(Node node);
    
    public DeploymentTransferInterface connectDeploymentTransferInterface(Node node);
    
    public LocalDeploymentStorage buildLocalDeploymentStorage();
    
    public WebappControlInterface buildWebappControlInterface();
    
    public WebappDeployment buildWebappDeployment();
    
    public RestartType getDefaultRestartType();

    public List<Node> listNodes();

    public boolean supportsCautiousShutdown();
}
