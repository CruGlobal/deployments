package org.ccci.deployment.basic;

import org.ccci.deployment.InvalidEnvironmentException;
import org.ccci.deployment.MissingParameterException;
import org.ccci.deployment.Options;
import org.ccci.deployment.spi.Application;
import org.ccci.deployment.spi.AppserverDeploymentConfiguration;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.windows.smb.ActiveDirectoryCredential;

import java.util.Map;

/**
 * @author Matt Drees
 */
public class BasicApplication implements Application {

    private StaticConfig staticConfig;

    public BasicApplication(StaticConfig staticConfig) {
        this.staticConfig = staticConfig;
    }

    @Override
    public DeploymentConfiguration buildDeploymentConfiguration(Options options)
    {
        String environmentName = options.environment.toLowerCase();
        Map<String, BasicEnvironment> environments = staticConfig.getEnvironments();
        BasicEnvironment environment;
        if (environments.containsKey(environmentName))
        {
            environment = environments.get(environmentName);
        }
        else
        {
            throw new InvalidEnvironmentException(options.environment, environments.keySet());
        }

        require(options.username, "username");
        require(options.password, "password");
        require(options.sourceDirectory, "sourceDirectory");
        if (options.domain == null)
            options.domain = "NET";

        ActiveDirectoryCredential credential = new ActiveDirectoryCredential(
            options.domain,
            options.password,
            options.username);

        BasicDeploymentConfiguration deploymentConfiguration = new BasicDeploymentConfiguration();
        deploymentConfiguration.setCredential(credential);
        deploymentConfiguration.setEnvironment(environment);
        deploymentConfiguration.setSourceDirectory(options.sourceDirectory);
        deploymentConfiguration.setStaticConfig(staticConfig);


        return deploymentConfiguration;
    }

    private void require(Object parameter, String parameterName)
    {
        if (parameter == null)
        {
            throw new MissingParameterException(parameterName);
        }
    }

    @Override
    public String getName()
    {
        return staticConfig.getApplicationName();
    }

    @Override
    public boolean isDefault()
    {
        return false;
    }

    @Override
    public AppserverDeploymentConfiguration buildAppserverDeploymentConfiguration(Options options)
    {
        throw new UnsupportedOperationException();
    }

}
