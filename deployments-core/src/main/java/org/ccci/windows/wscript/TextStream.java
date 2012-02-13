package org.ccci.windows.wscript;

import org.kohsuke.jinterop.JIProxy;
import org.kohsuke.jinterop.Property;

public interface TextStream extends JIProxy
{

    @Property
    boolean AtEndOfStream();
    
    @Property
    boolean AtEndOfLine();
    
    String ReadAll();
    
    String ReadLine();
    
    String Read(int characters);
    
    void Close();
}
