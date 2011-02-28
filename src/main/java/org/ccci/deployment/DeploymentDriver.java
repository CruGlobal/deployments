package org.ccci.deployment;

import java.io.File;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.ccci.deployment.WebappDeployment.Packaging;
import org.ccci.util.NotImplementedException;
import org.ccci.util.mail.EmailAddress;
import org.ccci.util.mail.MailMessage;
import org.ccci.util.mail.MailMessageFactory;
import org.ccci.util.strings.Strings;

import com.google.common.base.Throwables;

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
        this.factory = new MailMessageFactory("smtp1.ccci.org");
    }

    final Logger log = Logger.getLogger(DeploymentDriver.class);
    
    private final MailMessageFactory factory;
    private final Application application;
    private final DeploymentConfiguration configuration;
    private final String environment;
    
    public void deploy()
    {
        //TODO: allow user to specify restartType
        RestartType restartType = configuration.getDefaultRestartType();

        //TODO: allow user to specify exceptionBehavior
        ExceptionBehavior exceptionBehavior = ExceptionBehavior.PROPAGATE;
        
        WebappDeployment deployment = configuration.buildWebappDeployment();
        sendNotificationEmail(deployment, exceptionBehavior);
        
        
        LocalDeploymentStorage localStorage = configuration.buildLocalDeploymentStorage();
        File deploymentLocation = localStorage.getDeploymentLocation();
        String type = deploymentLocation.isDirectory() ? "directory" : "file";
        log.info("deploying from " + type + " " + deploymentLocation);

        for (Node node : configuration.listNodes())
        {
            log.info("beginning deployment process for " + node.getName());
            //TODO: do this for both nodes, before transferring files at all
            DeploymentTransferInterface transferInterface = configuration.connectDeploymentTransferInterface(node);
            
            log.info("transferring new webapp to server");
            transferInterface.transferNewDeploymentToServer(deployment, localStorage);
            
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
            
            WebappControlInterface webappControlInterface = configuration.buildWebappControlInterface();
            webappControlInterface.enableForService();


            //TODO: figure out rollback logic
            
            log.info("deployment completed");
        }
    }

    private void sendNotificationEmail(WebappDeployment deployment, ExceptionBehavior emailExceptionBehavior)
    {
        MailMessage mailMessage = factory.createApplicationMessage();
        
        for (EmailAddress address : configuration.listDeploymentNotificationRecipients())
        {
            mailMessage.addTo(address);
        }
        
        List<Node> nodes = configuration.listNodes();
        String nodeDescription = Strings.join(nodes, ",", " and ");
        String subject = "deploying " + application.getName() + " to " + nodeDescription;
        
        if (deployment.getPackaging() != Packaging.EXPLODED)
            throw new NotImplementedException();
        String environmentDescription = Strings.capitalsAndUnderscoresToLabel(environment) ;
        String body = "This is an automated email notifying you that in a few seconds " + application.getName() + 
            " will be deployed to the "+ environmentDescription + " environment on " + nodeDescription + 
            ", and any associated appservers will be restarted.";
        
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
