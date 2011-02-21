package org.ccci.deployment;

import java.io.File;

public class SpecificDirectoryDeploymentStorage implements LocalDeploymentStorage
{

    private final File sourceDirectory;

    public SpecificDirectoryDeploymentStorage(File sourceDirectory)
    {
        this.sourceDirectory = sourceDirectory;
    }

    @Override
    public File getDeploymentLocation()
    {
        return sourceDirectory;
    }

}
