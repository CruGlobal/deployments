package org.ccci.ssh;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.crypto.Base64;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
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

    /**
     * Whether or not this verifier's {@link KnownHosts} has a key for the given {@code hostname}
     */
    public boolean containsHost(String hostname)
    /*
     * note: this implementation is rather silly, since KnownHosts doesn't easily expose which hosts it contains.
     * Oh well. 
     */
    {
        String garbageKey = "AAAAB3NzaC1yc2EAAAABIwAAAQEaB3DDvFKJy7QHfJVmn25MRKuqDh5bUO0yjlxE9542DMP/G8KDssA/pJGhFxe2ym1FQXf1yyBA9KXi15rlDAvkEKRlR/5UPSM82hF8QmVG8VH+Dm1EWe60Aqvlgj9cTUy7Hkkl/oDxhCIJTZqTlx0LwNHP3NYVysvcs+I+hoI/1C0VFTNKIpRloCXDxck+o4Jp8v0FLGW1PmzlrqhfoC7FV46LfTdc3LlfKWto9HmcVn1sKH7CDl/8YqMotv2OJtLBnpWPeMW7A9WqM91ldOUWB2GCKPMaX4p4vZkEdLsH5/uf18p89JIFrkB3OZPq6i3dKoS5GYiDcWxT7fO3b6EyGw==";
        int result;
        try
        {
            result = knownHosts.verifyHostkey(hostname, "ssh-rsa", Base64.decode(garbageKey.toCharArray()));
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
        
        switch (result)
        {
            case KnownHosts.HOSTKEY_IS_NEW:
                return false;
            case KnownHosts.HOSTKEY_HAS_CHANGED:
                return true;
            case KnownHosts.HOSTKEY_IS_OK:
                throw new IllegalStateException("there shouldn't actually be a host with this garbage key");
            default:
                throw new IllegalStateException();
        }
    }
}
