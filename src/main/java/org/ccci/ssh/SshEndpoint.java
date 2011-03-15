package org.ccci.ssh;

public class SshEndpoint
{

    private final String username;
    private final String hostName;
    private final String password;

    public SshEndpoint(String username, String hostName, String password)
    {
        this.username = username;
        this.hostName = hostName;
        this.password = password;
    }

    public String getUsername()
    {
        return username;
    }

    public String getHostName()
    {
        return hostName;
    }

    public String getPassword()
    {
        return password;
    }

}
