package org.ccci.deployment.maven;


import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ccci.deployment.ApplicationLookup;
import org.ccci.deployment.ConfigurationException;
import org.ccci.deployment.Options;
import org.ccci.deployment.appserver.AppserverDeploymentDriver;
import org.ccci.util.ConsoleUtil;
import org.ccci.util.logging.JuliToLog4jHandler;

/**
 * Goal which updates the appservers
 *
 * @goal deploy-appserver
 * 
 */

public class DeployAppserverMojo
    extends AbstractDeploymentsMojo
{

    /**
     * the url for the continuous integration server running this deployment
     * 
     * @parameter expression="${deployments.continuousIntegrationUrl}" 
     */
    private String continuousIntegrationUrl;
    
    
    /**
     * which directory contains the application to deploy
     * 
     * @parameter expression="${deployments.sourceDirectory}" 
     */
    private File sourceDirectory;
    
    /**
     * The name of the installer script to use for appserver updates
     * 
     * @parameter expression="${deployments.installerScript}"
     */
    private String installerScript;
    
    
    
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        setUpLogging();
        lookupPasswordIfNecessary();
        Options options = initializeOptions();
        
        AppserverDeploymentDriver driver;
        try
        {
            driver = new AppserverDeploymentDriver(options);
        }
        catch (ConfigurationException e)
        {
            throw buildFailureException(e);
        }
        
        driver.deploy();
    }

    protected void setMojoOptions(Options options) {
        options.continuousIntegrationUrl = continuousIntegrationUrl;
        options.sourceDirectory = sourceDirectory;
        options.installerScript = installerScript;
    }


    public String getContinuousIntegrationUrl()
    {
        return continuousIntegrationUrl;
    }

    public void setContinuousIntegrationUrl(String continuousIntegrationUrl)
    {
        this.continuousIntegrationUrl = continuousIntegrationUrl;
    }

    public File getSourceDirectory()
    {
        return sourceDirectory;
    }

    public void setSourceDirectory(File sourceDirectory)
    {
        this.sourceDirectory = sourceDirectory;
    }

    public String getInstallerScript() {
        return installerScript;
    }

    public void setInstallerScript(String installerScript) {
        this.installerScript = installerScript;
    }
}
