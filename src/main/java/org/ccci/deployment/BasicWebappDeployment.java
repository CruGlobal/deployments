package org.ccci.deployment;

import org.ccci.util.NotImplementedException;

public class BasicWebappDeployment implements WebappDeployment
{

    private String name;
    private String deployedWarName;
    private Packaging packaging;
    private ConfigFileDescriptor configFileDescriptor;

    
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

    public void setConfigFileDescriptor(ConfigFileDescriptor configFileDescriptor)
    {
        this.configFileDescriptor = configFileDescriptor;
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
    public ConfigFileDescriptor getConfigFileDescriptor()
    {
        return configFileDescriptor;
    }

}
