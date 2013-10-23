package org.ccci.deployment.basic;

import org.ccci.deployment.Version;
import org.ccci.deployment.spi.WebappControlInterface;
import org.ccci.deployment.util.PageMatcher;

/**
 * @author Matt Drees
 */
public class BasicWebappControlInterface implements WebappControlInterface {

    private final String server;
    private final int port;
    private final PingConfig pingConfig;

    public BasicWebappControlInterface(String server, int port, PingConfig pingConfig) {
        this.server = server;
        this.port = port;
        this.pingConfig = pingConfig;
    }

    @Override
    public void disableForUpgrade()
    {
    }

    @Override
    public void verifyNewDeploymentActive()
    {
        String uri = "http://" + server + ":" + port + pingConfig.uriPath;
        new PageMatcher().pingUntilPageMatches(uri, pingConfig.regex, pingConfig.uriPath, pingConfig.secondsBeforeTimeout);
    }

    @Override
    public Version getCurrentVersion()
    {
        throw new UnsupportedOperationException();
    }
}
