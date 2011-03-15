package org.ccci.deployment.windows;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.ccci.deployment.Node;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.util.NotImplementedException;
import org.ccci.windows.management.RemoteServiceControl;
import org.ccci.windows.smb.ActiveDirectoryCredential;
import org.jinterop.dcom.common.IJIAuthInfo;
import org.jinterop.dcom.common.JIDefaultAuthInfoImpl;
import org.jinterop.dcom.common.JIException;

import com.google.common.base.Throwables;

public class TomcatWindowsAppserverInterface implements AppserverInterface
{

    private final String serviceName;
    private final Node node;
    private final ActiveDirectoryCredential activeDirectoryCredential;

    public TomcatWindowsAppserverInterface(Node node, String serviceName, ActiveDirectoryCredential activeDirectoryCredential)
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
    public void startupServer()
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
        IJIAuthInfo credential = new JIDefaultAuthInfoImpl(
            activeDirectoryCredential.getDomain(), 
            activeDirectoryCredential.getUsername(), 
            activeDirectoryCredential.getPassword());
        RemoteServiceControl control = new RemoteServiceControl(node.getHostname(), credential, serviceName);
        return control;
    }

}
