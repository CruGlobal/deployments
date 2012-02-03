package org.ccci.deployment.maven;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ccci.deployment.DeploymentDriver;
import org.jboss.seam.util.Strings;

/**
 * Goal which deploys the configured application
 *
 * @goal deploy
 * 
 */
public class DeploymentsMojo
    extends AbstractMojo
{
    /**
     * The name of the environment variable that contains the password to use for deployment.
     * 
     * @parameter expression="${deployments.passwordEnvironmentVariableName}" 
     */
    private String passwordEnvironmentVariableName;

    public void execute()
        throws MojoExecutionException
    {
        
        
        String password;
        if (Strings.isEmpty(passwordEnvironmentVariableName))
        {
            getLog().warn("did not provide passwordEnvironmentVariableName");
            return;
        }
        else
        {
            password = System.getenv(passwordEnvironmentVariableName);
        }
        
        if (Strings.isEmpty(password))
        {
            getLog().warn("Unable to get password from $" + passwordEnvironmentVariableName);
        }
        else
        {
            getLog().info("got password; it is " + password.length() + " characters long");
        }
    }

    public String getPasswordEnvironmentVariableName()
    {
        return passwordEnvironmentVariableName;
    }

    public void setPasswordEnvironmentVariableName(String passwordEnvironmentVariableName)
    {
        this.passwordEnvironmentVariableName = passwordEnvironmentVariableName;
    }
    
    
}
