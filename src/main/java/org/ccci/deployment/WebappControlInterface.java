package org.ccci.deployment;

public interface WebappControlInterface
{

    public void disableForUpgrade();
    
    public void verifyNewDeploymentActive();
    
    public Version getCurrentVersion();
    
}
