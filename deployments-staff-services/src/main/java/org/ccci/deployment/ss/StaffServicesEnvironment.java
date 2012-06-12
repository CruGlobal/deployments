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
    BRANCHWORK(
        "W$/Tomcat/instances/ss-inst-branch-work", 
        "Tomcat - Staff Services - branch work", 
        buildA321(),
        buildProjectLead(),
        9180),
    TEST(
        "W$/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA321(),
        buildProjectLead(),
        8180),
    STAGING(
        "W$/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA331A332(),
        buildProjectLead(),
        8180),
    STAGE1(
        "W$/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA331(),
        buildProjectLead(),
        8180),
    STAGE2(
        "W$/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA332(),
        buildProjectLead(),
        8180),
    PRODUCTION(
        "W$/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA341A342(),
        buildProductionSubscribers(),
        8180),
    PROD1(
        "/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA341(),
        buildProductionSubscribers(),
        8180),
    PROD2(
        "/Tomcat/instances/ss-inst", 
        "Tomcat - Staff Services", 
        buildA342(),
        buildProductionSubscribers(),
        8180),
    SIEBEL_TEST(
        "W$/Tomcat/instances/ss-inst-siebel", 
        "Tomcat - Staff Services - Siebel", 
        buildA321(),
        buildProjectLead(),
        9380),
    SIEBEL_STAGE1(
        "W$/Tomcat/instances/ss-inst-siebel", 
        "Tomcat - Staff Services - Siebel", 
        buildA331(),
        buildProjectLead(),
        9380),
    SIEBEL_STAGE2(
        "W$/Tomcat/instances/ss-inst-siebel", 
        "Tomcat - Staff Services - Siebel", 
        buildA332(),
        buildProjectLead(),
        9380),
    SIEBEL_STAGING(
        "W$/Tomcat/instances/ss-inst-siebel", 
        "Tomcat - Staff Services - Siebel", 
        buildA331A332(),
        buildProjectLead(),
        9380),
    STAFFWEB_REBRANDING_TEST(
        "/Tomcat/instances/ss-inst-staffweb-rebranding", 
        "Tomcat - Staff Services - Rebranding", 
        buildA321(),
        buildProjectLead(),
        9580),
    STAFFWEB_REBRANDING_STAGING(
        "/Tomcat/instances/ss-inst-staffweb-rebranding", 
        "Tomcat - Staff Services - Rebranding", 
        buildA331A332(),
        buildProjectLead(),
        9580),
    STAFFWEB_REBRANDING_STAGE1(
        "/Tomcat/instances/ss-inst-staffweb-rebranding", 
        "Tomcat - Staff Services - Rebranding", 
        buildA331(),
        buildProjectLead(),
        9580),
    STAFFWEB_REBRANDING_STAGE2(
        "/Tomcat/instances/ss-inst-staffweb-rebranding", 
        "Tomcat - Staff Services - Rebranding", 
        buildA332(),
        buildProjectLead(),
        9580);

    public final String serviceName;
    
    /** composed of the network share name followed by the path to the tomcat "base" directory */
    public final String tomcatBasePath;

    public final List<Node> nodes;

    public Set<EmailAddress> deploymentSubscribers;

    public final int port;
    
    private StaffServicesEnvironment(
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
        return ImmutableSet.of(EmailAddress.valueOf("matt.drees@ccci.org"));
    }

    private static Set<EmailAddress> buildProductionSubscribers()
    {
        return ImmutableSet.of(
            EmailAddress.valueOf("matt.drees@ccci.org"),
            EmailAddress.valueOf("ben.sisson@ccci.org"),
            EmailAddress.valueOf("ryan.t.carlson@ccci.org"),
            EmailAddress.valueOf("linda.ye@ccci.org"),
            EmailAddress.valueOf("steve.bratton@ccci.org"),
            EmailAddress.valueOf("luis.rodriguez@ccci.org"));
    }
    
    private static List<Node> buildA321()
    {
        return ImmutableList.of(new Node("a321", "hart-a321.net.ccci.org"));
    }
    
    private static List<Node> buildA331()
    {
        return ImmutableList.of(new Node("a331", "hart-a331.net.ccci.org"));
    }
    
    private static List<Node> buildA332()
    {
        return ImmutableList.of(new Node("a332", "hart-a332.net.ccci.org"));
    }
    
    private static List<Node> buildA331A332()
    {
        return ImmutableList.copyOf(
            Iterables.concat(buildA331(), buildA332()));
    }
    
    private static List<Node> buildA341()
    {
        return ImmutableList.of(new Node("a341", "hart-a341.net.ccci.org"));
    }
    
    private static List<Node> buildA342()
    {
        return ImmutableList.of(new Node("a342", "hart-a342.net.ccci.org"));
    }
    
    private static List<Node> buildA341A342()
    {
        return ImmutableList.copyOf(
            Iterables.concat(buildA341(), buildA342()));
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