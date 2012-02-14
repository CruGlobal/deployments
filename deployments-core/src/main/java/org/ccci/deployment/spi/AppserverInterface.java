package org.ccci.deployment.spi;

import org.ccci.deployment.ExceptionBehavior;
import org.ccci.deployment.WebappDeployment;

public interface AppserverInterface
{
    
    public void stopApplication(WebappDeployment deployment);
    
    public void startApplication(WebappDeployment deployment);

    public void shutdownServer();
    
    public void startupServer(ExceptionBehavior nonfatalExceptionBehavior);

    public void updateInstallation(String stagingDirectory, 
                                   String jbossInstallationPackedName,
                                   String installerScriptName, 
                                   ExceptionBehavior nonfatalExceptionBehavior);
    
}
