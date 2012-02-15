package org.ccci.deployment.windows;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.ccci.deployment.ExceptionBehavior;
import org.ccci.deployment.Node;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.spi.AppserverInterface;
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
        RemoteShell remoteShell = new RemoteShell(node.getHostname(), credential);
        
        try
        {
            log.info("clearing old backup if necessary");
            remoteShell.executeSingleCommand("cd " + stagingDirectory);
            remoteShell.executeSingleCommand("rd /s /q installation");
            remoteShell.executeSingleCommand("rd /s /q jboss-as-*");
            log.info("unzipping installation archive");
            remoteShell.executeSingleCommand("%java_home%\\bin\\jar xf " + installationPackedName);
            log.info("stopping jboss");
            shutdownServer();
            log.info("running installer");
            remoteShell.executeSingleCommand("installation\\update_jboss_installation.bat");
            log.info("starting jboss");
            startupServer(nonfatalExceptionBehavior);
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }
}
