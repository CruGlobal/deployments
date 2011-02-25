package org.ccci.deployment;

import java.util.Set;

import com.google.common.collect.Sets;

public class DeploymentFileDescription
{

    private final Set<String> deploymentSpecificPaths = Sets.newHashSet();
    private Set<String> ignoredPaths = Sets.newHashSet();
    private String logPath;

    
    public Set<String> getDeploymentSpecificPaths()
    {
        return deploymentSpecificPaths;
    }

    public void setLogPath(String webInfLogDir)
    {
        this.logPath = webInfLogDir;
    }

    public String getLogPath()
    {
        return logPath;
    }

    public Set<String> getIgnoredPaths()
    {
        return ignoredPaths;
    }
    
}
