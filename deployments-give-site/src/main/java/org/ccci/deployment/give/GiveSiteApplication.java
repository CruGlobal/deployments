package org.ccci.deployment.give;

import java.util.Set;

import org.ccci.deployment.InvalidEnvironmentException;
import org.ccci.deployment.MissingParameterException;
import org.ccci.deployment.Options;
import org.ccci.deployment.spi.Application;
import org.ccci.deployment.spi.AppserverDeploymentConfiguration;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.windows.smb.ActiveDirectoryCredential;

import com.google.common.collect.Sets;


public class GiveSiteApplication implements Application
{
	@Override
    public DeploymentConfiguration buildDeploymentConfiguration(Options options)
    {
        GiveSiteEnvironment giveSiteEnvironment;
        try
        {
        	giveSiteEnvironment = GiveSiteEnvironment.valueOf(options.environment.toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            Set<String> possibilities = Sets.newHashSet();
            for (GiveSiteEnvironment possibility : GiveSiteEnvironment.values())
            {
                possibilities.add(possibility.toString().toLowerCase());
            }
            throw new InvalidEnvironmentException(options.environment, possibilities);
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
        
        return new GiveSiteDeploymentConfiguration(
            credential, 
            giveSiteEnvironment,
            options.sourceDirectory);
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
        return "Give Site";
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
