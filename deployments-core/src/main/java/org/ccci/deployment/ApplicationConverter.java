package org.ccci.deployment;


import org.ccci.deployment.spi.Application;

import com.beust.jcommander.IStringConverter;

public class ApplicationConverter implements IStringConverter<Application>
{
    @Override
    public Application convert(String code)
    {
        return new ApplicationLookup().lookupApplication(code);
    }
    
}