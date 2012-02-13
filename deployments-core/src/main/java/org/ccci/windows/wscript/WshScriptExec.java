package org.ccci.windows.wscript;

import org.kohsuke.jinterop.JIProxy;
import org.kohsuke.jinterop.Property;

public interface WshScriptExec extends JIProxy
{

    @Property
    TextStream StdOut();
    
    @Property
    TextStream StdErr();
    
    @Property
    TextStream StdIn();
    
    @Property
    int ExitCode();
    
    @Property
    int ProcessID();
    
    @Property
    int Status();
    
    public static final int WSH_RUNNING = 0;
    public static final int WSH_FINISHED = 1;
}
