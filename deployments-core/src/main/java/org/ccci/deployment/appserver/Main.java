package org.ccci.deployment.appserver;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.ccci.deployment.ConfigurationException;
import org.ccci.deployment.Options;
import org.ccci.util.ProgramFailureException;
import org.ccci.util.logging.JuliToLog4jHandler;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;


//this class re-uses some class from the deployments project (org.ccci:deployments) that weren't 
// really intended for this... but they work pretty well.  Maybe some alignment could be done.  For
// example, we reuse the Options class, even though most are not applicable.

//TODO: there's too much copy/paste between this and org.ccci.deployment.Main in org.ccci:deployments
public class Main
{

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Main.class);
    
    public static void main(String[] args)
    {
        try
        {
            setUpLogging();
            
            Options options = new Options();
            try
            {
                JCommander jCommander = new JCommander(options);
                if (args.length == 0 ||
                        (args.length == 1 && args[0].equals("help")))
                {
                    jCommander.usage();
                    return;
                }
                jCommander.parse(args);
            }
            catch (ParameterException e)
            {
                throw new ProgramFailureException(e.getMessage(), e);
            }
            try
            {
                options.initializeDefaults();
            }
            catch (IllegalStateException e)
            {
                throw new ProgramFailureException(e.getMessage(), e);
            }
            
            AppserverDeploymentDriver driver;
            try
            {
                driver = new AppserverDeploymentDriver(options);
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
            log.debug("full stacktrace:" , e);
            System.exit(1);
        }
        catch (RuntimeException e)
        {
            System.err.println("Deployment program failed");
            e.printStackTrace();
            System.exit(2);
        }
        
        
    }


    private static void setUpLogging()
    {
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
