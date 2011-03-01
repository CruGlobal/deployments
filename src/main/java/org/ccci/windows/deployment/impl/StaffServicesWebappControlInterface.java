package org.ccci.windows.deployment.impl;

import org.ccci.deployment.Version;
import org.ccci.deployment.WebappControlInterface;

public class StaffServicesWebappControlInterface implements WebappControlInterface
{

    @Override
    public void disableForUpgrade()
    {
    }

    @Override
    public void validateNewDeploymentActive()
    {
        
    }

    @Override
    public Version getCurrentVersion()
    {
        throw new UnsupportedOperationException();
    }

}
