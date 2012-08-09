package org.ccci.windows.wscript;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.ccci.ssh.RemoteExecutionFailureException;
import org.jinterop.dcom.common.IJIAuthInfo;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JISession;

import com.google.common.base.Throwables;

public class RemoteShell
{

    static int defaultGlobalSocketTimeout = (int) TimeUnit.SECONDS.toMillis(45);
    private final JISession session;
    private WshShell wshShell;
    
    public RemoteShell(String hostName, IJIAuthInfo credential)
    {
        this(hostName, credential, defaultGlobalSocketTimeout);
    }
    
    public RemoteShell(String hostName, IJIAuthInfo credential, long globalSocketTimeout)
    {
        this.session = JISession.createSession(credential);
        session.setGlobalSocketTimeout((int) globalSocketTimeout);
        
        try
        {
            this.wshShell = CcciWSH.connect(session, hostName);
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
    
    /**
     * Executes a single command.  If the exit status is 0 (success), the output (written to stdout) is returned.
     * Otherwise, a {@link RemoteExecutionFailureException} is thrown that contains stdout and stderr output, as well as the exit 
     * code.
     * 
     * @throws RemoteExecutionFailureException if the command fails
     * @throws IOException if the sftp transport fails
     */
    /*
     * Note: if the command generates lots of error output, and the buffer is of limited size, this might
     * deadlock.  A more robust solution would be to use something like StreamGobbler for both stdOut and stdErr
     */
    public String executeSingleCommand(String command) throws IOException
    {
        WshScriptExec exec = wshShell.Exec("%comspec% /c \"" + command +"\"");

        String outputString = readAll(exec.StdOut());
        String errString = readAll(exec.StdErr());
        
        waitForCommandToFinish(exec);
        
        if (exec.ExitCode() != 0)
        {
            throw new RemoteExecutionFailureException(command, exec.ExitCode(), outputString, errString);
        }
        else
        {
            return outputString;
        }
    }

    private void waitForCommandToFinish(WshScriptExec exec)
    {
        while (exec.Status() == WshScriptExec.WSH_RUNNING)
        {
            try
            {
                TimeUnit.MILLISECONDS.sleep(10);
            }
            catch (InterruptedException e)
            {
                throw Throwables.propagate(e);
            }
        }
    }

    private String readAll(TextStream stdOut)
    {
        StringBuilder output = new StringBuilder();
        while(!stdOut.AtEndOfStream()){ 
            output.append(stdOut.ReadAll()); 
        }
        return output.toString();
    }

}
