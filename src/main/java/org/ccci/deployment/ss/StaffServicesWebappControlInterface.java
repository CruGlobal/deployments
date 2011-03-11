package org.ccci.deployment.ss;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.ccci.deployment.Version;
import org.ccci.deployment.WebappControlInterface;

import com.google.common.base.Throwables;

public class StaffServicesWebappControlInterface implements WebappControlInterface
{
    
    Logger log = Logger.getLogger(StaffServicesWebappControlInterface.class);

    private final HttpClient httpclient = new DefaultHttpClient();

    private final String server;
    private final int port;
    
    public StaffServicesWebappControlInterface(String server, int port)
    {
        this.server = server;
        this.port = port;
        
        httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, (int) TimeUnit.SECONDS.toMillis(5));
        httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, (int) TimeUnit.SECONDS.toMillis(30));
    }

    @Override
    public void disableForUpgrade()
    {
    }

    
    @Override
    public void verifyNewDeploymentActive()
    {
        //wait for http://hart-a041.net.ccci.org:8180/ss/green.html to return "OK"
        
        String uri = "http://" + server + ":" + port +"/ss/green.html";
        
        log.info("requesting " + uri);
        HttpGet request = new HttpGet(uri);
        
        int maxWaitTime = 30;
        
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxWaitTime);
        
        int ioFailures = 0;
        while (true)
        {
            try
            {
                HttpResponse response = httpclient.execute(request);
                
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() != 200)
                {
                    throw new RuntimeException("status page not available; response: " + statusLine);
                }
                
                String content = EntityUtils.toString(response.getEntity());
                if (!content.contains("OK"))
                {
                    log.error("Green.html page does not contain 'OK'; page content follows:" + 
                        System.getProperty("line.separator") +
                        content);
                    
                    throw new RuntimeException("status page does not contain 'OK'.  Actual content was logged.");
                }
                return;
            }
            catch (HttpHostConnectException e)
            {
                long currentTime = System.currentTimeMillis();
                if (currentTime > deadline)
                    throw new RuntimeException("Unable to connect to " + uri + " after " + maxWaitTime + "seconds of trying", e);
                else
                    sleepBriefly();
            }
            catch (ClientProtocolException e)
            {
                throw Throwables.propagate(e);
            }
            catch (IOException e)
            {
                ioFailures++;
                if (ioFailures > 2)
                {
                    throw new RuntimeException("Unable to connect to " + uri + " after " + ioFailures + " attempts");
                }
                else
                    sleepBriefly();
            }
        }

        
    }

    private void sleepBriefly()
    {
        try
        {
            TimeUnit.MILLISECONDS.sleep(500);
        }
        catch (InterruptedException interruption)
        {
            throw Throwables.propagate(interruption);
        }
    }

    @Override
    public Version getCurrentVersion()
    {
        throw new UnsupportedOperationException();
    }

}
