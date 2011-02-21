package org.ccci.deployment;

public class MissingParameterException extends ConfigurationException
{

    public MissingParameterException(String parameterName)
    {
        super(String.format("required parameter '%s' was not specified", parameterName));
    }

    private static final long serialVersionUID = 1L;

}
