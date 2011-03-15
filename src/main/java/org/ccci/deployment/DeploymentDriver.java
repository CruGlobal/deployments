package org.ccci.deployment;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.ccci.deployment.spi.Application;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.deployment.spi.DeploymentTransferInterface;
import org.ccci.deployment.spi.LoadbalancerInterface;
import org.ccci.deployment.spi.LocalDeploymentStorage;
import org.ccci.deployment.spi.WebappControlInterface;
import org.ccci.util.mail.EmailAddress;
import org.ccci.util.mail.MailMessage;
import org.ccci.util.mail.MailMessageFactory;
import org.ccci.util.strings.Strings;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class DeploymentDriver
{

    public enum ExceptionBehavior
    {
        PROPAGATE,
        SUPRESS;
    }


    public DeploymentDriver(Options options)
    {
        this.configuration = options.application.buildDeploymentConfiguration(options);
        this.application = options.application;
        this.environment = options.environment;
        this.continuousIntegrationUrl = options.continuousIntegrationUrl;
        this.factory = new MailMessageFactory("smtp1.ccci.org");
        this.restartType = options.restartType;
    }

    final Logger log = Logger.getLogger(DeploymentDriver.class);
    
    private final MailMessageFactory factory;
    private final Application application;
    private final DeploymentConfiguration configuration;
    private final String environment;
    private final String continuousIntegrationUrl;

    private RestartType restartType;
    

    //TODO: allow user to specify exceptionBehavior
    private ExceptionBehavior exceptionBehavior = ExceptionBehavior.PROPAGATE;
    
    public void deploy()
    {
        if (restartType == null)
            restartType = configuration.getDefaultRestartType();

        
        WebappDeployment deployment = configuration.buildWebappDeployment();
        sendNotificationEmail(deployment, exceptionBehavior, continuousIntegrationUrl);
        
        
        LocalDeploymentStorage localStorage = configuration.buildLocalDeploymentStorage();
        File deploymentLocation = localStorage.getDeploymentLocation();
        String type = deploymentLocation.isDirectory() ? "directory" : "file";
        log.info("deploying from " + type + " " + deploymentLocation);

        
        List<DeploymentTransferInterface> transferInterfaces = Lists.newArrayList();
        List<AppserverInterface> appserverInterfaces = Lists.newArrayList();
        try
        {
            deployToEachNode(deployment, localStorage, transferInterfaces, appserverInterfaces);
        }
        finally
        {
            configuration.closeResources();
        }

        log.info("deployment process completed");
    }

    private void deployToEachNode(
          WebappDeployment deployment, 
          LocalDeploymentStorage localStorage,
          List<DeploymentTransferInterface> transferInterfaces, 
          List<AppserverInterface> appserverInterfaces)
    {
        for (Node node : configuration.listNodes())
        {
            DeploymentTransferInterface transferInterface = configuration.connectDeploymentTransferInterface(node);
            transferInterfaces.add(transferInterface);
            
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
                removeNodeFromLoadbalancerService(webappControlInterface, node);
            }
            AppserverInterface appserverInterface = configuration.buildAppserverInterface(node);
            appserverInterfaces.add(appserverInterface);
            
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

            log.info("enabling new webapp");
            transferInterface.backupOldDeploymentAndActivateNewDeployment(deployment);
            
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
        //TODO: wait for for load balancer to stop sending requests
        
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
        int pauseTime = 10;
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

    private void sendNotificationEmail(
        WebappDeployment deployment, 
        ExceptionBehavior emailExceptionBehavior,
        String continuousIntegrationUrl)
    {
        MailMessage mailMessage = factory.createApplicationMessage();
        
        for (EmailAddress address : configuration.listDeploymentNotificationRecipients())
        {
            mailMessage.addTo(address);
        }
        
        List<Node> nodes = configuration.listNodes();
        String nodeDescription = Strings.join(nodes, ",", " and ");
        String subject = "deploying " + application.getName() + " to " + nodeDescription;
        
        String environmentDescription = Strings.capitalsAndUnderscoresToLabel(environment) ;
        String body = "This is an automated email notifying you that in a few seconds " + application.getName() + 
            " will be deployed to the "+ environmentDescription + " environment on " + nodeDescription + 
            (restartType == RestartType.FULL_PROCESS_RESTART ? ", and any associated appservers will be restarted" : "") +
            ".";
        
        if (continuousIntegrationUrl != null)
        {
            body += "\r\n" +
            "For more information, visit " +
            continuousIntegrationUrl;
        }
        
        mailMessage.setMessage(subject, body);
        mailMessage.setFrom(EmailAddress.valueOf("deployments-do-not-reply@ccci.org"));
        
        try
        {
            mailMessage.sendToAll();
        }
        catch (MessagingException e)
        {
            if (emailExceptionBehavior == ExceptionBehavior.PROPAGATE)
                throw Throwables.propagate(e);
            else if (emailExceptionBehavior == ExceptionBehavior.SUPRESS)
                log.error("unable to send email notification for deployment", e);
            else
                throw new AssertionError();
        }
    }

}
