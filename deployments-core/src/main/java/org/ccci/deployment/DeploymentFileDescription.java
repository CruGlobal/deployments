package org.ccci.deployment;

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * describes which files in an exploded deployment need to be treated specially
 *
 * Only applicable for {@link WebappDeployment.Packaging#EXPLODED} deployments
 */
public class DeploymentFileDescription
{

    private final Set<String> deploymentSpecificPaths = Sets.newHashSet();
    private Set<String> ignoredPaths = Sets.newHashSet();
    private String logPath;

    /**
     * These paths will not be transferred to the server, but they will be copied
     * from the old deployment to the new deployment. These should contain configuration
     * files and other deployment-specific files, such as file-based caches.
     */
    public Set<String> getDeploymentSpecificPaths()
    {
        return deploymentSpecificPaths;
    }

    public void setLogPath(String webInfLogDir)
    {
        this.logPath = webInfLogDir;
    }

    /**
     * This path will be moved (not copied) from the old deployment to the
     * new deployment.  This is so log files can be preserved, but the deployment
     * process will not copy log files, which would take a long time and use disk
     * space that we might not have.
     * 
     * If null, log file moving will not be performed.
     */
    public String getLogPath()
    {
        return logPath;
    }

    /**
     * These paths will not be transferred to the server.  These may be directories or files.
     */
    public Set<String> getIgnoredPaths()
    {
        return ignoredPaths;
    }
    
}
