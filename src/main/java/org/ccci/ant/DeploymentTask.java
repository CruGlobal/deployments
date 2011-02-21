package org.ccci.ant;

import java.io.File;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.ccci.deployment.ConfigurationException;
import org.ccci.deployment.DeploymentDriver;
import org.ccci.deployment.Application;
import org.ccci.deployment.Options;

public class DeploymentTask extends Task
{

    private String environment;
    private Application application;
    private File sourceDirectory;
    private String username;
    private String password;
    private String domain;

    @Override
    public void execute() throws BuildException
    {
        require(application, "application");
        require(environment, "environment");
        
        Options options = new Options();
        options.application = application;
        options.environment = environment;
        options.username = username;
        options.password = password;
        options.domain = domain;
        options.sourceDirectory = sourceDirectory;
        DeploymentDriver driver;
        try
        {
            driver = new DeploymentDriver(options);
        }
        catch (ConfigurationException e)
        {
            throw new BuildException(e.getMessage());
        }
        
        driver.deploy();
    }
    
    private void require(Object parameter, String parameterName)
    {
        if (parameter == null)
            throw new BuildException(String.format("Parameter %s is required", parameterName));
    }

    public void setEnvironment(String environment)
    {
        this.environment = environment;
    }
    
    public void setApplication(Application application)
    {
        this.application = application;
    }
    
    public void setSourceDirectory(File sourceDirectory)
    {
        this.sourceDirectory = sourceDirectory;
    }
    
}
