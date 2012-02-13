package org.ccci.windows.management;

import java.io.IOException;

import org.ccci.util.ConsoleUtil;
import org.ccci.windows.wscript.RemoteShell;
import org.jinterop.dcom.common.JIDefaultAuthInfoImpl;
import org.jinterop.dcom.common.JIException;


public class RemoteShellInteractiveTest
{

    public void restartService() throws JIException, IOException, InterruptedException
    {
        String password = ConsoleUtil.readPasswordFromInput();

        String hostName = "hart-a321.net.ccci.org";
        String domain = "NET";
        String username = "phillip.drees";
        JIDefaultAuthInfoImpl credential = new JIDefaultAuthInfoImpl(domain, username, password);

        RemoteShell shell = new RemoteShell(hostName, credential);
        
        String output = shell.executeSingleCommand("echo bar");
        System.out.println("Output:");
        System.out.println(output);
    }
    
    public static void main(String... args) throws JIException, IOException, InterruptedException
    {
        new RemoteShellInteractiveTest().restartService();
    }

}
