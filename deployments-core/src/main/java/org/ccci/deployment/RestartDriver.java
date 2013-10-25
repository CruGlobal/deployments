package org.ccci.deployment;

import org.apache.log4j.Logger;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.deployment.spi.WebappControlInterface;
import org.ccci.deployment.util.Waiter;

public class RestartDriver
{

    public RestartDriver(Options options)
    {
        this.configuration = options.application.buildDeploymentConfiguration(options);
        this.nonfatalExceptionBehavior = options.nonfatalExceptionBehavior;

        //TODO: timeout should be configurable
        this.cautiousShutdown = new CautiousShutdown(configuration, nonfatalExceptionBehavior, 45);
        this.initialAppCheck = new InitialAppCheck(configuration);
    }

    final Logger log = Logger.getLogger(RestartDriver.class);
    
    private final DeploymentConfiguration configuration;

    private final ExceptionBehavior nonfatalExceptionBehavior;

    private final CautiousShutdown cautiousShutdown;
    private final InitialAppCheck initialAppCheck;

    public void restart()
    {
        try
        {
            restartEachNode();
        }
        finally
        {
            configuration.closeResources();
        }

        log.info("restart process completed");
    }

    private void restartEachNode()
    {
        initialAppCheck.check();
        
        Waiter waiter = new Waiter(configuration.getWaitTimeBetweenNodes());
        for (Node node : configuration.listNodes())
        {
            waiter.waitIfNecessary();

            log.info("executing restart on " + node.getName());

            WebappControlInterface webappControlInterface = configuration.buildWebappControlInterface(node);
            cautiousShutdown.cautiouslyShutdownIfPossible(node, webappControlInterface);

            AppserverInterface appserverInterface = configuration.buildAppserverInterface(node);
            
            log.info("stopping app server");
            appserverInterface.shutdownServer();

            log.info("starting app server");
            appserverInterface.startupServer(nonfatalExceptionBehavior);

            log.info("verifying that newly deployed webapp is serving requests");
            webappControlInterface.verifyNewDeploymentActive();
            log.info("verified");

            log.info("restart completed on " + node.getName());
        }
    }

}
