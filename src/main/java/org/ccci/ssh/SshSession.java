package org.ccci.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.ccci.util.NotImplementedException;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

public class SshSession
{
    private Logger log = Logger.getLogger(getClass());
    
    private final int sshSocketConnectTimeout = 10;
    
    private final SshEndpoint endpoint;
    private final  ServerHostKeyVerifier verifier;
    private Connection connection;
    
    public SshSession(SshEndpoint endpoint, ServerHostKeyVerifier verifier)
    {
        this.endpoint = endpoint;
        this.verifier = verifier;
    }
    
    Charset charset = Charsets.UTF_8;



    public void connect() throws IOException
    {
        connection = new Connection(endpoint.getHostName());
        
        connection.connect(verifier, (int) TimeUnit.SECONDS.toMillis(sshSocketConnectTimeout), 0);
        boolean success = connection.authenticateWithPassword(endpoint.getUsername(), endpoint.getPassword());
        if (!success)
            throw new IOException("ssh password authentication to " + endpoint.getHostName() + "for user " + endpoint.getUsername() + " refused");
    }


//    private void openShell() throws IOException
//    {
//        Session shellSession = connection.openSession();
//        getOutputUpToPrompt();
//    }

    
    public String executeSessionCommand(String command) throws IOException
    {
        throw new NotImplementedException();
//        openShellIfNecessary();
//        writeShellCommand(command);
//        return getOutputUpToPrompt();
    }


//    private void openShellIfNecessary() throws JSchException, IOException
//    {
//        if (shellChannel == null)
//            openShell();
//    }
//
//
//    private void writeShellCommand(String command) throws IOException
//    {
//        commandWriter.write(command);
//        commandWriter.write("\n");
//        commandWriter.flush();
//    }
//
//
//    private String getOutputUpToPrompt() throws IOException
//    {
//        Pattern outputAndPromptPattern = buildPromptPattern();
//
//        StringBuilder builder = new StringBuilder();
//        Matcher matcher = outputAndPromptPattern.matcher(builder);
//        while (! matcher.matches())
//        {
//            char character = (char) outputReader.read();
//            builder.append(character);
//            matcher.reset();
//        }
//        
//        return matcher.group(1);
//    }
//
//    /** should match something like "output\n[mdrees@harta120 ~]$ " */
//    Pattern buildPromptPattern()
//    {
//        String usernamePattern = escape(endpoint.getUsername());
//        String machinePattern = nameUpToFirstDot(endpoint.getHostName());
//        String workingDirPattern = "[\\w~]*";
//        String endOfInput = "\\z";
//        String promptPatternString =
//                "(.*)" + "\\[" + usernamePattern + "@" + machinePattern + " " + workingDirPattern + "\\]\\$ " + endOfInput;
//        
//        return Pattern.compile(promptPatternString, Pattern.DOTALL);
//    }
//
//    private String nameUpToFirstDot(String hostName)
//    {
//        if (hostName.contains("."))
//        {
//            return hostName.substring(0, hostName.indexOf("."));
//        }
//        else
//        {
//            return hostName;
//        }
//    }
//
//    private String escape(String username)
//    {
//        //TODO: this could be more robust...
//        return username.replace(".", "\\.");
//    }
    
    public void close()
    {
//        disconnectShell();
        connection.close();
    }


//    private void disconnectShell()
//    {
//        if (shellChannel != null)
//        {
//            try
//            {
//                writeShellCommand("exit");
//                commandWriter.close();
//            }
//            catch (IOException e)
//            {
//                log.error("unable to send 'exit' command to shell; ignoring");
//            }
//            disconnectChannelAndLogExceptions(shellChannel);
//        }
//    }
    
    
    private void disconnectSessionAndLogExceptions(Session session )
    {
        try
        {
            session.close();
        }
        catch (Exception e)
        {
            log.error("exception while disconnecting session", e);
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
    public String executeSingleCommand(String command) throws IOException
    {
        Session session = connection.openSession();
        try
        {
            session.requestDumbPTY();
            InputStream stdout = new StreamGobbler(session.getStdout());
            InputStream stderr = new StreamGobbler(session.getStderr());
            session.execCommand(command);
            
            String output = CharStreams.toString(new InputStreamReader(stdout, Charsets.UTF_8));
            String errorOutput = CharStreams.toString(new InputStreamReader(stderr, Charsets.UTF_8));
            
            session.waitForCondition(ChannelCondition.EXIT_STATUS, TimeUnit.SECONDS.toMillis(1));
            
            Integer exitStatus = session.getExitStatus();
            if (exitStatus == null)
                log.warn("unable to determine exit status for command '" + command + "'");
            else if (exitStatus > 0)
                throw new RemoteExecutionFailureException(exitStatus, output, errorOutput);
            return output;
        }
        finally
        {
            disconnectSessionAndLogExceptions(session);
        }
    }
    
    
    public void sendFile(String localFilePath, String destinationDirectory, String destinationFileName, String mode) throws IOException
    {
        SCPClient scpClient = connection.createSCPClient();
        scpClient.put(localFilePath, destinationFileName, destinationDirectory, mode);
    }
    
}
