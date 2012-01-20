package org.ccci.ssh;

import org.ccci.util.strings.Strings;

public class RemoteExecutionFailureException extends RuntimeException
{
    public final int exitStatus;
    public final String output;
    public final String errorOutput;

    public RemoteExecutionFailureException(int exitStatus, String output, String errorOutput)
    {
        super("Execution failed; exit status: " + exitStatus + ";\n" +
            "error output follows:\n" + errorOutput + 
            buildProgramOutputString(output));
        this.exitStatus = exitStatus;
        this.output = output;
        this.errorOutput = errorOutput;
    }

    private static String buildProgramOutputString(String output)
    {
        return Strings.isEmpty(output) ?
            "" : "\n" + "program output follows:\n" + output;
    }

    private static final long serialVersionUID = 1L;
}
