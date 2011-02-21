package org.ccci.deployment;

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
