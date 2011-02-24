package org.ccci.deployment;

import java.util.Set;

import com.google.common.collect.Sets;

//TODO: this is ugly.  Find something nicer.
public class DeploymentFileDescription
{


    private final Set<String> deploymentSpecificClasspathResources = Sets.newHashSet();
    private final Set<String> deploymentSpecificWebInfResources = Sets.newHashSet();
    private final Set<String> ignoredWebInfDirectories = Sets.newHashSet();
    private String webInfLogDir;

    
    
    public Set<String> getDeploymentSpecificClasspathResources()
    {
        return deploymentSpecificClasspathResources;
    }
    
    public Set<String> getDeploymentSpecificWebInfResources()
    {
        return deploymentSpecificWebInfResources;
    }
    
    public Set<String> getIgnoredWebInfDirectories()
    {
        return ignoredWebInfDirectories;
    }

    public void setWebInfLogDir(String webInfLogDir)
    {
        this.webInfLogDir = webInfLogDir;
    }

    public String getWebInfLogDir()
    {
        return webInfLogDir;
    }
    
}
