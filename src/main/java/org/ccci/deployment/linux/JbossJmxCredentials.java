package org.ccci.deployment.linux;

public class JbossJmxCredentials
{

    private final String password;
    private final String user;
    
    public JbossJmxCredentials(String user, String password)
    {
        this.password = password;
        this.user = user;
    }

    public String getPassword()
    {
        return password;
    }

    public String getUser()
    {
        return user;
    }
}
