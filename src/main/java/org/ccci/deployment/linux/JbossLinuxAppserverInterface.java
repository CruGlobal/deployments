package org.ccci.deployment.linux;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.ccci.deployment.ExceptionBehavior;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.ssh.RemoteExecutionFailureException;
import org.ccci.ssh.SshSession;

import com.google.common.base.Throwables;

public class JbossLinuxAppserverInterface implements AppserverInterface
{
    private final SshSession session;
    private final String jbossBinDir;
    private final JbossJmxCredentials jmxCredentials;
    
    private Logger log = Logger.getLogger(getClass());
    
    public JbossLinuxAppserverInterface(SshSession sshSession, String jbossServerPath, JbossJmxCredentials jmxCredentials)
    {
        this.jmxCredentials = jmxCredentials;
        this.jbossBinDir = jbossServerPath + "/bin";
        this.session = sshSession;
    }

    @Override
    public void shutdownServer()
    {
        executeRemoteJbossServiceCommand("stop");
        
        //TODO: I don't think this is needed, with behavior of java service wrapper
        long startTime = System.currentTimeMillis();
        while(serviceIsRunning())
        {
            int maxWait = 30;
            if (System.currentTimeMillis() > startTime + TimeUnit.SECONDS.toMillis(maxWait))
            {
                throw new RuntimeException("service not stopped after " + maxWait + " seconds");
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
        
        try
        {
            String output = executeTwiddle("get", "jboss.system:type=Server", "Started");
            if (output.matches("Started=true\\s*"))
                return true;
            else if (output.matches("Started=false\\s*"))
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
    }

    private String executeTwiddle(String invocation, String jmxMbean, String arguments) throws IOException
    {
        return session.executeSingleCommand(
            //first cd to tmp directory so creation of twiddle.log won't fail due to permissions problems,
            //then execute twiddle.sh with appropriate parameters
            "cd /tmp; sudo -u jboss /usr/local/jboss/bin/twiddle.sh --user=" + jmxCredentials.getUser() +
            " --password=" + jmxCredentials.getPassword() +
            " " + invocation + " \"" + jmxMbean + "\" " + arguments);
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
                int maxWait = 180;
                if (System.currentTimeMillis() > startTime + TimeUnit.SECONDS.toMillis(maxWait))
                {
                    throw new RuntimeException("service not started after " + maxWait + " seconds");
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
            return session.executeSingleCommand("sudo -u jboss " + jbossBinDir + "/jboss.sh " + jbossServiceCommand);
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }
    
    @Override
    public void startApplication(WebappDeployment deployment)
    {
        try
        {
            executeTwiddle("invoke", "jboss.web.deployment:war=/" + deployment.getDeployedWarName(), "start");
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }


    @Override
    public void stopApplication(WebappDeployment deployment)
    {
        try
        {
            executeTwiddle("invoke", "jboss.web.deployment:war=/" + deployment.getDeployedWarName(), "stop");
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }

}
