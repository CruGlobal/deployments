package org.ccci.windows.wscript;

import org.kohsuke.jinterop.JIProxy;

/**
 * Represents a windows scripting host shell
 * See http://msdn.microsoft.com/en-us/library/aew9yb99%28v=VS.84%29.aspx
 * @author Matt Drees
 *
 */
public interface WshShell extends JIProxy
{
    WshScriptExec Exec(String command);

}
