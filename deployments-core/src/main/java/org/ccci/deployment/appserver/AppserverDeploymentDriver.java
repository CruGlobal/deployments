package org.ccci.deployment.appserver;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.ccci.deployment.ConfigurationException;
import org.ccci.deployment.ExceptionBehavior;
import org.ccci.deployment.FailoverDatabaseControlInterface;
import org.ccci.deployment.Node;
import org.ccci.deployment.Options;
import org.ccci.deployment.linux.Jboss7JSWLinuxAppserverInterface;
import org.ccci.deployment.spi.Application;
import org.ccci.deployment.spi.AppserverDeploymentConfiguration;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.deployment.spi.DeploymentTransferInterface;
import org.ccci.deployment.spi.LoadbalancerInterface;
import org.ccci.deployment.spi.WebappControlInterface;
import org.ccci.ssh.SshSession;
import org.ccci.ssh.SshSession.AsUser;

import com.google.common.base.Throwables;

/**
 * The purpose of this class is to transfer a newly-configured jboss appserver
 * installation to the remote server(s) and to run the installer remotely.
 * 
 * @author Matt Drees
 */
public class AppserverDeploymentDriver
{

    final Logger log = Logger.getLogger(AppserverDeploymentDriver.class);
    
    private final Application application;
    
    private DeploymentConfiguration configuration;
    private File sourceDirectory;

    private final ExceptionBehavior nonfatalExceptionBehavior;
    
    private final String jbossInstallationPackedName;

    private String installerScriptName;

    private String stagingDirectory;

    private AppserverDeploymentConfiguration appserverDeploymentConfiguration;
    
    public AppserverDeploymentDriver(Options options)
    {
        application = options.application;
        
        configuration = application.buildDeploymentConfiguration(options);
        appserverDeploymentConfiguration = application.buildAppserverDeploymentConfiguration(options);
        installerScriptName = appserverDeploymentConfiguration.getInstallerScriptName();
        jbossInstallationPackedName = appserverDeploymentConfiguration.getInstallationFileName();
        stagingDirectory = appserverDeploymentConfiguration.getStagingDirectory();
        
        sourceDirectory = options.sourceDirectory;
        if (sourceDirectory == null)
        {
            throw new ConfigurationException("please specify 'sourceDirectory', containing the jboss installation archive to deploy");
        }

        this.nonfatalExceptionBehavior = options.nonfatalExceptionBehavior;
    }

    public void deploy()
    {
        try
        {
            String localFilePath = sourceDirectory + "/" + jbossInstallationPackedName;

            log.info("installation file: " + localFilePath);

            
            transferInstallationToNodes(localFilePath, stagingDirectory);

            boolean first = true;
            for (Node node : configuration.listNodes())
            {
                waitIfNecessary(first);
                first = false;
                
                WebappControlInterface webappControlInterface = configuration.buildWebappControlInterface(node);

                cautiouslyShutDownIfPossible(node, webappControlInterface);
                flushFailoverDataIfNecessary(node);
                updateJbossInstallation(stagingDirectory, node);
                verifyNewDeploymentServingRequests(webappControlInterface);
                prepareForFailover(node);
            }
        }
        finally
        {
            configuration.closeResources();
        }
        
        log.info("appserver deployment process completed");

    }

    private void transferInstallationToNodes(String localFilePath,
                                             String stagingDirectory)
    {
        for (Node node: configuration.listNodes())
        {
            log.info("transferring installation to " + node);
            
            DeploymentTransferInterface deploymentTransferInterface = configuration.connectDeploymentTransferInterface(node);
            
            deploymentTransferInterface.transferAppserverInstallationToServer(localFilePath, stagingDirectory, jbossInstallationPackedName);
        }
    }

    private void verifyNewDeploymentServingRequests(WebappControlInterface webappControlInterface)
    {
        log.info("verifying that newly deployed webapp is serving requests");
        webappControlInterface.verifyNewDeploymentActive();
        log.info("verified");
    }

    private void cautiouslyShutDownIfPossible(Node node, WebappControlInterface webappControlInterface)
            throws AssertionError
    {
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
    }

    private void flushFailoverDataIfNecessary(Node node)
    {
        FailoverDatabaseControlInterface failoverDatabaseControl = configuration.buildFailoverDatabaseControl(node);
        if (failoverDatabaseControl != null)
        {
            log.info("flushing failover database on " + node);
            try
            {
                failoverDatabaseControl.recoverDataFromFailoverDatabase();
            }
            catch (RuntimeException e)
            {
                if (nonfatalExceptionBehavior == ExceptionBehavior.LOG)
                {
                    log.error("unable to flush failover database for " + node + "; continuing deployment", e);
                }
                else if (nonfatalExceptionBehavior == ExceptionBehavior.HALT)
                {
                    throw e;
                }
                else
                    throw new AssertionError();
            }
        }
    }
    

    private void prepareForFailover(Node node)
    {
        FailoverDatabaseControlInterface failoverDatabaseControl = configuration.buildFailoverDatabaseControl(node);
        if (failoverDatabaseControl != null)
        {
            log.info("preparing failover database on " + node);
            try
            {
                failoverDatabaseControl.prepareDataForFailover();
            }
            catch (RuntimeException e)
            {
                if (nonfatalExceptionBehavior == ExceptionBehavior.LOG)
                {
                    log.error("unable to prepare data for failover for " + node + "; continuing deployment", e);
                }
                else if (nonfatalExceptionBehavior == ExceptionBehavior.HALT)
                {
                    throw e;
                }
                else
                    throw new AssertionError();
            }
        }
    }
    

    private void updateJbossInstallation(String stagingDirectory, Node node)
    {
        AppserverInterface appserverInterface = configuration.buildAppserverInterface(node);
        log.info("updating jboss installation on " + node);
        
        appserverInterface.updateInstallation(
            stagingDirectory, 
            jbossInstallationPackedName, 
            installerScriptName, 
            nonfatalExceptionBehavior);
        
    }
    
    /**
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
