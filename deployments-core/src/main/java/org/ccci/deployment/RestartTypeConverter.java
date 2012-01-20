package org.ccci.deployment;

import com.beust.jcommander.IStringConverter;

public class RestartTypeConverter implements IStringConverter<RestartType>
{
    public RestartType convert(String string)
    {
        for (RestartType type : RestartType.values())
        {
            if (type.code.equals(string))
                return type;
        }
        throw new IllegalArgumentException("invalid restart type: " + string);
    }
}