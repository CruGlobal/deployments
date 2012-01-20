package org.ccci.deployment;

import java.io.File;

import org.ccci.deployment.spi.LocalDeploymentStorage;

/*
 * This would probably be better implemented using Aether:
 * https://docs.sonatype.org/display/AETHER/Home
 */
public class MavenRepositoryLocalDeploymentStorage implements LocalDeploymentStorage
{

    private final String artifactId;
    private final String groupId;
    private final String version;

    public MavenRepositoryLocalDeploymentStorage(String artifactId, String groupId, String version)
    {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
    }

    @Override
    public File getDeploymentLocation()
    {
        String path = System.getProperty("user.home") + "/.m2/repository/" + toPath(groupId) + "/" + artifactId + "/" + version;
        File deploymentLocation = new File(path);
        if (!deploymentLocation.exists())
            throw new IllegalStateException(deploymentLocation + " does not exist");
        if (!deploymentLocation.isDirectory())
            throw new IllegalStateException(deploymentLocation + " is not a directory");
        return deploymentLocation;
    }

    private static String toPath(String groupId)
    {
        return groupId.replace('.', '/');
    }

}
