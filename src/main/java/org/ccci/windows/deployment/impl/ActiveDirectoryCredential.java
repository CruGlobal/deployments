package org.ccci.windows.deployment.impl;

public class ActiveDirectoryCredential
{

    private final String domain;
    private final String password;
    private final String username;
    
    
    public ActiveDirectoryCredential(String domain, String password, String username)
    {
        this.domain = domain;
        this.password = password;
        this.username = username;
    }

    public String getDomain()
    {
        return domain;
    }

    public String getPassword()
    {
        return password;
    }

    public String getUsername()
    {
        return username;
    }
    
}
