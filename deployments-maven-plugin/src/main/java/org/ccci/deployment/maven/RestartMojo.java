package org.ccci.deployment.maven;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ccci.deployment.ConfigurationException;
import org.ccci.deployment.Options;
import org.ccci.deployment.RestartDriver;

/**
 * Goal which restarts the appservers that host this application
 *
 * @goal restart
 * 
 */
public class RestartMojo extends AbstractDeploymentsMojo
{

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        setUpLogging();
        lookupPasswordIfNecessary();

        Options options = initializeOptions();
        
        RestartDriver driver;
        try
        {
            driver = new RestartDriver(options);
        }
        catch (ConfigurationException e)
        {
            throw buildFailureException(e);
        }
        
        driver.restart();
    }

    @Override
    protected void setMojoOptions(Options options) {
    }
}
