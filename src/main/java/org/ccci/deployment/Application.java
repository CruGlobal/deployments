package org.ccci.deployment;

import java.util.Set;

import org.ccci.util.NotImplementedException;
import org.ccci.windows.deployment.impl.ActiveDirectoryCredential;
import org.ccci.windows.deployment.impl.StaffServicesDeploymentConfiguration;
import org.ccci.windows.deployment.impl.StaffServicesDeploymentConfiguration.StaffServicesEnvironment;
import org.testng.v6.Sets;

public enum Application
{

    STAFF_SERVICES {
        @Override
        public DeploymentConfiguration buildDeploymentConfiguration(Options options)
        {
            StaffServicesEnvironment staffServicesEnvironment;
            try
            {
                staffServicesEnvironment = StaffServicesEnvironment.valueOf(options.environment);
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
    },
    
    CREDIT_CARD_PROCESSING {
        @Override
        public DeploymentConfiguration buildDeploymentConfiguration(Options options)
        {
            throw new NotImplementedException();
        }
    };
    
    
    public abstract DeploymentConfiguration buildDeploymentConfiguration(Options options);

}
