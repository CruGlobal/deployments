package org.ccci.deployment;

import java.util.Set;

import com.google.common.collect.Sets;

public class DeploymentFileDescription
{

    private final Set<String> deploymentSpecificPaths = Sets.newHashSet();
    private Set<String> ignoredPaths = Sets.newHashSet();
    private String webInfLogDir;

    
    public Set<String> getDeploymentSpecificPaths()
    {
        return deploymentSpecificPaths;
    }

    public void setWebInfLogDir(String webInfLogDir)
    {
        this.webInfLogDir = webInfLogDir;
    }

    public String getWebInfLogDir()
    {
        return webInfLogDir;
    }

    public Set<String> getIgnoredPaths()
    {
        return ignoredPaths;
    }
    
}
