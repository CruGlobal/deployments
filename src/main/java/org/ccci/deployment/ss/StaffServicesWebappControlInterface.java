package org.ccci.deployment.ss;

import org.apache.log4j.Logger;
import org.ccci.deployment.Version;
import org.ccci.deployment.WebappControlInterface;
import org.ccci.deployment.util.PageMatcher;

public class StaffServicesWebappControlInterface implements WebappControlInterface
{
    
    Logger log = Logger.getLogger(StaffServicesWebappControlInterface.class);

    private final String server;
    private final int port;
    
    public StaffServicesWebappControlInterface(String server, int port)
    {
        this.server = server;
        this.port = port;
    }

    @Override
    public void disableForUpgrade()
    {
    }
    
    @Override
    public void verifyNewDeploymentActive()
    {
        String uri = "http://" + server + ":" + port +"/ss/green.html";
        new PageMatcher().pingUntilPageMatches(uri, ".*OK.*", "green.html");
    }

    @Override
    public Version getCurrentVersion()
    {
        throw new UnsupportedOperationException();
    }

}
