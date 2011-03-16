package org.ccci.ssh;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.ServerHostKeyVerifier;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

public class StrictKnownHostsVerifier implements ServerHostKeyVerifier
{

    private final KnownHosts knownHosts;
    
    /**
     * Builds a KnownHosts database from /known_hosts files on the classpath
     */
    public static StrictKnownHostsVerifier loadFromClasspath()
    {
        KnownHosts knownHosts = new KnownHosts();
        Enumeration<URL> knownHostResources;
        try
        {
            knownHostResources = Thread.currentThread().getContextClassLoader().getResources("known_hosts");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to search for known_hosts files", e);
        }
        while (knownHostResources.hasMoreElements())
        {
            URL knownHostResource = knownHostResources.nextElement();
            
            InputStreamReader reader;
            try
            {
                reader = new InputStreamReader(knownHostResource.openStream(), Charsets.UTF_8);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Unable to open " + knownHostResource, e);
            }
            try
            {
                knownHosts.addHostkeys(CharStreams.toString(reader).toCharArray());
            }
            catch (IOException e)
            {
                throw new RuntimeException("Unable to add known_hosts content from " + knownHostResource, e);
            }
            finally
            {
                Closeables.closeQuietly(reader);
            }
        }
        return new StrictKnownHostsVerifier(knownHosts);

    }
    
    public StrictKnownHostsVerifier(KnownHosts knownHosts)
    {
        this.knownHosts = knownHosts;
    }

    @Override
    public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey)
            throws Exception
    {
        int result = knownHosts.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);

        switch (result)
        {
            case KnownHosts.HOSTKEY_IS_OK:
                return true;
    
            case KnownHosts.HOSTKEY_IS_NEW:
                throw new IllegalStateException("unknown host: " + hostname + " with ssh-rsa fingerprint " + KnownHosts.createHexFingerprint("ssh-rsa", serverHostKey));
    
            case KnownHosts.HOSTKEY_HAS_CHANGED:
                throw new IllegalStateException("host key has changed: " + hostname + " has new ssh-rsa fingerprint " + KnownHosts.createHexFingerprint("ssh-rsa", serverHostKey));
    
            default:
                throw new IllegalStateException();
        }
    }

    public boolean containsHost(String hostname)
    {
        // Note: this only works if the known_hosts file is guaranteed to contain only one type of key per host.  
        // For us, this is the case.
        return knownHosts.getPreferredServerHostkeyAlgorithmOrder(hostname) != null;
    }
}
