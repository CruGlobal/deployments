package org.ccci.deployment;

public interface WebappControlInterface
{

    public void disableForUpgrade();
    
    public void enableForService();
    
    public Version getCurrentVersion();
    
}
