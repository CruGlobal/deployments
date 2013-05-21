package org.ccci.deployment.give;

import org.apache.log4j.Logger;
import org.ccci.deployment.Version;
import org.ccci.deployment.spi.WebappControlInterface;
import org.ccci.deployment.util.PageMatcher;

public class GiveSiteWebappControlInterface implements WebappControlInterface
{
    
    Logger log = Logger.getLogger(GiveSiteWebappControlInterface.class);

    private final String server;
    private final int port;
    
    public GiveSiteWebappControlInterface(String server, int port)
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
        String uri = "http://" + server + ":" + port +"/green.html";
        new PageMatcher().pingUntilPageMatches(uri, ".*OK.*", "green.html", 60);
    }

    @Override
    public Version getCurrentVersion()
    {
        throw new UnsupportedOperationException();
    }

}