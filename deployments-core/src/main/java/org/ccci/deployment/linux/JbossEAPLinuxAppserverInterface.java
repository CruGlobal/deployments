package org.ccci.deployment.linux;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import org.ccci.deployment.ExceptionBehavior;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.ssh.SshSession;

import java.io.IOException;

/**
 * For a Jboss EAP application server that's running as a System V service.
 */
public class JbossEAPLinuxAppserverInterface implements AppserverInterface
{
    private final SshSession session;
    private final String serviceName;
    private final String runtimeUser;
    private final String jbossHome;
    private final ZipScriptInstaller zipScriptInstaller;

    private Logger log = Logger.getLogger(getClass());

    public JbossEAPLinuxAppserverInterface(
        SshSession sshSession,
        String runtimeUser,
        String serviceName,
        String jbossHome)
    {
        this.session = sshSession;
        this.serviceName = serviceName;
        this.runtimeUser = runtimeUser;
        this.jbossHome = jbossHome;
        this.zipScriptInstaller = new ZipScriptInstaller(session, runtimeUser, this);
    }

    @Override
    public void shutdownServer()
    {
        executeRemoteServiceCommand("stop");
    }

    @Override
    public void startupServer(ExceptionBehavior exceptionBehavior)
    {
        executeRemoteServiceCommand("start");
        // the service script blocks until the server is booted up
        // (by scanning the console for JBAS015874)
    }
    
    private String executeRemoteServiceCommand(String serviceCommand)
    {
        try
        {
            return session.executeSingleCommand(
                "sudo /sbin/service " + serviceName + " " + serviceCommand);
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }
    
    
    @Override
    public void startApplication(WebappDeployment deployment)
    {
        executeAdminShell("deploy --name=" + deployment.getDeployedWarName() + ".war");
    }



    @Override
    public void stopApplication(WebappDeployment deployment)
    {
        executeAdminShell("undeploy " + deployment.getDeployedWarName() + ".war --keep-content");
    }

    private void executeAdminShell(String command)
    {
        try
        {
            session.as(runtimeUser).executeSingleCommand(
                jbossHome + "/bin/jboss-cli.sh --connect command='" + command + "'");
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void updateInstallation(String stagingDirectory, 
                                   String jbossInstallationPackedName,
                                   String installerScriptName,
                                   ExceptionBehavior nonfatalExceptionBehavior)
    {
        try
        {
            zipScriptInstaller.install(
                stagingDirectory,
                jbossInstallationPackedName,
                installerScriptName,
                nonfatalExceptionBehavior);
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }


}
