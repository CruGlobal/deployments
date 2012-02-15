package org.ccci.deployment.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.*;

import org.ccci.deployment.windows.SimpleWindowsServiceAppserverInterface;
import org.testng.annotations.Test;

public class SimpleWindowsServiceAppserverInterfaceTest
{
    @Test
    public void testPathReplacement()
    {
        SimpleWindowsServiceAppserverInterface appserverInterface = new SimpleWindowsServiceAppserverInterface(null, null, null);
        
        String result = appserverInterface.convertToWindowsPath("W$/tmp/wsapi-jboss-installation-work");
        
        assertThat(result, is("W:\\tmp\\wsapi-jboss-installation-work"));
        
    }

}
