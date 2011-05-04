package org.ccci.deployment.linux;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.ccci.annotations.NotThreadSafe;
import org.ccci.deployment.ExceptionBehavior;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.spi.DeploymentTransferInterface;
import org.ccci.deployment.spi.LocalDeploymentStorage;
import org.ccci.ssh.SshSession;

import com.google.common.base.Throwables;

@NotThreadSafe
public class SshDeploymentTransferInterface implements DeploymentTransferInterface
{
    private final Logger log = Logger.getLogger(SshDeploymentTransferInterface.class);
    
    
    public SshDeploymentTransferInterface(
        SshSession session, 
        String remoteDeploymentDirectory,
        String remoteTransferDirectory,
        String remoteBackupDirectory)
    {
        this.session = session;
        this.remoteDeploymentDirectory = remoteDeploymentDirectory;
        this.remoteTransferDirectory = remoteTransferDirectory;
        this.remoteBackupDirectory = remoteBackupDirectory;
    }


    private final SshSession session;
    

    private final String remoteDeploymentDirectory;
    private final String remoteTransferDirectory;
    private final String remoteBackupDirectory;
    
    
    //currently, do only one backup
    @Override
    public void backupOldDeploymentAndActivateNewDeployment(WebappDeployment deployment, ExceptionBehavior exceptionBehavior)
    {
        String warFileName = getDeployedWarFileName(deployment);
        String transferFilePath = remoteTransferDirectory + "/" + warFileName + ".tmp";
        String backupPath = getBackupFilePath(warFileName);
        String webappDeploymentPath = remoteDeploymentDirectory + "/" + warFileName;
        
        try
        {
            try
            {
                session.executeSingleCommand(buildMoveCommand(webappDeploymentPath, backupPath));
            }
            catch (Exception e)
            {
                if (exceptionBehavior == ExceptionBehavior.HALT)
                    Throwables.propagate(e);
                else if (exceptionBehavior == ExceptionBehavior.LOG)
                    log.error("unable to back up current deployment; ignoring", e);
                else
                    throw new AssertionError();
            }
            
            
            try
            {
                /* note: we can't use 'mv' here, because the jboss user won't have permission to 
                 * remove the deployment from the transfer location (its owner is whoever is
                 * running the deployment, not jboss)
                 */
                session.executeSingleCommand(buildCopyCommand(transferFilePath, webappDeploymentPath));
            }
            catch (Exception e)
            {
                log.warn("Old deployment was backed up, but new deployment could not be deployed.  Manual recovery is needed!");
                Throwables.propagateIfPossible(e, IOException.class);
            }
            /* cleanup the transfer file, because future transfers may be executed by a different user
             * who won't have permission to overwrite this transfer file
             */
            session.executeSingleCommand("rm " + transferFilePath);
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }

    private String getBackupFilePath(String warFileName)
    {
        return remoteBackupDirectory + "/" + warFileName;
    }

    @Override
    public void rollbackCurrentDeploymentAndActivateBackedUpDeployment(WebappDeployment deployment)
    {
        String warFileName = getDeployedWarFileName(deployment);
        String backupPath = getBackupFilePath(warFileName);
        String rolledBackDeploymentsPath = remoteBackupDirectory + "/" + warFileName + ".rolledback";
        
        String webappDeploymentPath = remoteDeploymentDirectory + "/" + warFileName;
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
            session.sendFile(localFilePath, remoteTransferDirectory, warFileName + ".tmp", "0644");
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
