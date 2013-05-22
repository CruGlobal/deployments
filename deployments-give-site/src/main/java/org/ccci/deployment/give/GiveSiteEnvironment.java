package org.ccci.deployment.give;

import java.util.List;
import java.util.Set;

import org.ccci.deployment.Node;
import org.ccci.util.mail.EmailAddress;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

public enum GiveSiteEnvironment
{
    TEST(
        "W$/Tomcat/instances/give-inst-siebel", 
        "Tomcat - DSS - Siebel", 
        buildA21(),
        buildProjectLead(),
        8280),
    STAGING(
        "W$/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA31A32(),
        buildProjectLead(),
        8280),
    STAGE1(
        "W$/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA31(),
        buildProjectLead(),
        8280),
    STAGE2(
        "W$/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA32(),
        buildProjectLead(),
        8280),
    PRODUCTION(
        "W$/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA41A42(),
        buildProductionSubscribers(),
        8280),
    PROD1(
        "/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA41(),
        buildProductionSubscribers(),
        8280),
    PROD2(
        "/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA42(),
        buildProductionSubscribers(),
        8280);

    public final String serviceName;
    
    /** composed of the network share name followed by the path to the tomcat "base" directory */
    public final String tomcatBasePath;

    public final List<Node> nodes;

    public Set<EmailAddress> deploymentSubscribers;

    public final int port;
    
    private GiveSiteEnvironment(
         String tomcatBasePath, 
         String serviceName, 
         List<Node> nodes, 
         Set<EmailAddress> deploymentSubscribers,
         int port)
    {
        this.tomcatBasePath = tomcatBasePath;
        this.serviceName = serviceName;
        this.nodes = nodes;
        this.deploymentSubscribers = deploymentSubscribers;
        this.port = port;
    }


    private static Set<EmailAddress> buildProjectLead()
    {
        return ImmutableSet.of(EmailAddress.valueOf("ryan.t.carlson@cru.org"));
    }

    private static Set<EmailAddress> buildProductionSubscribers()
    {
        return ImmutableSet.of(
            EmailAddress.valueOf("matt.drees@cru.org"),
            EmailAddress.valueOf("ryan.t.carlson@cru.org"),
            EmailAddress.valueOf("steve.bratton@cru.org"),
            EmailAddress.valueOf("william.randall@cru.org"),
            EmailAddress.valueOf("luis.rodriguez@cru.org"));
    }
    
    private static List<Node> buildA21()
    {
        return ImmutableList.of(new Node("twdssa21", "twdssa21.net.ccci.org"));
    }
    
    private static List<Node> buildA31()
    {
        return ImmutableList.of(new Node("swdssa31", "swdssa31.net.ccci.org"));
    }
    
    private static List<Node> buildA32()
    {
        return ImmutableList.of(new Node("swdssa32", "swdssa32.net.ccci.org"));
    }
    
    private static List<Node> buildA31A32()
    {
        return ImmutableList.copyOf(
            Iterables.concat(buildA31(), buildA32()));
    }
    
    private static List<Node> buildA41()
    {
        return ImmutableList.of(new Node("pwdssa41", "pwdssa41.net.ccci.org"));
    }
    
    private static List<Node> buildA42()
    {
        return ImmutableList.of(new Node("pwdssa42", "pwdssa42.net.ccci.org"));
    }
    
    private static List<Node> buildA41A42()
    {
        return ImmutableList.copyOf(
            Iterables.concat(buildA41(), buildA42()));
    }

    public List<Node> listNodes()
    {
        return nodes;
    }

    public Set<EmailAddress> getDeploymentSubscribers()
    {
        return deploymentSubscribers;
    }
    
}