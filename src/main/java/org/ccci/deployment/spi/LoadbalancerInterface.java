package org.ccci.deployment.spi;

import org.ccci.deployment.Node;

public interface LoadbalancerInterface
{

    public Node getActiveNode();
}
