package org.ccci.windows.wscript;

import java.net.UnknownHostException;

import org.ccci.windows.management.CcciJInteropInvocationHandler;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;

public class CcciWSH
{
    public static WshShell connect(JISession session, String hostName) throws UnknownHostException, JIException {
        JIComServer comStub = new JIComServer(
            JIProgId.valueOf("WScript.Shell"),hostName, session);
        
        return CcciJInteropInvocationHandler.wrap(WshShell.class,comStub.createInstance());
    }

    static {
        JISystem.setAutoRegisteration(true);
    }
}
