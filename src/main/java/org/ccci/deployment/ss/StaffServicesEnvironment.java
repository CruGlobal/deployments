package org.ccci.deployment.ss;

import java.util.List;
import java.util.Set;

import org.ccci.deployment.Node;
import org.ccci.util.mail.EmailAddress;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

public enum StaffServicesEnvironment
{
    STAGING(
        "Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA012(),
        true,
        buildProjectLead(),
        8180),
    TEST(
        "Tomcat/instances/ss-inst-test", 
        "Tomcat - Staff Services - Test", 
        buildA012(),
        true,
        buildProjectLead(),
        9180),
    SIEBEL_TEST(
        "W$/Tomcat/instances/ss-inst-siebel", 
        "Tomcat - Staff Services - Siebel", 
        buildA321(),
        false,
        buildProjectLead(),
        9380),
    STAFFWEB_REBRANDING_TEST(
        "/Tomcat/instances/ss-inst-staffweb-rebranding", 
        "Tomcat - Staff Services - Rebranding", 
        buildA321(),
        false,
        buildProjectLead(),
        9580),
    STAFFWEB_REBRANDING_STAGE1(
        "/Tomcat/instances/ss-inst-staffweb-rebranding", 
        "Tomcat - Staff Services - Rebranding", 
        buildA331(),
        false,
        buildProjectLead(),
        9580),
    PRODUCTION(
        "/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA041A042(),
        true,
        buildProductionSubscribers(),
        8180),
    PROD1(
        "/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA041(),
        true,
        buildProductionSubscribers(),
        8180),
    PROD2(
        "/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA042(),
        true,
        buildProductionSubscribers(),
        8180);

    public final String serviceName;
    
    /** composed of the network share name followed by the path to the tomcat "base" directory */
    public final String tomcatBasePath;

    public final List<Node> nodes;

    public final boolean moveWebInfLogs;
    
    public Set<EmailAddress> deploymentSubscribers;

    public final int port;
    
    private StaffServicesEnvironment(
         String tomcatBasePath, 
         String serviceName, 
         List<Node> nodes, 
         boolean moveWebInfLogs,
         Set<EmailAddress> deploymentSubscribers,
         int port)
    {
        this.tomcatBasePath = tomcatBasePath;
        this.serviceName = serviceName;
        this.nodes = nodes;
        this.moveWebInfLogs = moveWebInfLogs;
        this.deploymentSubscribers = deploymentSubscribers;
        this.port = port;
    }


    private static Set<EmailAddress> buildProjectLead()
    {
        return ImmutableSet.of(EmailAddress.valueOf("matt.drees@ccci.org"));
    }

    private static Set<EmailAddress> buildProductionSubscribers()
    {
        return ImmutableSet.of(
            EmailAddress.valueOf("matt.drees@ccci.org"),
            EmailAddress.valueOf("ben.sisson@ccci.org"),
            EmailAddress.valueOf("ryan.t.carlson@ccci.org"),
            EmailAddress.valueOf("steve.bratton@ccci.org"),
            EmailAddress.valueOf("luis.rodriguez@ccci.org"));
    }

    private static List<Node> buildA012()
    {
        return ImmutableList.of(new Node("a012", "hart-a012.net.ccci.org"));
    }
    
    private static List<Node> buildA321()
    {
        return ImmutableList.of(new Node("a321", "hart-a321.net.ccci.org"));
    }
    
    private static List<Node> buildA331()
    {
        return ImmutableList.of(new Node("a331", "hart-a331.net.ccci.org"));
    }
    
    @SuppressWarnings("unused")
    private static List<Node> buildA332()
    {
        return ImmutableList.of(new Node("a332", "hart-a332.net.ccci.org"));
    }
    
    private static List<Node> buildA041()
    {
        return ImmutableList.of(new Node("a041", "hart-a041.net.ccci.org"));
    }
    
    private static List<Node> buildA042()
    {
        return ImmutableList.of(new Node("a042", "hart-a042.net.ccci.org"));
    }
    
    private static List<Node> buildA041A042()
    {
        return ImmutableList.copyOf(
            Iterables.concat(buildA041(), buildA042()));
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