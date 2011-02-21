package org.ccci.deployment;

import java.util.Set;


public class InvalidEnvironmentException extends ConfigurationException
{

    public InvalidEnvironmentException(String environment, Set<String> possibilities)
    {
        super(String.format(
            "invalid environment: '%s'.  This application supports the following environments: %s",
            environment,
            possibilities
        ));
    }

    private static final long serialVersionUID = 1L;
}
