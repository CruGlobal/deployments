package org.ccci.deployment.basic;

/**
 * @author Matt Drees
 */
public class PingConfig {

    public final String uriPath;
    public final String regex;
    public final int secondsBeforeTimeout;

    public PingConfig(String uriPath, String regex, int secondsBeforeTimeout) {
        this.uriPath = uriPath;
        this.regex = regex;
        this.secondsBeforeTimeout = secondsBeforeTimeout;
    }
}
