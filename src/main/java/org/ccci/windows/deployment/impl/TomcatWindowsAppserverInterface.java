package org.ccci.windows.deployment.impl;

import java.io.IOException;

import org.ccci.deployment.AppserverInterface;
import org.ccci.deployment.Node;
import org.ccci.deployment.WebappDeployment;
import org.ccci.util.NotImplementedException;
import org.ccci.wmi.RemoteServiceControl;
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
