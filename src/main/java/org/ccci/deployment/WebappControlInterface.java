package org.ccci.deployment;

public interface WebappControlInterface
{

    public void disableForUpgrade();
    
    public void validateNewDeploymentActive();
    
    public Version getCurrentVersion();
    
}
