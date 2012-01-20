package org.ccci.deployment;

import com.beust.jcommander.IStringConverter;

public class ExceptionBehaviorConverter implements IStringConverter<ExceptionBehavior>
{
    public ExceptionBehavior convert(String string)
    {
        for (ExceptionBehavior exceptionBehavior : ExceptionBehavior.values())
        {
            if (exceptionBehavior.toString().toLowerCase().equals(string))
                return exceptionBehavior;
        }
        throw new IllegalArgumentException("invalid restart type: " + string);
    }
}