package org.ccci.deployment;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.ccci.deployment.DeploymentNotifier.Type;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.deployment.spi.DeploymentTransferInterface;
import org.ccci.deployment.spi.LoadbalancerInterface;
import org.ccci.deployment.spi.LocalDeploymentStorage;
import org.ccci.deployment.spi.WebappControlInterface;

import com.google.common.base.Throwables;

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
    }

    final Logger log = Logger.getLogger(DeploymentDriver.class);
    
    private final DeploymentConfiguration configuration;

    private final RestartType restartType;

    private final ExceptionBehavior nonfatalExceptionBehavior;

    private final DeploymentNotifier notifier;
    
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
        for (Node node : configuration.listNodes())
        {
            DeploymentTransferInterface transferInterface = configuration.connectDeploymentTransferInterface(node);
            
            log.info("transferring new webapp to " + node.getName());
            transferInterface.transferNewDeploymentToServer(deployment, localStorage);
        }
        
        boolean first = true;
        for (Node node : configuration.listNodes())
        {
            waitIfNecessary(first);
            first = false;
            
            log.info("executing restart on " + node.getName());
            DeploymentTransferInterface transferInterface = configuration.connectDeploymentTransferInterface(node);
            
            WebappControlInterface webappControlInterface = configuration.buildWebappControlInterface(node);
            if (configuration.supportsCautiousShutdown())
            {
                try
                {
                    removeNodeFromLoadbalancerService(webappControlInterface, node);
                }
                catch (RuntimeException e)
                {
                    if (nonfatalExceptionBehavior == ExceptionBehavior.LOG)
                    {
                        log.error("unable to remove " + node + " from loadbalancer service; continuing deployment", e);
                    }
                    else if (nonfatalExceptionBehavior == ExceptionBehavior.HALT)
                    {
                        throw e;
                    }
                    else
                        throw new AssertionError();
                }
            }
            
            AppserverInterface appserverInterface = configuration.buildAppserverInterface(node);
            
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

            log.info("replacing old deployment with new deployment");
            transferInterface.backupOldDeploymentAndActivateNewDeployment(deployment, nonfatalExceptionBehavior);
            
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
            
            log.info("verifying that newly deployed webapp is serving requests");
            webappControlInterface.verifyNewDeploymentActive();
            log.info("verified");

            log.info("deployment completed on " + node.getName());
        }
    }

    /*
     *  Informs the load balancer to stop sending requests to this node
     */
    private void removeNodeFromLoadbalancerService(WebappControlInterface webappControlInterface, Node restartingNode)
    {
        log.info("disabling app");
        webappControlInterface.disableForUpgrade();
        
        LoadbalancerInterface loadbalancerInterface = configuration.buildLoadBalancerInterface();
        
        int timeLimit = 30;
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeLimit);
        
        while (true)
        {
            Node activeNode = loadbalancerInterface.getActiveNode();
            if (! activeNode.equals(restartingNode))
            {
                return;
            }
            else
            {
                if (System.currentTimeMillis() > deadline)
                    throw new RuntimeException("load balancer did not remove " + activeNode + 
                        " from service within " + timeLimit + " seconds");
                try
                {
                    TimeUnit.SECONDS.sleep(1);
                }
                catch (InterruptedException e)
                {
                    throw Throwables.propagate(e);
                }
            }
        }
        
    }

    private void waitIfNecessary(boolean first)
    {
        int pauseTime = configuration.getWaitTimeBetweenNodes();
        if (!first)
        {
            log.info("waiting " + pauseTime + " seconds before restarting next node");
            try
            {
                TimeUnit.SECONDS.sleep(pauseTime);
            }
            catch (InterruptedException e)
            {
                throw Throwables.propagate(e);
            }
        }
    }

}
