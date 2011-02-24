package org.ccci.ant;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.ccci.deployment.ConfigurationException;
import org.ccci.deployment.DeploymentDriver;
import org.ccci.deployment.Application;
import org.ccci.deployment.Options;
import org.ccci.util.logging.JuliToLog4jHandler;

public class DeploymentTask extends Task
{

    private String environment;
    private Application application;
    private File sourceDirectory;
    private String username;
    private String password;
    private String domain;

    @Override
    public void execute()
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
        
        setUpLogging();
        
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
    

    private static void setUpLogging()
    {
        org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
        root.setLevel(org.apache.log4j.Level.INFO);
        
        redirectJavaUtilLoggingToLog4j();
    }

    private static void redirectJavaUtilLoggingToLog4j()
    {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        
        Handler activeHandler = new JuliToLog4jHandler();
        activeHandler.setLevel(Level.ALL);
        
        rootLogger.addHandler(activeHandler);
        rootLogger.setLevel(Level.ALL);
    }
    
    public void setEnvironment(String environment)
    {
        this.environment = environment;
    }
    
    public void setApplication(String application)
    {
        try
        {
            this.application = Application.valueOf(application.toUpperCase().replace(" ", "_"));
        }
        catch (IllegalArgumentException e)
        {
            throw new BuildException();
        }
    }
    
    public void setSourceDirectory(File sourceDirectory)
    {
        this.sourceDirectory = sourceDirectory;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }
    
}
