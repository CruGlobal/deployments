package org.ccci.deployment.maven;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ccci.deployment.DeploymentDriver;

/**
 * Goal which deploys the configured application
 *
 * @goal deploy
 * 
 */
public class DeploymentsMojo
    extends AbstractMojo
{

    public void execute()
        throws MojoExecutionException
    {
        
        DeploymentDriver driver = null;
    }
}
