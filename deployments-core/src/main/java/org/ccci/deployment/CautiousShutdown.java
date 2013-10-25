package org.ccci.deployment;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.deployment.spi.LoadbalancerInterface;
import org.ccci.deployment.spi.WebappControlInterface;

import java.util.concurrent.TimeUnit;

public class CautiousShutdown {
    
    private final DeploymentConfiguration configuration;
    private final ExceptionBehavior nonfatalExceptionBehavior;
    private int timeLimit;

    Logger log = Logger.getLogger(getClass());

    public CautiousShutdown(DeploymentConfiguration configuration, ExceptionBehavior nonfatalExceptionBehavior, int timeLimitInSeconds) {
        this.configuration = configuration;
        this.nonfatalExceptionBehavior = nonfatalExceptionBehavior;
        this.timeLimit = timeLimitInSeconds;
    }


    public void cautiouslyShutdownIfPossible(Node node, WebappControlInterface webappControlInterface)
        throws AssertionError
    {
        if (configuration.supportsCautiousShutdown())
        {
            try
            {
                removeNodeFromLoadbalancerService(webappControlInterface, node);
            }
            catch (RuntimeException e)
            {
                if (nonfatalExceptionBehavior == ExceptionBehavior.LOG)
                {
                    log.error("unable to remove " + node + " from loadbalancer service; continuing deployment", e);
                }
                else if (nonfatalExceptionBehavior == ExceptionBehavior.HALT)
                {
                    throw e;
                }
                else
                    throw new AssertionError();
            }
        }
    }

    /*
     *  Informs the load balancer to stop sending requests to this node
     */
    void removeNodeFromLoadbalancerService(WebappControlInterface webappControlInterface, Node restartingNode) {
        log.info("disabling app");
        webappControlInterface.disableForUpgrade();

        LoadbalancerInterface loadbalancerInterface = configuration.buildLoadBalancerInterface();

        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeLimit);

        while (true) {
            Node activeNode = loadbalancerInterface.getActiveNode();
            if (!activeNode.equals(restartingNode)) {
                return;
            } else {
                if (System.currentTimeMillis() > deadline)
                    throw new RuntimeException(
                        "load balancer did not remove " + activeNode +
                        " from service within " + timeLimit + " seconds");
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    throw Throwables.propagate(e);
                }
            }
        }

    }
}