package org.ccci.deployment.spi;

import org.ccci.deployment.WebappDeployment;

public interface AppserverInterface
{
    
    public void stopApplication(WebappDeployment deployment);
    
    public void startApplication(WebappDeployment deployment);

    public void shutdownServer();
    
    public void startupServer();
    
}
