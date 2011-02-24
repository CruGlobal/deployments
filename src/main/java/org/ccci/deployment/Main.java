package org.ccci.deployment;


import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.ccci.util.ProgramFailureException;
import org.ccci.util.logging.JuliToLog4jHandler;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

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
        try
        {
            setUpLogging();
            
            Options options = new Options();
            try
            {
                new JCommander(options, args);
            }
            catch (ParameterException e)
            {
                throw new ProgramFailureException(e.getMessage(), e);
            }
            DeploymentDriver driver;
            try
            {
                driver = new DeploymentDriver(options);
            }
            catch (ConfigurationException e)
            {
                throw new ProgramFailureException(e.getMessage(), e);
            }
            driver.deploy();
        }
        catch (ProgramFailureException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void setUpLogging()
    {
//        BasicConfigurator.configure();
//        PropertyConfigurator.configure("log4j.properties");
        
        org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
        root.setLevel(org.apache.log4j.Level.INFO);
        
        redirectJavaUtilLoggingToLog4j();
    }

    private static void redirectJavaUtilLoggingToLog4j()
    {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        
        Handler activeHandler = new JuliToLog4jHandler();
        activeHandler.setLevel(Level.ALL);
        
        rootLogger.addHandler(activeHandler);
        rootLogger.setLevel(Level.ALL);
    }

}
