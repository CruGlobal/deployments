package org.ccci.deployment;

import java.io.File;

import org.apache.log4j.Logger;
import org.ccci.deployment.DeploymentNotifier.Type;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.deployment.spi.DeploymentTransferInterface;
import org.ccci.deployment.spi.LocalDeploymentStorage;
import org.ccci.deployment.spi.WebappControlInterface;

import org.ccci.deployment.util.Waiter;

public class DeploymentDriver
{


    public DeploymentDriver(Options options)
    {
        this.configuration = options.application.buildDeploymentConfiguration(options);
        this.restartType = options.restartType != null ? 
                options.restartType : 
                configuration.getDefaultRestartType();
        this.nonfatalExceptionBehavior = options.nonfatalExceptionBehavior;
        this.notifier = new DeploymentNotifier(options, configuration);

        //TODO: timeout should be configurable
        this.cautiousShutdown = new CautiousShutdown(configuration, nonfatalExceptionBehavior, 45);
        this.initialAppCheck = new InitialAppCheck(configuration);
    }

    final Logger log = Logger.getLogger(DeploymentDriver.class);
    
    private final DeploymentConfiguration configuration;

    private final RestartType restartType;

    private final ExceptionBehavior nonfatalExceptionBehavior;

    private final DeploymentNotifier notifier;

    private final CautiousShutdown cautiousShutdown;
    private final InitialAppCheck initialAppCheck;

    public void deploy()
    {
        WebappDeployment deployment = configuration.buildWebappDeployment();
        notifier.sendNotificationEmail(Type.WEBAPP);
        
        LocalDeploymentStorage localStorage = configuration.buildLocalDeploymentStorage();
        File deploymentLocation = localStorage.getDeploymentLocation();
        String type = deploymentLocation.isDirectory() ? "directory" : "file";
        log.info("deploying from " + type + " " + deploymentLocation);

        try
        {
            deployToEachNode(deployment, localStorage);
        }
        finally
        {
            configuration.closeResources();
        }

        log.info("deployment process completed");
    }

    private void deployToEachNode(
          WebappDeployment deployment, 
          LocalDeploymentStorage localStorage)
    {
        initialAppCheck.check();

        for (Node node : configuration.listNodes())
        {
            DeploymentTransferInterface transferInterface = configuration.connectDeploymentTransferInterface(node);
            
            log.info("transferring new webapp to " + node.getName());
            transferInterface.transferNewDeploymentToServer(deployment, localStorage);
        }
        
        Waiter waiter = new Waiter(configuration.getWaitTimeBetweenNodes());
        for (Node node : configuration.listNodes())
        {
            waiter.waitIfNecessary();
            
            log.info("executing restart on " + node.getName());
            DeploymentTransferInterface transferInterface = configuration.connectDeploymentTransferInterface(node);
            
            WebappControlInterface webappControlInterface = configuration.buildWebappControlInterface(node);

            cautiousShutdown.cautiouslyShutdownIfPossible(node, webappControlInterface);

            AppserverInterface appserverInterface = configuration.buildAppserverInterface(node);

            stop(deployment, appserverInterface);

            log.info("replacing old deployment with new deployment");
            transferInterface.backupOldDeploymentAndActivateNewDeployment(deployment, nonfatalExceptionBehavior);

            start(deployment, appserverInterface);

            log.info("verifying that newly deployed webapp is serving requests");
            webappControlInterface.verifyNewDeploymentActive();
            log.info("verified");

            log.info("deployment completed on " + node.getName());
        }
    }

    private void start(WebappDeployment deployment, AppserverInterface appserverInterface) {
        if (restartType == RestartType.FULL_PROCESS_RESTART)
        {
            log.info("starting app server");
            appserverInterface.startupServer(nonfatalExceptionBehavior);
        }
        else
        {
            log.info("starting webapp");
            appserverInterface.startApplication(deployment);
        }
    }

    private void stop(WebappDeployment deployment, AppserverInterface appserverInterface) {
        if (restartType == RestartType.FULL_PROCESS_RESTART)
        {
            log.info("stopping app server");
            appserverInterface.shutdownServer();
        }
        else
        {
            log.info("stopping webapp");
            appserverInterface.stopApplication(deployment);
        }
    }


}
