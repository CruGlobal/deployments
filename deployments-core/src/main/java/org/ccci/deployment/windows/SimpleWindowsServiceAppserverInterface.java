package org.ccci.deployment.windows;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.ccci.deployment.ExceptionBehavior;
import org.ccci.deployment.Node;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.ssh.RemoteExecutionFailureException;
import org.ccci.util.NotImplementedException;
import org.ccci.windows.management.RemoteServiceControl;
import org.ccci.windows.smb.ActiveDirectoryCredential;
import org.ccci.windows.wscript.RemoteShell;
import org.jinterop.dcom.common.IJIAuthInfo;
import org.jinterop.dcom.common.JIDefaultAuthInfoImpl;
import org.jinterop.dcom.common.JIException;

import com.google.common.base.Throwables;

public class SimpleWindowsServiceAppserverInterface implements AppserverInterface
{

    private final String serviceName;
    private final Node node;
    private final ActiveDirectoryCredential activeDirectoryCredential;

    String unxUtilsLocation = "C:\\\"Program Files\"\\UnxUtils\\usr\\local\\wbin";
    
    private Logger log = Logger.getLogger(getClass());

    public SimpleWindowsServiceAppserverInterface(Node node, String serviceName, ActiveDirectoryCredential activeDirectoryCredential)
    {
        this.node = node;
        this.serviceName = serviceName;
        this.activeDirectoryCredential = activeDirectoryCredential;
    }

    @Override
    public void stopApplication(WebappDeployment deployment)
    {
        throw new NotImplementedException();
    }

    @Override
    public void startApplication(WebappDeployment deployment)
    {
        throw new NotImplementedException();
    }

    @Override
    public void shutdownServer()
    {
        RemoteServiceControl control = buildControl();
        try
        {
            control.stopService();
            
            long startTime = System.currentTimeMillis();
            while(!control.serviceIsStopped())
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
            
            //wait a small amount of additional time.  It seems even if the service is stopped, the files
            //the service uses are still locked for a short while.  I think.  
            try
            {
                TimeUnit.SECONDS.sleep(2);
            }
            catch (InterruptedException e)
            {
                //TODO: think about how to handle this right
                throw Throwables.propagate(e);
            }
            
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
        catch (JIException e)
        {
            throw Throwables.propagate(e);
        }
        finally
        {
            control.close();
        }
    }

    @Override
    public void startupServer(ExceptionBehavior exceptionBehavior)
    {
        RemoteServiceControl control = buildControl();
        
        try
        {
            control.startOrRestartService();
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
        catch (JIException e)
        {
            throw Throwables.propagate(e);
        }
        finally
        {
            control.close();
        }
    }

    private RemoteServiceControl buildControl()
    {
        IJIAuthInfo credential = makeIJIAuthInfo();
        RemoteServiceControl control = new RemoteServiceControl(node.getHostname(), credential, serviceName);
        return control;
    }

    private IJIAuthInfo makeIJIAuthInfo()
    {
        IJIAuthInfo credential = new JIDefaultAuthInfoImpl(
            activeDirectoryCredential.getDomain(), 
            activeDirectoryCredential.getUsername(), 
            activeDirectoryCredential.getPassword());
        return credential;
    }


    @Override
    public void updateInstallation(String stagingDirectory, 
                                   String installationPackedName,
                                   String installerScriptName, 
                                   ExceptionBehavior nonfatalExceptionBehavior)
    {
        IJIAuthInfo credential = makeIJIAuthInfo();
        RemoteShell remoteShell = new RemoteShell(node.getHostname(), credential, TimeUnit.MINUTES.toMillis(2));
        
        try
        {
            String stagingDirectoryWinPath = convertToWindowsPath(stagingDirectory);
            cleanOldInstallations(remoteShell, stagingDirectoryWinPath);
            log.info("unzipping installation archive");
            /* '-q' means 'quiet', '-d' means target directory for extraction */
            remoteShell.executeSingleCommand(unxUtilsLocation + "\\unzip -q "  + stagingDirectoryWinPath + "\\" + installationPackedName + " -d " + stagingDirectoryWinPath);
            log.info("stopping jboss");
            try
            {
                shutdownServer();
            }
            catch (IllegalArgumentException e){
                if(e.getMessage().contains("no such service"))
                    log.warn("Trying to shutdown non-existant service *** " + e.getMessage());
                else
                    throw Throwables.propagate(e);
            }
            log.info("running installer (" + installerScriptName + ")");
            String output = remoteShell.executeSingleCommand(stagingDirectoryWinPath + "\\installation\\" + installerScriptName);
            log.info("installer output follows:");
            log.info(output);
            log.info("starting jboss");
            startupServer(nonfatalExceptionBehavior);
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }

    private void cleanOldInstallations(RemoteShell remoteShell, String stagingDirectoryWinPath) throws IOException
    {
        log.info("clearing old backup if necessary");
        try
        {
            remoteShell.executeSingleCommand("rd /s /q " + stagingDirectoryWinPath + "\\installation");
        }
        catch (RemoteExecutionFailureException e)
        {
            if (e.exitStatus == 2)
            {
                //directory doesn't exist; ignore
            }
            else throw e;
        }
        //using unixUtil's rm since windows' rd can't handle wildcards
        remoteShell.executeSingleCommand(unxUtilsLocation + "\\rm --recursive --force " + stagingDirectoryWinPath + "\\jboss-as-*");
    }

    String convertToWindowsPath(String stagingDirectory)
    {
        //turn W$/tmp/wsapi-jboss-installation-work to W:\tmp\wsapi-jboss-installation-work
        return stagingDirectory
            .replaceFirst("^([A-Z])\\$", "$1:")
            .replaceAll("/", Matcher.quoteReplacement("\\"));
    }
}
