package org.ccci.windows.management;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.ccci.windows.registry.WindowsRegistryService;
import org.jinterop.dcom.common.IJIAuthInfo;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JISession;
import org.jvnet.hudson.wmi.SWbemServices;
import org.jvnet.hudson.wmi.Win32Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

public class RemoteServiceControl
{
    private final String serviceName;
    private final WindowsRegistryService registryService;
    private final JISession session;
    private final SWbemServices services;

    int globalSocketTimeout = (int) TimeUnit.SECONDS.toMillis(10);
    
    public RemoteServiceControl(String hostName, IJIAuthInfo credential, String serviceName)
    {
        this.serviceName = serviceName;
        this.registryService = new WindowsRegistryService(credential, hostName);
        this.session = JISession.createSession(credential);
        session.setGlobalSocketTimeout(globalSocketTimeout);
        try
        {
            this.services = CcciWMI.connect(session, hostName);
        }
        catch (UnknownHostException e)
        {
            throw Throwables.propagate(e);
        }
        catch (JIException e)
        {
            throw Throwables.propagate(e);
        }
    }

    public void startOrRestartService() throws JIException, IOException
    {
        Win32Service service = getService(session);
        boolean started = service.Started();
        setTimeout(service);
        
        try
        {
            performRestart(service, started);
        }
        finally
        {
            service.setInstanceLevelSocketTimeout(0);
        }
    }

    public void stopService() throws JIException, IOException
    {
        Win32Service service = getService(session);
        boolean started = service.Started();
        
        if (!started)
        {
            log.warn("service is not running.  Ignoring stop command.");
            return;
        }
        
        setTimeout(service);
        try
        {
            log("stopping");
            service.stop();
            log("stopped");
        }
        finally
        {
            service.setInstanceLevelSocketTimeout(0);
        }
    }

    private void setTimeout(Win32Service service)
    {
        int timeoutMs = getServiceStateChangeTimeout() + 200;
        
        service.setInstanceLevelSocketTimeout(timeoutMs);
    }
    
    private Win32Service getService(JISession session) throws UnknownHostException, JIException
    {
        Win32Service service = services.getService(serviceName);
        Preconditions.checkArgument(service != null, "no such service: " + serviceName);
        return service;
    }

    private void performRestart(Win32Service service, boolean started) throws JIException
    {
        if (!started)
        {
            log("starting");
            service.start();
        }
        else
        {
            log("stopping");
            service.stop();
            log("starting");
            service.start();
        }
        log("started");
    }

    /**
     * I believe the default ServicesPipeTimeout is 30 seconds; see http://bytes.com/topic/c-sharp/answers/274008-service-stop-timeout-setting
     */
    int defaultStartTimeout = (int) TimeUnit.SECONDS.toMillis(45);
    
    private int getServiceStateChangeTimeout()
    {
        return defaultStartTimeout;
        
        //not sure if this sort of thing is worth keeping around; it doesn't work, currently, on a012 at least.  It can't find the registry location.
//        try
//        {
//            return registryService.getHKeyLocalMachineRegistryValue(Integer.class,
//                        "System\\CurrentControlSet\\Control", "ServicesPipeTimeout");
//        }
//        catch (Exception e)
//        {
//            // TODO: hmmm
//            log.warn("unable to lookup service state change timeout", e);
//            return defaultStartTimeout;
//        }
    }


    Logger log = Logger.getLogger(getClass().getName());
    private void log(String message)
    {
        log.info(message);
    }

    public void close()
    {
        registryService.close();
        try
        {
            JISession.destroySession(session);
        }
        catch (JIException e)
        {
            log.warn("exception destroying session; ignoring it", e);
        }
    }

    public boolean serviceIsStarted()
    {
        try
        {
            Win32Service service = getService(session);
            return service.Started();
        }
        catch (Exception e)
        {
            throw Throwables.propagate(e);
        }
    }
    
    public boolean serviceIsStopped()
    {
        try
        {
            Win32Service service = getService(session);
            String state = service.State();
            return state.equals("Stopped");
        }
        catch (Exception e)
        {
            throw Throwables.propagate(e);
        }
    }

    
}
