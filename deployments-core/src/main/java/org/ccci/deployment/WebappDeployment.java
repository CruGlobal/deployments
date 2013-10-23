package org.ccci.deployment;

public interface WebappDeployment
{

    
    public enum Packaging
    {
        ARCHIVE, EXPLODED;
    }

    public Version getVersion();
    
    /**
     * The local name of the deployment.
     * If this is a {@link Packaging#ARCHIVE} deployment, the name suffixed with '.war' is
     * the name of the war file.
     * If this is a {@link Packaging#EXPLODED} deployment, the name is the name of the webapp directory.
     */
    //TODO: rename getLocalName()
    public String getName();
    
    /**
     * The remote name of the deployment.
     * The same naming convention as {@link #getName()} applies.
     */
    //TODO: rename getRemoteName()
    public String getDeployedWarName();
    
    public Packaging getPackaging();
    
    /**
     * Only applicable for {@link Packaging#EXPLODED} deployments
     * @return
     */
    public DeploymentFileDescription getDeploymentFileDescriptor();
}
