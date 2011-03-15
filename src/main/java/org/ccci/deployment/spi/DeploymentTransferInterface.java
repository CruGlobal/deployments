package org.ccci.deployment.spi;

import org.ccci.deployment.WebappDeployment;


/**
 * Exposes operations to transfer webapp deployments to a particular node, which is determined at
 * creation time.  This interface must be {@link #close() closed} when no longer needed. 
 * 
 * @author Matt Drees
 */
public interface DeploymentTransferInterface
{

    /**
     * Copies the local deployment to the server.  The web application may or may not be running during this
     * process, so the current deployment on the server should not be disrupted.
     * 
     * @param deployment the deployment to be deployed
     * @param localStorage defines where the local deployment lives
     */
    public void transferNewDeploymentToServer(WebappDeployment deployment, LocalDeploymentStorage localStorage);
    
    /**
     * Removes the current deployment from the appserver, relocating it to a backup location (where it can be restored if needed 
     * via {@link #rollbackCurrentDeploymentAndActivateBackedUpDeployment(WebappDeployment)})
     * and then deploys the new deployment to the appserver.  The app is unavailable during the duration of this method invocation, 
     * so implementations should make this as quick as possible. 
     * 
     * @param deployment the deployment to be deployed
     * @throws IllegalStateException if {@link #transferNewDeploymentToServer(WebappDeployment, LocalDeploymentStorage)} has not yet been called
     */
    public void backupOldDeploymentAndActivateNewDeployment(WebappDeployment deployment);
    
    public void rollbackCurrentDeploymentAndActivateBackedUpDeployment(WebappDeployment deployment);
}
