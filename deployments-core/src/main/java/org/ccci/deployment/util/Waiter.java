package org.ccci.deployment.util;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * @author Matt Drees
 */
public class Waiter {

    boolean first = true;
    private final int pauseTime;
    Logger log = Logger.getLogger(getClass());

    public Waiter(int pauseTime) {
        this.pauseTime = pauseTime;
    }

    public void waitIfNecessary()
    {
        if (first)
            first = false;
        else {
            log.info("waiting " + pauseTime + " seconds before restarting next node");
            try
            {
                TimeUnit.SECONDS.sleep(pauseTime);
            }
            catch (InterruptedException e)
            {
                throw Throwables.propagate(e);
            }
        }
    }
}
