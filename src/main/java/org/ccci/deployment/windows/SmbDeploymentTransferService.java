package org.ccci.deployment.windows;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import org.apache.log4j.Logger;
import org.ccci.deployment.DeploymentFileDescription;
import org.ccci.deployment.ExceptionBehavior;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.WebappDeployment.Packaging;
import org.ccci.deployment.spi.DeploymentTransferInterface;
import org.ccci.deployment.spi.LocalDeploymentStorage;
import org.ccci.windows.smb.SmbEndpoint;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.io.OutputSupplier;

public class SmbDeploymentTransferService implements DeploymentTransferInterface
{


    private final SmbEndpoint endpoint;
    
    private final String remoteDeploymentDirectory;
    private final String remoteTransferDirectory;
    private final String remoteBackupDirectory;
    
    
    Logger log = Logger.getLogger(SmbEndpoint.class);
    
    
    public SmbDeploymentTransferService(
        SmbEndpoint endpoint, 
        String remoteDeploymentDirectory, 
        String remoteTransferDirectory, 
        String remoteBackupDirectory)
    {
        this.endpoint = endpoint;
        this.remoteDeploymentDirectory = remoteDeploymentDirectory;
        this.remoteTransferDirectory = remoteTransferDirectory;
        this.remoteBackupDirectory = remoteBackupDirectory;
    }


