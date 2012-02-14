package org.ccci.deployment.spi;

public class AppserverDeploymentConfiguration
{

    private String stagingDirectory;
    private String installationFileName;
    private String installerScriptName;

    public String getInstallerScriptName()
    {
        return installerScriptName;
    }

    public String getInstallationFileName()
    {
        return installationFileName;
    }

    public String getStagingDirectory()
    {
        return stagingDirectory;
    }

    public void setStagingDirectory(String stagingDirectory)
    {
        this.stagingDirectory = stagingDirectory;
    }

    public void setInstallationFileName(String installationFileName)
    {
        this.installationFileName = installationFileName;
    }

    public void setInstallerScriptName(String installerScriptName)
    {
        this.installerScriptName = installerScriptName;
    }

    
}
