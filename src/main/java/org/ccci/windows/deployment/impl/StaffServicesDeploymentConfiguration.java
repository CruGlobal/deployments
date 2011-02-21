package org.ccci.windows.deployment.impl;

import java.io.File;
import java.util.List;

import org.ccci.deployment.AppserverInterface;
import org.ccci.deployment.BasicWebappDeployment;
import org.ccci.deployment.ConfigFileDescriptor;
import org.ccci.deployment.SpecificDirectoryDeploymentStorage;
import org.ccci.deployment.DeploymentConfiguration;
import org.ccci.deployment.DeploymentTransferInterface;
import org.ccci.deployment.LocalDeploymentStorage;
import org.ccci.deployment.Node;
import org.ccci.deployment.RestartType;
import org.ccci.deployment.WebappControlInterface;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.WebappDeployment.Packaging;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class StaffServicesDeploymentConfiguration implements DeploymentConfiguration
{

    public enum StaffServicesEnvironment
    {
        STAGING(
            "Tomcat/instances/ss-inst", 
            "Tomcat - Staff Services", 
            buildA012()),
        SIEBEL_TEST(
            "W$/Tomcat/instances/ss-inst-siebel", 
            "Tomcat - Staff Services - Siebel", 
            buildA321()),
        PRODUCTION(
            "/Tomcat/instances/ss-inst", 
            "Tomcat - Staff Services", 
            buildA041A042());

        public final String serviceName;
        
        /** composed of the network share name followed by the path to the tomcat "base" directory */
        public final String tomcatBasePath;

        public final List<Node> nodes;

        private StaffServicesEnvironment(String tomcatBasePath, String serviceName, List<Node> nodes)
        {
            this.tomcatBasePath = tomcatBasePath;
            this.serviceName = serviceName;
            this.nodes = nodes;
        }

        private static List<Node> buildA012()
        {
            return ImmutableList.of(new Node("a012", "hart-a012.ccci.org"));
        }
        
        private static List<Node> buildA321()
        {
            return ImmutableList.of(new Node("a321", "hart-a321.ccci.org"));
        }
        
        private static List<Node> buildA041A042()
        {
            return ImmutableList.of(
                new Node("a041", "hart-a041.ccci.org"),
                new Node("a042", "hart-a042.ccci.org"));
        }

        public List<Node> listNodes()
        {
            return nodes;
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
        String remoteDeploymentDirectory = environment.tomcatBasePath + "/webapps/ss";
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
        return new NoOpWebappControlInterface();
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

    private ConfigFileDescriptor getConfigFileDescriptor()
    {
        ConfigFileDescriptor descriptor = new ConfigFileDescriptor();
        descriptor.getDeploymentSpecificClasspathResources().addAll(
            ImmutableSet.of("Dbio.properties", "log4j.properties", "servlets.properties", "WebMacro.properties"));
        descriptor.getDeploymentSpecificWebInfResources().addAll(
            ImmutableSet.of("casLogoutRegistry.ser", "loggedInUsersFw.ser", "salaryOptionsSerialized.ser", "web.xml"));
        descriptor.getIgnoredWebInfDirectories().addAll(
            ImmutableSet.of("logs", "misc-lib", "notes", "sql", "SQR", "src", "test-src"));
        
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

}
