package org.ccci.deployment.basic;

import org.ccci.deployment.DeploymentFileDescription;
import org.ccci.deployment.Version;
import org.ccci.deployment.WebappDeployment;
import org.ccci.util.NotImplementedException;

public class BasicWebappDeployment implements WebappDeployment
{

    private String name;
    private String deployedWarName;
    private Packaging packaging;
    private DeploymentFileDescription deploymentFileDescription;

    
    public void setName(String name)
    {
        this.name = name;
    }

    public void setDeployedWarName(String deployedWarName)
    {
        this.deployedWarName = deployedWarName;
    }

    public void setPackaging(Packaging packaging)
    {
        this.packaging = packaging;
    }

    public void setDeploymentFileDescription(DeploymentFileDescription deploymentFileDescription)
    {
        this.deploymentFileDescription = deploymentFileDescription;
    }

    @Override
    public Version getVersion()
    {
        throw new NotImplementedException();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getDeployedWarName()
    {
        return deployedWarName;
    }

    @Override
    public Packaging getPackaging()
    {
        return packaging;
    }

    @Override
    public DeploymentFileDescription getDeploymentFileDescriptor()
    {
        return deploymentFileDescription;
    }

}
