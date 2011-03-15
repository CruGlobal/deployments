package org.ccci.deployment.spi;

import java.util.ServiceLoader;

import org.ccci.deployment.ConfigurationException;
import org.ccci.deployment.Options;

/**
 * Responsible for setting a {@link DeploymentConfiguration} for an application.
 * 
 * Implementations of {@link Application} will be loaded via a {@link ServiceLoader}, so should
 * have a public no-argument constructor and should have a corresponding META-INF/services/org.ccci.deployment.Application
 * file on the classpath whose contents is the fully-qualified classname of the implementation.
 * 
 * @author Matt Drees
 */
public interface Application
{

    /**
     * Responsible for building a DeploymentConfiguration that will drive the deployment process.
     * @throws ConfigurationException if this application deployment cannot be configured with the given options
     */
    DeploymentConfiguration buildDeploymentConfiguration(Options options);

    /**
     * Returns the name of this application.  This serves as the name a user should specify when running
     * the deployer and as a label displayed in messages.
     */
    String getName();

    /**
     * If true, then this application will be selected for deployment when the user does not specify one
     */
    boolean isDefault();

}
