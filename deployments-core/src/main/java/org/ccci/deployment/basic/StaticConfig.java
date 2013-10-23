package org.ccci.deployment.basic;

import org.ccci.deployment.DeploymentFileDescription;
import org.ccci.deployment.WebappDeployment;

import java.util.Map;

/**
 * @author Matt Drees
 */
public class StaticConfig {

    private String applicationName;
    private OS os;
    private AppserverType type;
    private int waitTimeBetweenNodes;
    private PingConfig pingConfig;
    private BasicWebappDeployment deployment;
    private Map<String, BasicEnvironment> environments;

    public Map<String, BasicEnvironment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Map<String, BasicEnvironment> environments) {
        this.environments = environments;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public OS getOs() {
        return os;
    }

    public void setOs(OS os) {
        this.os = os;
    }

    public AppserverType getType() {
        return type;
    }

    public void setType(AppserverType type) {
        this.type = type;
    }

    public int getWaitTimeBetweenNodes() {
        return waitTimeBetweenNodes;
    }

    public void setWaitTimeBetweenNodes(int waitTimeBetweenNodes) {
        this.waitTimeBetweenNodes = waitTimeBetweenNodes;
    }

    public PingConfig getPingConfig() {
        return pingConfig;
    }

    public void setPingConfig(PingConfig pingConfig) {
        this.pingConfig = pingConfig;
    }

    public BasicWebappDeployment getDeployment() {
        return deployment;
    }

    public void setDeployment(BasicWebappDeployment deployment) {
        this.deployment = deployment;
    }
}
