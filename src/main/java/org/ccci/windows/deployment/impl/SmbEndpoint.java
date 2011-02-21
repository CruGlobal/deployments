package org.ccci.windows.deployment.impl;

import java.net.MalformedURLException;
import java.net.URL;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class SmbEndpoint
{
    
    private final String server;
    NtlmPasswordAuthentication auth;
    

    public SmbEndpoint(ActiveDirectoryCredential credential, String server)
    {
        this.server = server;
        this.auth = new NtlmPasswordAuthentication(credential.getDomain(), credential.getUsername(), credential.getPassword());
    }

    public SmbFile createSmbFile(String string)
    {
        URL url;
        try
        {
            url = new URL("smb://" + server +"/" + string);
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("invalid target: " + string, e);
        }
        return new SmbFile(url, auth);
    }

    
}
