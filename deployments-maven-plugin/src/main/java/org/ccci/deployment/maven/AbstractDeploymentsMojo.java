package org.ccci.deployment.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ccci.deployment.ApplicationLookup;
import org.ccci.deployment.BasicApplicationBuilder;
import org.ccci.deployment.Options;
import org.ccci.util.ConsoleUtil;
import org.ccci.util.logging.JuliToLog4jHandler;

import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * @author Matt Drees
 */
public abstract class AbstractDeploymentsMojo extends AbstractMojo {

    /**
     * The name of the environment variable that contains the password to use for deployment.
     *
     * @parameter expression="${deployments.passwordEnvironmentVariableName}"
     */
    protected String passwordEnvironmentVariableName;

    /**
     * The password to use for transfer & service restarts
     *
     * @parameter expression="${deployments.password}"
     */
    protected String password;

    /**
     * Whether to prompt for a password if a password is not already configured
     * (requires keyboard interaction)
     *
     * @parameter
     *   expression="${deployments.promptPassword}"
     *   default-value="true"
     */
    protected boolean promptPassword;

    /**
     * The username to use for for transfer & service restarts
     *
     * @parameter expression="${deployments.username}"
     */
    protected String username;

    /**
     * the Active Directory domain for the username, if this is a deployment to a windows machine
     *
     * @parameter expression="${deployments.domain}"
     */
    protected String domain;

    /**
     * which web application to deploy.
     * Optional if a default application is on the classpath.
     * This option is mutually exclusive with {@code #applicationConfiguration}.
     *
     * @parameter expression="${deployments.application}"
     */
    protected String application;

    /**
     * specifies the location of a yaml deployment configuration file.
     * This option is mutually exclusive with {@code #applicaton}.
     *
     * @parameter expression="${deployments.applicationConfiguration}"
     */
    protected File applicationConfiguration;


    /**
     * Which environment (dev, staging, production, etc) to deploy to. Each application defines its own set of possible environments.
     *
     * @required
     * @parameter expression="${deployments.environment}"
     */
    protected String environment;

    protected void setUpLogging()
    {
        org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
        root.setLevel(org.apache.log4j.Level.INFO);
        root.addAppender(new MavenPluginAppender(getLog()));

        reduceJInteropLogOutput();

        redirectJavaUtilLoggingToLog4j();
    }

    private void reduceJInteropLogOutput()
    {
        /* jinterop is very chatty... lots of INFO and WARN messages that are really debug info */
        org.apache.log4j.Logger jinteropLogger = org.apache.log4j.Logger.getLogger("org.jinterop");
        jinteropLogger.setLevel(org.apache.log4j.Level.ERROR);
    }

    private void redirectJavaUtilLoggingToLog4j()
    {
        Logger rootLogger = LogManager.getLogManager().getLogger("");

        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        Handler activeHandler = new JuliToLog4jHandler();
        activeHandler.setLevel(Level.ALL);

        rootLogger.addHandler(activeHandler);
        rootLogger.setLevel(Level.ALL);
    }

    protected Options initializeOptions() throws MojoFailureException
    {
        Options options = new Options();

        require(environment, "environment");

        options.username = username;
        options.password = password;
        options.domain = domain;
        options.environment = environment;
        setMojoOptions(options);

        if (application != null && applicationConfiguration != null)
        {
            throw new MojoFailureException(
                "parameters 'application' and 'applicationConfiguration' are mutually exclusive");
        }

        if (application != null)
        {
            try
            {
                options.application = new ApplicationLookup().lookupApplication(application);
            }
            catch (IllegalArgumentException e)
            {
                throw buildFailureException(e);
            }
        }

        else if (applicationConfiguration != null)
        {
            try
            {
                options.application = new BasicApplicationBuilder().buildFrom(applicationConfiguration);
            }
            catch (IllegalArgumentException e)
            {
                throw buildFailureException(e);
            }
        }

        try
        {
            options.initializeDefaults();
        }
        catch (IllegalStateException e)
        {
            throw buildFailureException(e);
        }
        return options;
    }

    protected abstract void setMojoOptions(Options options);


    protected void lookupPasswordIfNecessary() throws MojoFailureException, MojoExecutionException
    {
        if (password == null)
        {
            if (passwordEnvironmentVariableName != null)
            {
                getPasswordFromEnvironmentVariable();
            }
            else if (promptPassword)
            {
                getPasswordFromConsolePrompt();
            }
            else
            {
                getLog().warn("using an empty password");
            }
        }
    }

    private void getPasswordFromEnvironmentVariable() throws MojoFailureException
    {
        password = System.getenv(passwordEnvironmentVariableName);
        if (password == null)
        {
            throw new MojoFailureException("Unable to get password from environment variable " + passwordEnvironmentVariableName);
        }
    }

    private void getPasswordFromConsolePrompt() throws MojoExecutionException
    {
        try
        {
            password = ConsoleUtil.readPasswordFromInput();
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("unable to read password", e);
        }
    }

    protected MojoFailureException buildFailureException(Exception e) throws MojoFailureException
    {
        MojoFailureException mojoFailureException = new MojoFailureException(e.getMessage());
        mojoFailureException.initCause(e);
        throw mojoFailureException;
    }

    protected void require(Object parameter, String parameterName) throws MojoFailureException
    {
        if (parameter == null)
            throw new MojoFailureException(String.format("Parameter %s is required", parameterName));
    }

    public String getPasswordEnvironmentVariableName()
    {
        return passwordEnvironmentVariableName;
    }

    public void setPasswordEnvironmentVariableName(String passwordEnvironmentVariableName)
    {
        this.passwordEnvironmentVariableName = passwordEnvironmentVariableName;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getEnvironment()
    {
        return environment;
    }

    public void setEnvironment(String environment)
    {
        this.environment = environment;
    }

    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public String getApplication()
    {
        return application;
    }

    public void setApplication(String application)
    {
        this.application = application;
    }

    public boolean isPromptPassword()
    {
        return promptPassword;
    }

    public void setPromptPassword(boolean promptPassword)
    {
        this.promptPassword = promptPassword;
    }

    public File getApplicationConfiguration() {
        return applicationConfiguration;
    }

    public void setApplicationConfiguration(File applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
    }
}