    @Override
    public void transferNewDeploymentToServer(WebappDeployment deployment, LocalDeploymentStorage localStorage)
    {
        assert deployment.getPackaging() == Packaging.EXPLODED; //for now
        
        File localDeploymentPath = localStorage.getDeploymentLocation();
        assert localDeploymentPath.isDirectory();
        assert localDeploymentPath.exists();
        
        try
        {
            SmbFile remoteTransferPath = getRemoteTransferPath(deployment);
            copyDeployment(localDeploymentPath, remoteTransferPath, deployment.getDeploymentFileDescriptor());
            copyConfigFilesFromCurrentToNewDeployment(remoteTransferPath, getRemoteDeploymentPath(deployment), deployment.getDeploymentFileDescriptor());
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
        
    }
    

    private SmbFile getRemoteBackupPath(WebappDeployment deployment) throws SmbException, MalformedURLException, UnknownHostException
    {
        return initRemoteDirectoryAndBuildPath(remoteBackupDirectory, 
            deployment.getName() + ".bak",
            deployment.getPackaging());
    }

    private SmbFile getRemoteDeploymentPath(WebappDeployment deployment) throws SmbException, MalformedURLException, UnknownHostException
    {
        return initRemoteDirectoryAndBuildPath(remoteDeploymentDirectory, 
            deployment.getName(), 
            deployment.getPackaging());
    }

    private SmbFile getRemoteTransferPath(WebappDeployment deployment) throws SmbException, MalformedURLException, UnknownHostException
    {
        return initRemoteDirectoryAndBuildPath(remoteTransferDirectory, 
            deployment.getName() + ".tmp", 
            deployment.getPackaging());
    }

    private SmbFile getRemoteRollbackPath(WebappDeployment deployment) throws SmbException, MalformedURLException, UnknownHostException
    {
        return initRemoteDirectoryAndBuildPath(remoteBackupDirectory, 
            deployment.getName() + ".rolledback", 
            deployment.getPackaging());
    }
    
    private SmbFile initRemoteDirectoryAndBuildPath(String directory, String name, Packaging packaging) 
        throws SmbException, MalformedURLException, UnknownHostException
    {
        SmbFile remoteDirectory = endpoint.createSmbPath(directory);
        if (remoteDirectory.exists())
        {
            Preconditions.checkState(remoteDirectory.isDirectory());
        }
        else
        {
            remoteDirectory.mkdirs();
        }
        
        return packaging == Packaging.ARCHIVE ? 
                endpoint.createChildFilePath(remoteDirectory, name) :
                endpoint.createChildDirectoryPath(remoteDirectory, name);
    }


    private void copyConfigFilesFromCurrentToNewDeployment(SmbFile remoteTransferPath, SmbFile remoteDeploymentPath, DeploymentFileDescription deploymentFileDescription) throws MalformedURLException, UnknownHostException, SmbException
    {
        for (String path : deploymentFileDescription.getDeploymentSpecificPaths())
        {
            path = removeInitialSlash(path);
            SmbFile sourceFile = endpoint.createChildFilePath(remoteDeploymentPath, path);
            if (sourceFile.exists())
            //when introducing a new config file, the current deployment will not have it yet,
            //so skip the copying.
            {
                SmbFile targetFile = endpoint.createChildFilePath(remoteTransferPath, path);
                assert !targetFile.exists();
                sourceFile.copyTo(targetFile);
            }
        }
    }

    private String removeInitialSlash(String path)
    {
        Preconditions.checkArgument(path.startsWith("/"), "should start with a slash: %s", path);
        return path.substring(1);
    }

    private void copyDeployment(File deploymentDir, SmbFile destination, DeploymentFileDescription deploymentFileDescription) throws IOException
    {
        if (destination.exists())
        {
            destination.delete();
        }
        destination.mkdir();
        recursivelyCopy(deploymentDir, destination, deploymentFileDescription, "");
    }
    
    private void recursivelyCopy(
         File localDir, 
         SmbFile destinationDir, 
         final DeploymentFileDescription deploymentFileDescription, 
         final String currentPath) throws IOException
    {
        assert localDir.isDirectory();
        FilenameFilter transferFilter = new FilenameFilter()
        {
            public boolean accept(File dir, String filename)
            {
                String filepath = currentPath + "/" + filename;
                return
                    !isHiddenFile(filename) &&
                    !deploymentFileDescription.getIgnoredPaths().contains(filepath) && 
                    !deploymentFileDescription.getDeploymentSpecificPaths().contains(filepath);
            }

            /** a hidden file, for example .DS_Store on macs, or eclipse or svn metadata */
            private boolean isHiddenFile(String filename)
            {
                return filename.startsWith(".");
            }
        };
        
        for (File child : localDir.listFiles(transferFilter))
        {
            if (child.isFile())
            {
                copyFile(child, destinationDir);
            }
            else if (child.isDirectory())
            {
                String name = child.getName();
                final SmbFile destinationSubDir = endpoint.createChildDirectoryPath(destinationDir, name);
                destinationSubDir.mkdir();
                String subPath = currentPath + "/" + name;
                recursivelyCopy(child, destinationSubDir, deploymentFileDescription, subPath);
            }
            else
            {
                throw new AssertionError("Not sure what to do with this: " + child);
            }
        }
    }

    private void copyFile(File file, SmbFile destinationDir) throws IOException
    {
        assert file.isFile();
        
        String filename = file.getName();
        final SmbFile destinationFile = endpoint.createChildFilePath(destinationDir, filename);
        destinationFile.createNewFile();
        
        Files.copy(file, new OutputSupplier<OutputStream>()
        {
            public OutputStream getOutput() throws IOException
            {
                return destinationFile.getOutputStream();
            }
        });
    }
    
    @Override
    public void backupOldDeploymentAndActivateNewDeployment(WebappDeployment deployment, ExceptionBehavior exceptionBehavior)
    {
        try
        {
            SmbFile transferPath = getRemoteTransferPath(deployment);
            SmbFile deploymentPath = getRemoteDeploymentPath(deployment);
            checkPaths(transferPath, deploymentPath);
            moveLogFilesToNewDeploymentIfNecessary(deployment, transferPath, deploymentPath);
            try
            {
                try
                {
                    backupCurrentDeployment(deployment, deploymentPath);
                }
                catch (Exception e)
                {
                    if (exceptionBehavior == ExceptionBehavior.HALT)
                        Throwables.propagate(e);
                    else if (exceptionBehavior == ExceptionBehavior.LOG)
                        log.error("Unable to back up current deployment; ignoring", e);
                    else throw new AssertionError();
                }
                activateNewDeployment(transferPath, deploymentPath);
            }
            catch (Exception e)
            {
                log.warn("log files have been moved to the new deployment, " +
                		"but an exception prevents the new deployment from activating.  " +
                		"The logs need to manually be moved back!");
                Throwables.propagate(e);
            }
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }

    private void checkPaths(SmbFile transferPath, SmbFile deploymentPath) throws SmbException
    {
        Preconditions.checkState(transferPath.exists(), "transfer path does not exist: %s", transferPath);
        Preconditions.checkState(deploymentPath.exists(), "deployment path does not exist: %s", deploymentPath);
    }

    private void activateNewDeployment(SmbFile transferPath, SmbFile deploymentPath) throws SmbException
    {
        assert !deploymentPath.exists();
        transferPath.renameTo(deploymentPath);
    }

    private void backupCurrentDeployment(WebappDeployment deployment, SmbFile deploymentPath) throws SmbException,
            MalformedURLException, UnknownHostException
    {
        SmbFile backupPath = getRemoteBackupPath(deployment);
        if (backupPath.exists())
        {
            backupPath.delete();
        }
        deploymentPath.renameTo(backupPath);
    }

    private void moveLogFilesToNewDeploymentIfNecessary(WebappDeployment deployment, SmbFile transferPath, SmbFile deploymentPath)
            throws MalformedURLException, SmbException
    {
        if (deployment.getPackaging() == Packaging.EXPLODED)
        {
            String logPath = deployment.getDeploymentFileDescriptor().getLogPath();
            if (logPath != null)
            {
                SmbFile deploymentLogFilePath = endpoint.createChildDirectoryPath(
                    deploymentPath, removeInitialSlash(logPath));
                    
                SmbFile transferLogFilePath = endpoint.createChildDirectoryPath(
                    transferPath, removeInitialSlash(logPath)); 
                
                deploymentLogFilePath.renameTo(transferLogFilePath);
            }
        }
    }


    @Override
    public void rollbackCurrentDeploymentAndActivateBackedUpDeployment(WebappDeployment deployment)
    {
        try
        {
            SmbFile backupPath = getRemoteBackupPath(deployment);
            Preconditions.checkState(backupPath.exists(), "backup path does not exist: %s", backupPath);
            
            SmbFile deploymentPath = getRemoteDeploymentPath(deployment);
            try
            {
                if (deploymentPath.exists())
                {
                    SmbFile rollbackPath = getRemoteRollbackPath(deployment);
                    
                    if (rollbackPath.exists())
                    {
                        rollbackPath.delete();
                    }
                    deploymentPath.renameTo(rollbackPath);
                }
            }
            catch (SmbException e)
            // if for some reason we can't save the bad deployment, we'll just try to just delete it.  
            // If this fails, we'll have to abort.
            {
                if (deploymentPath.exists())
                {
                    deploymentPath.delete();
                }
            }
            
            backupPath.renameTo(deploymentPath);
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }

}
