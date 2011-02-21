package org.ccci.windows.deployment.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Set;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import org.ccci.deployment.ConfigFileDescriptor;
import org.ccci.deployment.DeploymentTransferInterface;
import org.ccci.deployment.LocalDeploymentStorage;
import org.ccci.deployment.WebappDeployment;
import org.ccci.deployment.WebappDeployment.Packaging;

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
    
    
    public SmbDeploymentTransferService(SmbEndpoint endpoint, String remoteDeploymentDirectory, String remoteTransferDirectory, String remoteBackupDirectory)
    {
        this.endpoint = endpoint;
        this.remoteDeploymentDirectory = remoteDeploymentDirectory;
        this.remoteTransferDirectory = remoteTransferDirectory;
        this.remoteBackupDirectory = remoteBackupDirectory;
    }

    @Override
    public void backupOldDeploymentAndActivateNewDeployment(WebappDeployment deployment)
    {
        try
        {
            SmbFile transferPath = getRemoteTransferPath(deployment);
            Preconditions.checkState(transferPath.exists(), "transfer path does not exist: %s", transferPath);
            SmbFile deploymentPath = getRemoteDeploymentPath(deployment);
            Preconditions.checkState(deploymentPath.exists(), "deployment path does not exist: %s", deploymentPath);

            SmbFile backupPath = getRemoteBackupPath(deployment);
            
            if (backupPath.exists())
            {
                backupPath.delete();
            }
            
            deploymentPath.renameTo(backupPath);
            assert !deploymentPath.exists();
            
            transferPath.renameTo(deploymentPath);
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
    }

    private SmbFile getRemoteBackupPath(WebappDeployment deployment) throws SmbException, MalformedURLException, UnknownHostException
    {
        return initRemoteDirectoryAndBuildPath(remoteBackupDirectory, deployment.getName() + ".bak");
    }

    private SmbFile getRemoteDeploymentPath(WebappDeployment deployment) throws SmbException, MalformedURLException, UnknownHostException
    {
        return initRemoteDirectoryAndBuildPath(remoteDeploymentDirectory, deployment.getName());
    }

    private SmbFile getRemoteTransferPath(WebappDeployment deployment) throws SmbException, MalformedURLException, UnknownHostException
    {
        return initRemoteDirectoryAndBuildPath(remoteTransferDirectory, deployment.getName() + ".tmp");
    }

    private SmbFile getRemoteRollbackPath(WebappDeployment deployment) throws SmbException, MalformedURLException, UnknownHostException
    {
        return initRemoteDirectoryAndBuildPath(remoteBackupDirectory, deployment.getName() + ".rolledback");
    }
    
    private SmbFile initRemoteDirectoryAndBuildPath(String directory, String name) throws SmbException, MalformedURLException, UnknownHostException
    {
        SmbFile remoteDirectory = endpoint.createSmbFile(directory);
        remoteDirectory.mkdirs();
        return new SmbFile(remoteDirectory, name);
    }

    @Override
    public void close()
    {
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
            // if for some reason we can't save the bad deployment, we'll just try to just delete it.  If this fails, we'll have to abort.
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


    @Override
    public void transferNewDeploymentToServer(WebappDeployment deployment, LocalDeploymentStorage localStorage)
    {
        assert deployment.getPackaging() == Packaging.EXPLODED;
        
        File deploymentLocation = localStorage.getDeploymentLocation();
        
        File localDeploymentPath = new File(deploymentLocation, deployment.getName());
        assert localDeploymentPath.isDirectory();
        assert localDeploymentPath.exists();
        
        try
        {
            SmbFile remoteTransferPath = getRemoteTransferPath(deployment);
            copyDeployment(localDeploymentPath, remoteTransferPath, deployment.getConfigFileDescriptor());
            copyConfigFilesFromCurrentToNewDeployment(remoteTransferPath, getRemoteDeploymentPath(deployment), deployment.getConfigFileDescriptor());
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
        
    }

    private void copyConfigFilesFromCurrentToNewDeployment(SmbFile remoteTransferPath, SmbFile remoteDeploymentPath, ConfigFileDescriptor configFileDescriptor) throws MalformedURLException, UnknownHostException, SmbException
    {
        SmbFile currentWebInfDir = new SmbFile(remoteDeploymentPath, "WEB-INF");
        SmbFile newWebInfDir = new SmbFile(remoteTransferPath, "WEB-INF");
        copyRemoteFiles(currentWebInfDir, newWebInfDir, configFileDescriptor.getDeploymentSpecificWebInfResources());
        
        SmbFile currentClassesDir = new SmbFile(currentWebInfDir, "classes");
        SmbFile newClassesDir = new SmbFile(newWebInfDir, "classes");
        copyRemoteFiles(currentClassesDir, newClassesDir, configFileDescriptor.getDeploymentSpecificClasspathResources());
    }

    private void copyRemoteFiles(SmbFile sourceDir, SmbFile targetDir, Set<String> filenames)
            throws MalformedURLException, UnknownHostException, SmbException
    {
        for (String filename : filenames)
        {
            SmbFile sourceFile = new SmbFile(sourceDir, filename);
            SmbFile targetFile = new SmbFile(targetDir, filename);
            assert !targetFile.exists();
            sourceFile.copyTo(targetFile);
        }
    }

    private void copyDeployment(File deploymentDir, SmbFile destination, ConfigFileDescriptor configFileDescriptor) throws IOException
    {
        if (destination.exists())
        {
            destination.delete();
        }
        destination.mkdir();
        recursivelyCopy(deploymentDir, destination, configFileDescriptor);
    }

    
    private void recursivelyCopy(File localDir, SmbFile destinationDir, final ConfigFileDescriptor configFileDescriptor) throws IOException
    {
        assert localDir.isDirectory();
        FilenameFilter transferFilter = new FilenameFilter()
        {
            public boolean accept(File dir, String filename)
            {
                if (filename.equals(".svn"))
                    return false;
                
                if (dir.getName().equals("WEB-INF"))
                {
                    Set<String> ignoredDirs = configFileDescriptor.getIgnoredWebInfDirectories();
                    if (ignoredDirs.contains(filename))
                        return false;
                    Set<String> ignoredFiles = configFileDescriptor.getDeploymentSpecificWebInfResources();
                    if (ignoredFiles.contains(filename))
                        return false;
                }
                
                if (dir.getParent().equals("WEB-INF")  && dir.getName().equals("classes") )
                {
                    Set<String> ignoredFiles = configFileDescriptor.getDeploymentSpecificClasspathResources();
                    if (ignoredFiles.contains(filename))
                        return false;
                }
                
                return true;
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
                final SmbFile destinationSubDir = new SmbFile(destinationDir, name);
                destinationSubDir.mkdir();
                recursivelyCopy(child, destinationSubDir, configFileDescriptor);
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
        
        final SmbFile destinationFile = new SmbFile(destinationDir, file.getName());
        destinationFile.createNewFile();
        
        Files.copy(file, new OutputSupplier<OutputStream>()
        {
            public OutputStream getOutput() throws IOException
            {
                return destinationFile.getOutputStream();
            }
        });
    }

}
