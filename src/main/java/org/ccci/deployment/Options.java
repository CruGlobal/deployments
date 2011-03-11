package org.ccci.deployment;

import java.io.File;
import java.util.ServiceLoader;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.google.common.base.Preconditions;

public class Options
{

    public static class ApplicationConverter implements IStringConverter<Application>
    {
        @Override
        public Application convert(String code)
        {
            ServiceLoader<Application> loader = ServiceLoader.load(Application.class);
            
            for (Application app : loader)
            {
                if (applicationNamed(app, code))
                {
                    return app;
                }
            }
            throw new IllegalArgumentException(String.format(
                "no application named %s is on classpath", 
                code));
        }
        
        private boolean applicationNamed(Application app, String code)
        {
            return app.getName().toUpperCase().equals(code.toUpperCase().replace('_', ' '));
        }
        
    }

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
            "no default application is on classpath", 
            defaultApp);
        
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
        description = "the Active Directory domain for the username, if this is a deployment to a windows machine", 
        required = true)
    public String domain;

    @Parameter(
        names = {"--continuousIntegrationUrl", "-c"}, 
        description = "the url for the continuous integration server running this deployment")
    public String continuousIntegrationUrl;
}