package org.ccci.deployment.maven;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ccci.deployment.ConfigurationException;
import org.ccci.deployment.DeploymentDriver;
import org.ccci.deployment.Options;

import java.io.File;

/**
 * Goal which deploys the configured application
 *
 * @goal deploy
 * 
 */
public class DeployMojo extends AbstractDeploymentsMojo
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
    
    
    
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        setUpLogging();
        lookupPasswordIfNecessary();

        Options options = initializeOptions();
        
        DeploymentDriver driver;
        try
        {
            driver = new DeploymentDriver(options);
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

}
