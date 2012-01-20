package org.ccci.deployment;

import org.ccci.deployment.spi.DeploymentConfiguration;

/**
 * Represents a host that an application can be deployed to.
 * 
 * Nodes are usable as keys in maps, but only implement {@link #equals(Object)} using
 * object identity, so deployment configurations should take care to return the same list of nodes from
 * {@link DeploymentConfiguration#listNodes()}
 * 
 * @author Matt Drees
 */
public class Node
{
    
    private final String name;
    private final String hostname;
    
    public Node(String name, String hostname)
    {
        this.name = name;
        this.hostname = hostname;
    }

    public String getName()
    {
        return name;
    }

    /**
     * For usage in emails, logs, etc
     */
    @Override
    public String toString()
    {
        return name;
    }
    
    /**
     * 
     * e.g. hart-a321.net.ccci.org
     * or
     * harta121.ccci.org
     */
    public String getHostname()
    {
        return hostname;
    }
    
}
