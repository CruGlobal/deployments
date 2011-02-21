package org.ccci.deployment;

import java.util.Set;

import com.google.common.collect.Sets;

public class ConfigFileDescriptor
{


    private final Set<String> deploymentSpecificClasspathResources = Sets.newHashSet();
    private final Set<String> deploymentSpecificWebInfResources = Sets.newHashSet();
    private final Set<String> ignoredWebInfDirectories = Sets.newHashSet();

    
    
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
    

}
