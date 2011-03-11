package org.ccci.deployment.ss;

import java.util.Set;

import org.ccci.deployment.Application;
import org.ccci.deployment.DeploymentConfiguration;
import org.ccci.deployment.InvalidEnvironmentException;
import org.ccci.deployment.MissingParameterException;
import org.ccci.deployment.Options;
import org.ccci.windows.deployment.impl.ActiveDirectoryCredential;
import org.testng.v6.Sets;

public class StaffServicesApplication implements Application
{

    @Override
    public DeploymentConfiguration buildDeploymentConfiguration(Options options)
    {
        StaffServicesEnvironment staffServicesEnvironment;
        try
        {
            staffServicesEnvironment = StaffServicesEnvironment.valueOf(options.environment.toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            Set<String> possibilities = Sets.newHashSet();
            for (StaffServicesEnvironment possibility : StaffServicesEnvironment.values())
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
        
        return new StaffServicesDeploymentConfiguration(
            credential, 
            staffServicesEnvironment,
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
        return "Staff Services";
    }

    @Override
    public boolean isDefault()
    {
        return false;
    }

}
