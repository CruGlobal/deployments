package org.ccci.deployment.ss;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.ccci.deployment.BasicWebappDeployment;
import org.ccci.deployment.DeploymentFileDescription;
import org.ccci.deployment.SpecificDirectoryDeploymentStorage;
import org.ccci.deployment.Node;
import org.ccci.deployment.RestartType;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.WebappDeployment.Packaging;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.deployment.spi.DeploymentTransferInterface;
import org.ccci.deployment.spi.LoadbalancerInterface;
import org.ccci.deployment.spi.LocalDeploymentStorage;
import org.ccci.deployment.spi.WebappControlInterface;
import org.ccci.deployment.windows.SmbDeploymentTransferService;
import org.ccci.deployment.windows.TomcatWindowsAppserverInterface;
import org.ccci.util.mail.EmailAddress;
import org.ccci.windows.smb.ActiveDirectoryCredential;
import org.ccci.windows.smb.SmbEndpoint;

import com.google.common.collect.ImmutableSet;

public class StaffServicesDeploymentConfiguration implements DeploymentConfiguration
{

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
    public WebappControlInterface buildWebappControlInterface(Node node)
    {
        return new StaffServicesWebappControlInterface(node.getHostname(), environment.port);
    }

    @Override
    public WebappDeployment buildWebappDeployment()
    {
        BasicWebappDeployment deployment = new BasicWebappDeployment();
        deployment.setName("ss");
        deployment.setDeployedWarName("ss");
        deployment.setPackaging(Packaging.EXPLODED);
        deployment.setDeploymentFileDescription(getConfigFileDescriptor());
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
                "/WEB-INF/classes/datasources.properties", 
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

    @Override
    public LoadbalancerInterface buildLoadBalancerInterface()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void closeResources()
    {
    }

    @Override
    public int getWaitTimeBetweenNodes()
    {
        return 30;
    }

}
