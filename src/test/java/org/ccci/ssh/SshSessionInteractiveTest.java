package org.ccci.ssh;

import java.io.IOException;

import org.ccci.util.ConsoleUtil;

public class SshSessionInteractiveTest
{

    public static void main(String... args) throws IOException
    {
        SshSessionInteractiveTest test = new SshSessionInteractiveTest();
        test.testRemoteExecution();
    }

    public void testRemoteExecution() throws IOException
    {    
        String password = ConsoleUtil.readPasswordFromInput();
        SshSession session = new SshSession(new SshEndpoint("mdrees", "harta122", password), StrictKnownHostsVerifier.loadFromClasspath());

        session.connect();
        
//        printOutput(session.executeSingleCommand("sudo -u jboss /usr/local/jboss/server/ccpserver/bin/jboss.sh status"));
        printOutput(session.executeSingleCommand("cd /tmp; sudo -u jboss /usr/local/jboss/bin/twiddle.sh --user=admin --password=admin get \"jboss.system:type=Server\" Started"));
        
        session.close();
        
    }

    private void printOutput(String string)
    {
        System.out.println("output: " + string);
    }
}
