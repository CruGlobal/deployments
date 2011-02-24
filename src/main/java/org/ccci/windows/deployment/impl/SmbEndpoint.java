package org.ccci.windows.deployment.impl;

import java.net.MalformedURLException;

import com.google.common.base.Preconditions;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
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

    public SmbFile createSmbPath(String path) throws SmbException
    {
        String url = "smb://" + server +"/" + path;
        try
        {
            SmbFile smbFile = new SmbFile(url, auth);
            /* jcifs is kinda funky with SmbFile paths that represent directories but don't end with '/',
             * so always make directory paths end with a slash
             */
            if (smbFile.exists() && smbFile.isDirectory() && !path.endsWith("/"))
            {
                smbFile = new SmbFile(url + "/", auth);
            }
            return smbFile;
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("invalid target: " + path, e);
        }
    }

    public SmbFile createChildFilePath(SmbFile destinationDir, String filename) throws MalformedURLException
    {
        Preconditions.checkArgument(!filename.contains("/"), "filename must not contain a slash: %s", filename);
        String destinationDirUrl = getCorrectlyTerminatedUrl(destinationDir);
        
        return new SmbFile(destinationDirUrl, filename, auth);
    }

    public SmbFile createChildDirectoryPath(SmbFile destinationDir, String directoryname) throws MalformedURLException
    {
        Preconditions.checkArgument(!directoryname.contains("/"), "directoryname must not contain a slash: %s", directoryname);
        String destinationDirUrl = getCorrectlyTerminatedUrl(destinationDir);
        
        return new SmbFile(destinationDirUrl, directoryname + "/", auth);
    }

    /*
     * the SmbFile constructor with 2 arguments is picky.  If the first argument (an SmbFile) is 
     * associated with a url that doesn't have a slash, then the last component of the url is lost
     * during the construction of the new file.  So we have to make sure there is always a slash.
     */
    private String getCorrectlyTerminatedUrl(SmbFile destinationDir)
    {
        String destinationDirUrl = destinationDir.getURL().toString();
        if (!destinationDirUrl.endsWith("/"))
            destinationDirUrl += "/";
        return destinationDirUrl;
    }
    
}
