package org.ccci.deployment;

import java.io.File;

import org.apache.log4j.Logger;

public class DeploymentDriver
{

    private final Options options;
    
    public DeploymentDriver(Options options)
    {
        this.options = options;
    }

    Logger log = Logger.getLogger(DeploymentDriver.class);
    
    public void deploy()
    {
        DeploymentConfiguration configuration = options.application.buildDeploymentConfiguration(options);

        RestartType restartType = configuration.getDefaultRestartType();
        //TODO: allow user to specify restartType

        LocalDeploymentStorage localStorage = configuration.buildLocalDeploymentStorage();
        File deploymentLocation = localStorage.getDeploymentLocation();
        String type = deploymentLocation.isDirectory() ? "directory" : "file";
        log.info("deploying from " + type + " " + deploymentLocation);
        WebappDeployment deployment = configuration.buildWebappDeployment();

        for (Node node : configuration.listNodes())
        {
            log.info("beginning deployment process for " + node.getName());
            //TODO: do this for both nodes, before transferring files at all
            DeploymentTransferInterface transferInterface = configuration.connectDeploymentTransferInterface(node);
            
            if (configuration.supportsCautiousShutdown())
            {
                WebappControlInterface webappControlInterface = configuration.buildWebappControlInterface();
                log.info("disabling app");
                webappControlInterface.disableForUpgrade();
                //TODO: wait for for load balancer to stop sending requests
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

            log.info("transferring new webapp to temp storage");
            transferInterface.backupOldDeploymentAndActivateNewDeployment(deployment);
            
            log.info("enabling new webapp");
            transferInterface.transferNewDeploymentToServer(deployment, localStorage);
            
            
            if (restartType == RestartType.FULL_PROCESS_RESTART)
            {
                log.info("starting app server");
                appserverInterface.startupServer();
            }
            else
            {
                log.info("starting webapp");
                appserverInterface.startApplication(deployment);
            }
            
            WebappControlInterface webappControlInterface = configuration.buildWebappControlInterface();
            webappControlInterface.enableForService();

            
            //TODO: figure out rollback logic
        }
    }

}
