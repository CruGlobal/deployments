package org.ccci.deployment.linux;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.ccci.annotations.NotThreadSafe;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.spi.DeploymentTransferInterface;
import org.ccci.deployment.spi.LocalDeploymentStorage;
import org.ccci.ssh.SshSession;

import com.google.common.base.Throwables;

@NotThreadSafe
public class SshDeploymentTransferInterface implements DeploymentTransferInterface
{
    private final Logger log = Logger.getLogger(SshDeploymentTransferInterface.class);
    
    
    public SshDeploymentTransferInterface(SshSession session, String jbossServerPath)
    {
        this.session = session;
        this.jbossServerPath = jbossServerPath;
        this.jbossDeploymentPath = getJbossDeploymentPath(jbossServerPath);
    }

    private String getJbossDeploymentPath(String jbossServerPath)
    {
        return jbossServerPath + "/deploy";
    }

    private final String jbossDeploymentPath ;
    private final SshSession session;
    private final String jbossServerPath;
    
    
    //currently, do only one backup
    @Override
    public void backupOldDeploymentAndActivateNewDeployment(WebappDeployment deployment)
    {
        String warFileName = getDeployedWarFileName(deployment);
        String transferFilePath = "/tmp" + "/" + warFileName + ".tmp";
        String backupPath = getBackupFilePath(warFileName);
        String webappDeploymentPath = jbossDeploymentPath + "/" + warFileName;
        
        try
        {
            session.executeSingleCommand(buildMoveCommand(webappDeploymentPath, backupPath));
            try
            {
                session.executeSingleCommand(buildCopyCommand(transferFilePath, webappDeploymentPath));
            }
            catch (Exception e)
            {
                log.warn("Old deployment was backed up, but new deployment could not be deployed.  Manual recovery is needed!");
                Throwables.propagateIfPossible(e, IOException.class);
            }
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }

    private String getBackupFilePath(String warFileName)
    {
        return jbossServerPath + "/backups/" + warFileName;
    }

    @Override
    public void rollbackCurrentDeploymentAndActivateBackedUpDeployment(WebappDeployment deployment)
    {
        String warFileName = getDeployedWarFileName(deployment);
        String backupPath = getBackupFilePath(warFileName);
        String rolledBackDeploymentsPath = jbossDeploymentPath + "/rolledBackDeployments/" + warFileName;
        
        String webappDeploymentPath = jbossDeploymentPath + "/" + warFileName;
        try
        {
            session.executeSingleCommand(buildMoveCommand(webappDeploymentPath, rolledBackDeploymentsPath));
            try
            {
                session.executeSingleCommand(buildMoveCommand(backupPath, warFileName));
            }
            catch (Exception e)
            {
                log.warn("Current deployment was rolled back, but old deployment could not be restored.  Manual recovery is needed!");
                Throwables.propagateIfPossible(e, IOException.class);
            }
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }

    private String buildMoveCommand(String currentPath, String destinationPath)
    {
        return "sudo -u jboss mv " + currentPath + " " + destinationPath;
    }
    
    private String buildCopyCommand(String currentPath, String destinationPath)
    {
        return "sudo -u jboss cp " + currentPath + " " + destinationPath;
    }

    @Override
    public void transferNewDeploymentToServer(WebappDeployment deployment, LocalDeploymentStorage localStorage)
    {
        String warFileName = getDeployedWarFileName(deployment);
        
        String localFilePath = localStorage.getDeploymentLocation().getPath() + "/" + deployment.getName() + ".war";
        try
        {
            session.sendFile(localFilePath, "/tmp", warFileName + ".tmp", "0644");
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }

    private String getDeployedWarFileName(WebappDeployment deployment)
    {
        return deployment.getDeployedWarName() + ".war";
    }

}
