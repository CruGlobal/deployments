package org.ccci.deployment.ant;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.ccci.deployment.ApplicationConverter;
import org.ccci.deployment.ApplicationLookup;
import org.ccci.deployment.ConfigurationException;
import org.ccci.deployment.DeploymentDriver;
import org.ccci.deployment.Options;
import org.ccci.util.logging.JuliToLog4jHandler;
import org.ccci.util.strings.Strings;

public class DeploymentTask extends Task
{

    private final Options options = new Options();
    
    @Override
    public void execute()
    {
        setUpLogging();
        
        require(options.environment, "environment");
        
        try
        {
            options.initializeDefaults();
        }
        catch (IllegalStateException e)
        {
            throw new BuildException(e.getMessage(), e);
        }
        
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
        options.environment = environment;
    }
    
    public void setApplication(String application)
    {
        try
        {
            options.application = new ApplicationLookup().lookupApplication(application);
        }
        catch (IllegalArgumentException e)
        {
            throw new BuildException(e.getMessage());
        }
    }
    
    public void setSourceDirectory(File sourceDirectory)
    {
        options.sourceDirectory = sourceDirectory;
    }

    public void setUsername(String username)
    {
        options.username = username;
    }

    public void setPassword(String password)
    {
        options.password = password;
    }

    public void setDomain(String domain)
    {
        options.domain = domain;
    }
    
    public void setContinuousIntegrationUrl(String continuousIntegrationUrl)
    {
        if (!Strings.isEmpty(continuousIntegrationUrl))
            options.continuousIntegrationUrl = continuousIntegrationUrl;
    }
    
}
