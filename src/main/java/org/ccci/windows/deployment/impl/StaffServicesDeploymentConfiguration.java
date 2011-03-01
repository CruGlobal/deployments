package org.ccci.windows.deployment.impl;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.ccci.deployment.AppserverInterface;
import org.ccci.deployment.BasicWebappDeployment;
import org.ccci.deployment.DeploymentFileDescription;
import org.ccci.deployment.SpecificDirectoryDeploymentStorage;
import org.ccci.deployment.DeploymentConfiguration;
import org.ccci.deployment.DeploymentTransferInterface;
import org.ccci.deployment.LocalDeploymentStorage;
import org.ccci.deployment.Node;
import org.ccci.deployment.RestartType;
import org.ccci.deployment.WebappControlInterface;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.WebappDeployment.Packaging;
import org.ccci.util.mail.EmailAddress;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

public class StaffServicesDeploymentConfiguration implements DeploymentConfiguration
{

    public enum StaffServicesEnvironment
    {
        STAGING(
            "Tomcat/instances/ss-inst", 
            "Tomcat - Staff Services", 
            buildA012(),
            true,
            buildProjectLead()),
        TEST(
            "Tomcat/instances/ss-inst-test", 
            "Tomcat - Staff Services - Test", 
            buildA012(),
            true,
            buildProjectLead()),
        SIEBEL_TEST(
            "W$/Tomcat/instances/ss-inst-siebel", 
            "Tomcat - Staff Services - Siebel", 
            buildA321(),
            false,
            buildProjectLead()),
        PRODUCTION(
            "/Tomcat/instances/ss-inst", 
            "Tomcat - Staff Services", 
            buildA041A042(),
            true,
            buildProductionSubscribers()),
        PROD1(
            "/Tomcat/instances/ss-inst", 
            "Tomcat - Staff Services", 
            buildA041(),
            true,
            buildProductionSubscribers()),
        PROD2(
            "/Tomcat/instances/ss-inst", 
            "Tomcat - Staff Services", 
            buildA042(),
            true,
            buildProductionSubscribers());

        public final String serviceName;
        
        /** composed of the network share name followed by the path to the tomcat "base" directory */
        public final String tomcatBasePath;

        public final List<Node> nodes;

        public final boolean moveWebInfLogs;
        
        public Set<EmailAddress> deploymentSubscribers;

        private StaffServicesEnvironment(String tomcatBasePath, 
                                         String serviceName, 
                                         List<Node> nodes, 
                                         boolean moveWebInfLogs,
                                         Set<EmailAddress> deploymentSubscribers)
        {
            this.tomcatBasePath = tomcatBasePath;
            this.serviceName = serviceName;
            this.nodes = nodes;
            this.moveWebInfLogs = moveWebInfLogs;
            this.deploymentSubscribers = deploymentSubscribers;
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

    private final ActiveDirectoryCredential credential;
    private final StaffServicesEnvironment environment;
    private final File sourceDirectory;
    
    public StaffServicesDeploymentConfiguration(
        ActiveDirectoryCredential credential,
        StaffServicesEnvironment environment, 
        File sourceDirectory)
    {
        this.credential = credential;
        this.environment = environment;
        this.sourceDirectory = sourceDirectory;
    }

    @Override
    public AppserverInterface buildAppserverInterface(Node node)
    {
        return new TomcatWindowsAppserverInterface(node, environment.serviceName, credential);
    }

    @Override
    public DeploymentTransferInterface connectDeploymentTransferInterface(Node node)
    {
        String remoteDeploymentDirectory = environment.tomcatBasePath + "/webapps";
        String remoteTransferDirectory = environment.tomcatBasePath + "/temp/ss-transfer";
        String remoteBackupDirectory = environment.tomcatBasePath + "/work/ss-previous-deployment";
        return new SmbDeploymentTransferService(
            createEndpoint(node), 
            remoteDeploymentDirectory, 
            remoteTransferDirectory, 
            remoteBackupDirectory);
    }

    private SmbEndpoint createEndpoint(Node node)
    {
        return new SmbEndpoint(
            credential,
            node.getHostname());
    }

    @Override
    public LocalDeploymentStorage buildLocalDeploymentStorage()
    {
        return new SpecificDirectoryDeploymentStorage(sourceDirectory);
    }

    @Override
    public WebappControlInterface buildWebappControlInterface()
    {
        return new StaffServicesWebappControlInterface();
    }

    @Override
    public WebappDeployment buildWebappDeployment()
    {
        BasicWebappDeployment deployment = new BasicWebappDeployment();
        deployment.setName("ss");
        deployment.setDeployedWarName("ss");
        deployment.setPackaging(Packaging.EXPLODED);
        deployment.setConfigFileDescriptor(getConfigFileDescriptor());
        return deployment;
    }

    private DeploymentFileDescription getConfigFileDescriptor()
    {
        DeploymentFileDescription descriptor = new DeploymentFileDescription();
        descriptor.getDeploymentSpecificPaths().addAll(
            ImmutableSet.of(
                "/WEB-INF/casLogoutRegistry.ser", 
                "/WEB-INF/loggedInUsersFw.ser", 
                "/WEB-INF/salaryOptionsSerialized.ser", 
                "/WEB-INF/web.xml",
                "/WEB-INF/classes/Dbio.properties", 
                "/WEB-INF/classes/log4j.properties", 
                "/WEB-INF/classes/servlets.properties", 
                "/WEB-INF/classes/WebMacro.properties",
                "/WEB-INF/classes/obiee.properties"));
        
        descriptor.getIgnoredPaths().addAll(
            ImmutableSet.of(
                "/build.xml",
                "/WEB-INF/logs", 
                "/WEB-INF/misc-lib", 
                "/WEB-INF/notes", 
                "/WEB-INF/sql", 
                "/WEB-INF/SQR", 
                "/WEB-INF/src", 
                "/WEB-INF/test-src"));
        
        if (environment.moveWebInfLogs)
        {
            descriptor.setLogPath("/WEB-INF/logs");
        }
        
        return descriptor;
    }

    @Override
    public RestartType getDefaultRestartType()
    {
        return RestartType.FULL_PROCESS_RESTART;
    }

    @Override
    public List<Node> listNodes()
    {
        return environment.listNodes();
    }

    @Override
    public boolean supportsCautiousShutdown()
    {
        return false;
    }

    @Override
    public Set<EmailAddress> listDeploymentNotificationRecipients()
    {
        return environment.getDeploymentSubscribers();
    }

}
