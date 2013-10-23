package org.ccci.deployment.basic;

import org.ccci.deployment.FailoverDatabaseControlInterface;
import org.ccci.deployment.Node;
import org.ccci.deployment.RestartType;
import org.ccci.deployment.SpecificDirectoryDeploymentStorage;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.deployment.spi.DeploymentConfiguration;
import org.ccci.deployment.spi.DeploymentTransferInterface;
import org.ccci.deployment.spi.LoadbalancerInterface;
import org.ccci.deployment.spi.LocalDeploymentStorage;
import org.ccci.deployment.spi.WebappControlInterface;
import org.ccci.deployment.windows.SimpleWindowsServiceAppserverInterface;
import org.ccci.deployment.windows.SmbDeploymentTransferService;
import org.ccci.util.mail.EmailAddress;
import org.ccci.windows.smb.ActiveDirectoryCredential;
import org.ccci.windows.smb.SmbEndpoint;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * @author Matt Drees
 */
public class BasicDeploymentConfiguration implements DeploymentConfiguration {


    private ActiveDirectoryCredential credential;
    private BasicEnvironment environment;
    private File sourceDirectory;
    private StaticConfig staticConfig;


    @Override
    public AppserverInterface buildAppserverInterface(Node node)
    {
        OS os = staticConfig.getOs();
        if (os == OS.WINDOWS)
        {
            return new SimpleWindowsServiceAppserverInterface(node, environment.serviceName, credential);
        }
        else
            throw new RuntimeException("Not implemented: " + os);
    }

    @Override
    public DeploymentTransferInterface connectDeploymentTransferInterface(Node node) {
        AppserverType appserverType = staticConfig.getType();

        String remoteDeploymentDirectory;
        String remoteTransferDirectory;
        String remoteBackupDirectory;
        if (appserverType == AppserverType.TOMCAT) {
            remoteDeploymentDirectory = environment.appserverBasePath + "/webapps";
            String deploymentName = staticConfig.getDeployment().getDeployedWarName();
            remoteTransferDirectory = environment.appserverBasePath + "/temp/" + deploymentName + "-transfer";
            remoteBackupDirectory = environment.appserverBasePath + "/work/" + deploymentName + "-previous-deployment";
        } else {
            throw new RuntimeException("Not implemented: " + appserverType);
        }

        OS os = staticConfig.getOs();
        if (os == OS.WINDOWS)
        {
            return new SmbDeploymentTransferService(
                createEndpoint(node),
                remoteDeploymentDirectory,
                remoteTransferDirectory,
                remoteBackupDirectory);
        }
        else
            throw new RuntimeException("Not implemented: " + os);
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
        return new BasicWebappControlInterface(node.getHostname(), environment.port, staticConfig.getPingConfig());
    }

    @Override
    public WebappDeployment buildWebappDeployment()
    {
        return staticConfig.getDeployment();
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
        return staticConfig.getWaitTimeBetweenNodes();
    }

    @Override
    public FailoverDatabaseControlInterface buildFailoverDatabaseControl(Node node)
    {
        throw new UnsupportedOperationException();
    }

    public ActiveDirectoryCredential getCredential() {
        return credential;
    }

    public void setCredential(ActiveDirectoryCredential credential) {
        this.credential = credential;
    }

    public BasicEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(BasicEnvironment environment) {
        this.environment = environment;
    }

    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public void setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public void setStaticConfig(StaticConfig staticConfig) {
        this.staticConfig = staticConfig;
    }

    public StaticConfig getStaticConfig() {
        return staticConfig;
    }
}
