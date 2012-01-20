package org.ccci.deployment.spi;

import org.ccci.deployment.Version;

public interface WebappControlInterface
{

    public void disableForUpgrade();
    
    public void verifyNewDeploymentActive();
    
    public Version getCurrentVersion();
    
}
