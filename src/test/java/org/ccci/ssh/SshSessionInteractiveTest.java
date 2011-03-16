package org.ccci.ssh;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

import org.ccci.ssh.SshEndpoint;
import org.ccci.ssh.SshSession;

public class SshSessionInteractiveTest
{

    public static void main(String... args) throws IOException
    {
        SshSessionInteractiveTest test = new SshSessionInteractiveTest();
        test.testRemoteExecution();
    }

    private static String readPasswordFromInput() throws IOException
    {
        System.out.print("password: ");
        Console console = System.console();
        if (console == null)
        {
            return new BufferedReader(new InputStreamReader(System.in)).readLine();
        }
        else
        {
            return new String(console.readPassword());
        }
    }
    
    public void testRemoteExecution() throws IOException
    {    
        String password = readPasswordFromInput();
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
