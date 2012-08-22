package org.ccci.deployment;

import java.util.List;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.ccci.deployment.DeploymentNotifier.Type;
import org.ccci.deployment.spi.Application;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.util.mail.EmailAddress;
import org.ccci.util.mail.MailMessage;
import org.ccci.util.mail.MailMessageFactory;
import org.ccci.util.strings.Strings;

import com.google.common.base.Throwables;

public class DeploymentNotifier
{


    public enum Type
    {
        WEBAPP, APPLICATION_SERVER;

    }

    private final MailMessageFactory factory;
    private final DeploymentConfiguration configuration;
    private final Application application;
    private final String environment;
    private final RestartType restartType;
    private final String continuousIntegrationUrl;
    private final ExceptionBehavior nonfatalExceptionBehavior;
    private final Logger log = Logger.getLogger(getClass());

    public DeploymentNotifier(Options options, DeploymentConfiguration configuration)
    {
        this.configuration = configuration;
        this.factory = new MailMessageFactory("smtp1.ccci.org");
        this.application = options.application;
        this.environment = options.environment;
        this.continuousIntegrationUrl = options.continuousIntegrationUrl;
        this.restartType = options.restartType;
        this.nonfatalExceptionBehavior = options.nonfatalExceptionBehavior;
    }
    
    public void sendNotificationEmail(Type type)
    {
        MailMessage mailMessage = factory.createApplicationMessage();
        
        for (EmailAddress address : configuration.listDeploymentNotificationRecipients())
        {
            mailMessage.addTo(address);
        }
        
        List<Node> nodes = configuration.listNodes();
        String nodeDescription = Strings.join(nodes, ",", " and ");
        String subject;
        if (type == Type.WEBAPP)
        {
            subject = "deploying " + application.getName() + " to " + nodeDescription;
        }
        else if (type == Type.APPLICATION_SERVER)
        {
            subject = "deploying " + application.getName() + " appserver reconfiguration to " + nodeDescription;
        }
        else throw new AssertionError();
        
        String environmentDescription = Strings.capitalsAndUnderscoresToLabel(environment) ;
        String body;
        if (type == Type.WEBAPP)
        {
            body = "This is an automated email notifying you that in a few seconds " + application.getName() + 
                " will be deployed to the "+ environmentDescription + " environment on " + nodeDescription + 
                (restartType == RestartType.FULL_PROCESS_RESTART ? ", and any associated appservers will be restarted" : "") +
                ".";
        }
        else if (type == Type.APPLICATION_SERVER)
        {
            body = "This is an automated email notifying you that in a few seconds the application server(s) for " + application.getName() + 
                    " in the "+ environmentDescription + " environment on " + nodeDescription + " will be reconfigured and restarted.";
        }
        else throw new AssertionError();
        
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
            if (nonfatalExceptionBehavior == ExceptionBehavior.HALT)
                throw Throwables.propagate(e);
            else if (nonfatalExceptionBehavior == ExceptionBehavior.LOG)
                log.error("unable to send email notification for deployment", e);
            else
                throw new AssertionError();
        }
    }

}
