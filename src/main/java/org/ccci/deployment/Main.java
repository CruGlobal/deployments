package org.ccci.deployment;


import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;

public class Main
{
    
    public static class SystemConveter implements IStringConverter<Application>
    {
        @Override
        public Application convert(String string)
        {
            return Application.valueOf(string.toUpperCase());
        }
    }
    
    public static void main(String[] args)
    {
        Options options = new Options();
        new JCommander(options, args);
        
        DeploymentDriver driver;
        try
        {
            driver = new DeploymentDriver(options);
        }
        catch (ConfigurationException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
            throw new AssertionError("unreachable");
        }
        driver.deploy();
    }

}
