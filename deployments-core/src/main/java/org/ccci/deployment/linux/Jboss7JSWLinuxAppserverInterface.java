package org.ccci.deployment.linux;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.ccci.deployment.ExceptionBehavior;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.ssh.RemoteExecutionFailureException;
import org.ccci.ssh.SshSession;
import org.ccci.ssh.SshSession.AsUser;

import com.google.common.base.Throwables;

public class Jboss7JSWLinuxAppserverInterface implements AppserverInterface
{
    private final SshSession session;
    private final String jswBinPath;
    private final String requiredUser;
    
    private Logger log = Logger.getLogger(getClass());
    private int startupWaitTime = 240;
    
    public Jboss7JSWLinuxAppserverInterface(
        SshSession sshSession, 
        String jswBinPath, 
        String requiredUser)
    {
        this.requiredUser = requiredUser;
        this.jswBinPath = jswBinPath;
        this.session = sshSession;
    }

    @Override
    public void shutdownServer()
    {
        executeRemoteJbossServiceCommand("stop");
        //JSW is pretty reliable at stopping the service; it will kill it if need be.
        //We don't need to check that the service actually stopped.
    }

    private boolean serviceIsRunning()
    {
        String status;
        try
        {
            status = executeRemoteJbossServiceCommand("status");
        }
        catch (RemoteExecutionFailureException e)
        // the 'status' command returns an exit code of 1 if the program is not running.
        {
            if (e.exitStatus == 1)
                return false;
            else throw e;
        }
        assert status.contains("is running (PID:");
        
        //TODO:
        return true;
        /*
        try
        {
            String output = executeTwiddle("get", "jboss.system:type=Server", "Started");
            if (output.matches("Started=true\\s*"))
                return true;
            else if (output.matches("Started=false\\s*"))
                return false;
            else if (output.contains("Connection refused"))
                return false;
            else 
                throw new IllegalStateException("unexpected output from status query: " + output);
        }
        catch (RemoteExecutionFailureException e)
        {
            if (e.exitStatus == 1)
                return false;
            else throw e;
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
        */
    }

    //TODO: twiddle doesn't exist anymore. hmm.
//    private String executeTwiddle(String invocation, String jmxMbean, String arguments) throws IOException
//    {
//        return session.executeSingleCommand(
//            //first cd to tmp directory so creation of twiddle.log won't fail due to permissions problems,
//            //then execute twiddle.sh with appropriate parameters
//            "cd /tmp; sudo -u jboss /usr/local/jboss/bin/twiddle.sh --user=" + jmxCredentials.getUser() +
//            " --password=" + jmxCredentials.getPassword() +
//            " " + invocation + " \"" + jmxMbean + "\" " + arguments);
//    }
    
    

    private void executeAdminShell(String command)
    {
        try
        {
            session.as(requiredUser).executeSingleCommand("/usr/local/jboss/bin/jboss-admin.sh --connect command='" + command + "'");
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }
    
    

    @Override
    public void startupServer(ExceptionBehavior exceptionBehavior)
    {
        executeRemoteJbossServiceCommand("start");
        long startTime = System.currentTimeMillis();
        try
        {
            while(!serviceIsRunning())
            {
                if (System.currentTimeMillis() > startTime + TimeUnit.SECONDS.toMillis(startupWaitTime))
                {
                    throw new RuntimeException("service not started after " + startupWaitTime + " seconds");
                }
                try
                {
                    TimeUnit.MILLISECONDS.sleep(100);
                }
                catch (InterruptedException e)
                {
                    //TODO: think about how to handle this right
                    throw Throwables.propagate(e);
                }
            }
        }
        catch (RuntimeException e)
        {
            if (exceptionBehavior == ExceptionBehavior.HALT)
                throw e;
            else if (exceptionBehavior == ExceptionBehavior.LOG)
            {
                log.error("unable to determine status of server after successfully issuing start command; proceeding", e);
                return;
            }
            else
                throw new AssertionError();
        }
    }
    
    private String executeRemoteJbossServiceCommand(String jbossServiceCommand)
    {
        try
        {
            return session.as(requiredUser).executeSingleCommand(jswBinPath + "/jboss.sh " + jbossServiceCommand);
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

    @Override
    public void updateInstallation(String stagingDirectory, 
                                   String jbossInstallationPackedName,
                                   String installerScriptName,
                                   ExceptionBehavior nonfatalExceptionBehavior)
    {
        try
        {
            AsUser asJboss = session.as(requiredUser);
            log.info("clearing old backup if necessary");
            asJboss.executeSingleCommand("rm -rf " + stagingDirectory + "/installation");
            asJboss.executeSingleCommand("rm -rf " + stagingDirectory + "/jboss-as-*");
            log.info("unzipping installation archive");
            /* '-q' means 'quiet', '-d' means target directory for extraction */
            asJboss.executeSingleCommand("unzip -q " + stagingDirectory + "/" + jbossInstallationPackedName + " -d " + stagingDirectory);
            log.info("stopping jboss");
            shutdownServer();
            log.info("running installer");
            asJboss.executeSingleCommand("sh " + stagingDirectory + "/installation/update_jboss_installation.sh");
            log.info("starting jboss");
            startupServer(nonfatalExceptionBehavior);
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }

    
}
