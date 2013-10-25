package org.ccci.deployment.appserver;

import org.apache.log4j.Logger;
import org.ccci.deployment.CautiousShutdown;
import org.ccci.deployment.ConfigurationException;
import org.ccci.deployment.DeploymentNotifier;
import org.ccci.deployment.DeploymentNotifier.Type;
import org.ccci.deployment.ExceptionBehavior;
import org.ccci.deployment.FailoverDatabaseControlInterface;
import org.ccci.deployment.InitialAppCheck;
import org.ccci.deployment.Node;
import org.ccci.deployment.Options;
import org.ccci.deployment.spi.Application;
import org.ccci.deployment.spi.AppserverDeploymentConfiguration;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.deployment.spi.DeploymentTransferInterface;
import org.ccci.deployment.spi.WebappControlInterface;
import org.ccci.deployment.util.Waiter;

import java.io.File;

/**
 * The purpose of this class is to transfer a newly-configured appserver
 * installation to the remote server(s) and to run the appropriate installer remotely.
 * 
 * @author Matt Drees
 */
public class AppserverDeploymentDriver
{

    final Logger log = Logger.getLogger(AppserverDeploymentDriver.class);

    private final CautiousShutdown cautiousShutdown;
    private final InitialAppCheck initialAppCheck;

    private DeploymentConfiguration configuration;
    private File sourceDirectory;

    private final ExceptionBehavior nonfatalExceptionBehavior;
    
    private final String installationPackedName;

    private String installerScriptName;

    private String stagingDirectory;

    private final DeploymentNotifier notifier;
    
    public AppserverDeploymentDriver(Options options)
    {
        Application application = options.application;
        
        configuration = application.buildDeploymentConfiguration(options);
        AppserverDeploymentConfiguration appserverDeploymentConfiguration =
            application.buildAppserverDeploymentConfiguration(options);
        installerScriptName = appserverDeploymentConfiguration.getInstallerScriptName();
        installationPackedName = appserverDeploymentConfiguration.getInstallationFileName();
        stagingDirectory = appserverDeploymentConfiguration.getStagingDirectory();
        
        sourceDirectory = options.sourceDirectory;
        if (sourceDirectory == null)
        {
            throw new ConfigurationException("please specify 'sourceDirectory', containing the appserver installation archive to deploy");
        }

        this.nonfatalExceptionBehavior = options.nonfatalExceptionBehavior;
        this.notifier = new DeploymentNotifier(options, configuration);

        //TODO: timeout should be configurable
        this.cautiousShutdown = new CautiousShutdown(configuration, nonfatalExceptionBehavior, 45);
        this.initialAppCheck = new InitialAppCheck(configuration);
    }

    public void deploy()
    {
        try
        {
            initialAppCheck.check();

            notifier.sendNotificationEmail(Type.APPLICATION_SERVER);
            String localFilePath = sourceDirectory + "/" + installationPackedName;

            log.info("installation file: " + localFilePath);

            transferInstallationToNodes(localFilePath, stagingDirectory);

            Waiter waiter = new Waiter(configuration.getWaitTimeBetweenNodes());
            for (Node node : configuration.listNodes())
            {
                waiter.waitIfNecessary();
                WebappControlInterface webappControlInterface = configuration.buildWebappControlInterface(node);
                cautiousShutdown.cautiouslyShutdownIfPossible(node, webappControlInterface);
                flushFailoverDataIfNecessary(node);
                updateAppserverInstallation(stagingDirectory, node);
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
            
            deploymentTransferInterface.transferAppserverInstallationToServer(localFilePath, stagingDirectory, installationPackedName);
        }
    }

    private void verifyNewDeploymentServingRequests(WebappControlInterface webappControlInterface)
    {
        log.info("verifying that newly deployed webapp is serving requests");
        webappControlInterface.verifyNewDeploymentActive();
        log.info("verified");
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
    

    private void updateAppserverInstallation(String stagingDirectory, Node node)
    {
        AppserverInterface appserverInterface = configuration.buildAppserverInterface(node);
        log.info("updating jboss installation on " + node);
        
        appserverInterface.updateInstallation(
            stagingDirectory, 
            installationPackedName, 
            installerScriptName, 
            nonfatalExceptionBehavior);
        
    }

}
