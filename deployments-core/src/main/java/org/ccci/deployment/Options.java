package org.ccci.deployment;

import java.io.File;
import java.util.ServiceLoader;

import org.ccci.deployment.spi.Application;

import com.beust.jcommander.Parameter;
import com.google.common.base.Preconditions;

public class Options
{

    public void initializeDefaults()
    {
        if (application == null)
            loadDefaultApplication();
    }
    
    private void loadDefaultApplication()
    {
        ServiceLoader<Application> loader = ServiceLoader.load(Application.class);
    
        Application defaultApp = null;
        for (Application app : loader)
        {
            if (app.isDefault())
            {
                Preconditions.checkState(
                    defaultApp == null, 
                    "two default applications on classpath: %s and %s", 
                    defaultApp,
                    app);
                defaultApp = app;
            }
        }
        Preconditions.checkState(
            defaultApp != null,
            "no default application is on classpath");
        
        application = defaultApp;
    }
    
    @Parameter(
        names = {"--application", "-a"}, 
        description = "which web application to deploy.  Optional if a default application is on the classpath.", 
        converter = ApplicationConverter.class, 
        required = false)
    public Application application;
    
    @Parameter(
        names = {"--environment", "-e"}, 
        description = "which environment (dev, staging, production, etc) to deploy to. Each application defines its own set of possible environments", 
        required = true)
    public String environment;

    @Parameter(
        names = {"--sourceDirectory", "-s"}, 
        description = "which directory contains the application to deploy")
    public File sourceDirectory;
    
    @Parameter(
        names = {"--username", "-u"}, 
        description = "username with rights to copy files and restart services", 
        required = true)
    public String username;
    
    @Parameter(
        names = {"--password", "-p"}, 
        description = "password associated with username", 
        password = true)
    public String password;

    @Parameter(
        names = {"--domain", "-d"}, 
        description = "the Active Directory domain for the username, if this is a deployment to a windows machine")
    public String domain;

    @Parameter(
        names = {"--continuousIntegrationUrl", "-c"}, 
        description = "the url for the continuous integration server running this deployment")
    public String continuousIntegrationUrl;

    @Parameter(
        names = {"--version", "-v"}, 
        description = "the version of this application to deploy")
    public String version;

    @Parameter(
        names = {"--restartType", "-r"}, 
        description = "either 'full' which will restart the entire server jvm, " +
        		"or 'quick', which will unload/load the webapp without a jvm restart.  " +
        		"If not specified, use the application configuration's default",
		converter = RestartTypeConverter.class)
    public RestartType restartType;

    @Parameter(
        names = {"--jmxUser", "-ju"}, 
        description = "the JMX username configured within the application server")
    public String jmxUser;
    
    @Parameter(
        names = {"--jmxPassword", "-jp"}, 
        description = "the password associated with JMX user",
        password = true)
    public String jmxPassword;


    @Parameter(
        names = {"--nonfatalExceptionBehavior", "-eb"}, 
        description = "whether nonfatal exceptions should cause the deployment to fail or should be logged but not halt the deployment. possible values: 'halt', 'log'.  Default is 'halt'",
        converter = ExceptionBehaviorConverter.class)
    public ExceptionBehavior nonfatalExceptionBehavior = ExceptionBehavior.HALT;
}