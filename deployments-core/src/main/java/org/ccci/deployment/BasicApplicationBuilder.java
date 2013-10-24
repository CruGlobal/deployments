package org.ccci.deployment;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import org.ccci.deployment.basic.AppserverType;
import org.ccci.deployment.basic.BasicApplication;
import org.ccci.deployment.basic.BasicEnvironment;
import org.ccci.deployment.basic.BasicWebappDeployment;
import org.ccci.deployment.basic.OS;
import org.ccci.deployment.basic.PingConfig;
import org.ccci.deployment.basic.StaticConfig;
import org.ccci.deployment.spi.Application;
import org.ccci.util.mail.EmailAddress;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Matt Drees
 */
public class BasicApplicationBuilder {

    /**
     * Builds a {@link BasicApplication} from a given yml configuration file
     *
     * @throws  IllegalArgumentException if the configuration file is invalid
     */
    public Application buildFrom(File applicationConfiguration) {
        Preconditions.checkNotNull(applicationConfiguration);
        Preconditions.checkArgument(applicationConfiguration.exists(), applicationConfiguration + " does not exist");
        Preconditions.checkArgument(applicationConfiguration.isFile(), applicationConfiguration + " is not a file");

        return parseAndBuildBasicApplication(applicationConfiguration);
    }

    private Application parseAndBuildBasicApplication(File applicationConfiguration) {

        BasicApplicationConfig config = parse(applicationConfiguration);
        return buildFromConfig(config);
    }

    private BasicApplicationConfig parse(File applicationConfiguration) {
        Yaml yaml = new Yaml();
        yaml.setBeanAccess(BeanAccess.FIELD);
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(applicationConfiguration);
        } catch (FileNotFoundException e) {
            throw new AssertionError(applicationConfiguration + " was checked to exist");
        }
        try
        {
            return yaml.loadAs(inputStream, BasicApplicationConfig.class);
        }
        finally
        {
            Closeables.closeQuietly(inputStream);
        }
    }

    private BasicApplication buildFromConfig(BasicApplicationConfig config) {
        StaticConfig staticConfig = new StaticConfig();
        staticConfig.setEnvironments(buildEnvironments(config));
        staticConfig.setPingConfig(buildPingConfig(config));
        staticConfig.setApplicationName(config.applicationName);
        staticConfig.setWaitTimeBetweenNodes(config.waitTimeBetweenNodes);
        staticConfig.setDeployment(buildDeployment(config));
        staticConfig.setOs(buildOs(config));
        staticConfig.setType(buildType(config));

        return new BasicApplication(staticConfig);
    }

    private Map<String, BasicEnvironment> buildEnvironments(BasicApplicationConfig config) {
        ImmutableMap.Builder<String, BasicEnvironment> environments = ImmutableMap.builder();
        for(Map.Entry<String, Environment> entry : config.environments.entrySet())
        {
            String name = entry.getKey();
            Environment configEnvironment = entry.getValue();
            List<Node> nodes = buidNodes(configEnvironment.nodes);
            Set<EmailAddress> deploymentSubscribers = buildDeploymentSubscribers(configEnvironment.deploymentSubscribers);

            environments.put(name, new BasicEnvironment(
                configEnvironment.serviceName,
                configEnvironment.port,
                configEnvironment.appserverBasePath,
                nodes,
                deploymentSubscribers
            ));
        }
        return environments.build();
    }

    private Set<EmailAddress> buildDeploymentSubscribers(List<String> deploymentSubscribers) {
        ImmutableSet.Builder<EmailAddress> subscribers = ImmutableSet.builder();
        for (String deploymentSubscriber : deploymentSubscribers) {
            subscribers.add(EmailAddress.valueOf(deploymentSubscriber));
        }
        return subscribers.build();
    }

    private List<Node> buidNodes(List<NodeConfig> configNodes) {
        ImmutableList.Builder<Node> builder = ImmutableList.builder();
        for (NodeConfig configNode : configNodes) {
            builder.add(new Node(
                configNode.name,
                configNode.hostname
            ));
        }
        return builder.build();
    }

    private PingConfig buildPingConfig(BasicApplicationConfig config) {
        Ping ping = config.deploymentVerification.ping;
        Preconditions.checkArgument(ping != null, "at the moment, 'ping' is the only kind of deployment verification");

        return new PingConfig(
            ping.path,
            ping.expectedContentRegex,
            ping.secondsBeforeTimeout);
    }


    private OS buildOs(BasicApplicationConfig config) {
        return OS.valueOf(config.os.toUpperCase());
    }

    private AppserverType buildType(BasicApplicationConfig config) {
        return AppserverType.valueOf(config.type.toUpperCase());
    }

    private BasicWebappDeployment buildDeployment(BasicApplicationConfig config) {
        BasicWebappDeployment deployment = new BasicWebappDeployment();
        deployment.setName(config.deployment.name);
        deployment.setDeployedWarName(config.deployment.name);

        if (config.deployment.explodedPackaging != null)
        {
            deployment.setPackaging(WebappDeployment.Packaging.EXPLODED);
            deployment.setDeploymentFileDescription(buildDeploymentFileDescription(config));
        }
        else
        {
            deployment.setPackaging(WebappDeployment.Packaging.ARCHIVE);
        }
        return deployment;
    }

    private DeploymentFileDescription buildDeploymentFileDescription(BasicApplicationConfig config) {
        DeploymentFileDescription description = new DeploymentFileDescription();
        description.getDeploymentSpecificPaths().addAll(config.deployment.explodedPackaging.deploymentSpecificPaths);
        description.getIgnoredPaths().addAll(config.deployment.explodedPackaging.ignoredPaths);
        description.setLogPath(config.deployment.explodedPackaging.logPath);
        return description;
    }


    public static class BasicApplicationConfig {
        String type;
        String os;
        String applicationName;
        DeploymentConfig deployment;
        int waitTimeBetweenNodes = 10;
        DeploymentVerification deploymentVerification;
        Map<String, Environment> environments;
    }

    private static class DeploymentConfig {
        String name;
        ExplodedPackaging explodedPackaging;
    }

    private static class ExplodedPackaging {
        List<String> deploymentSpecificPaths;
        List<String> ignoredPaths;
        String logPath;
    }

    private static class DeploymentVerification {
        Ping ping;
    }

    private static class Ping {
        String path;
        String expectedContentRegex;
        int secondsBeforeTimeout;
    }

    private static class Environment {
        String appserverBasePath;
        String serviceName;
        List<NodeConfig> nodes;
        List<String> deploymentSubscribers;
        int port;
    }

    private static class NodeConfig {
        String name;
        String hostname;
    }

}
