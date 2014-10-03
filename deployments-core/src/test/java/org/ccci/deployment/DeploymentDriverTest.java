package org.ccci.deployment;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.ccci.deployment.basic.AppserverType;
import org.ccci.deployment.basic.BasicApplication;
import org.ccci.deployment.basic.BasicEnvironment;
import org.ccci.deployment.basic.OS;
import org.ccci.deployment.basic.PingConfig;
import org.ccci.deployment.basic.StaticConfig;
import org.ccci.util.ConsoleUtil;
import org.ccci.util.mail.EmailAddress;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Functional test to test an actual deploy to a server
 *
 * Created by William.Randall on 9/11/2014.
 */
public class DeploymentDriverTest
{
    DeploymentDriver deploymentDriver;

    public void setUp() throws Exception
    {
        deploymentDriver = new DeploymentDriver(createTestOptions());
    }

    public void testDeploy()
    {
        deploymentDriver.deploy();
    }

    private Options createTestOptions() throws IOException
    {
        Options testOptions = new Options();

        testOptions.environment = "checkout";
        testOptions.username = "christopher.randall";
        testOptions.password = ConsoleUtil.readPasswordFromInput();
        testOptions.application = new BasicApplication(createTestStaticConfig());

        testOptions.sourceDirectory = new File("C:/Tomcat/instances/give-inst-checkout/webapps/ROOT");

        return testOptions;
    }

    private StaticConfig createTestStaticConfig()
    {
        StaticConfig staticConfig = new StaticConfig();
        staticConfig.setApplicationName("Give Branded Checkout");
        staticConfig.setDeployment(createTestWebappDeployment());
        staticConfig.setEnvironments(createEnvironments());
        staticConfig.setOs(OS.WINDOWS);
        staticConfig.setPingConfig(createPingConfig());
        staticConfig.setType(AppserverType.TOMCAT);
        staticConfig.setWaitTimeBetweenNodes(30);

        return staticConfig;
    }

    private BasicWebappDeployment createTestWebappDeployment()
    {
        BasicWebappDeployment deployment = new BasicWebappDeployment();
        deployment.setName("ROOT");
        deployment.setPackaging(WebappDeployment.Packaging.EXPLODED);
        deployment.setDeploymentFileDescription(createTestDeploymentFileDescription());
        deployment.setDeployedWarName("ROOT");

        return deployment;
    }

    private DeploymentFileDescription createTestDeploymentFileDescription()
    {
        DeploymentFileDescription deploymentFileDescription = new DeploymentFileDescription();
        Set<String> ignoredPaths = deploymentFileDescription.getIgnoredPaths();
        Set<String> retainedPaths = deploymentFileDescription.getDeploymentSpecificPaths();

        ignoredPaths.addAll(createIgnoredPaths());
        retainedPaths.addAll(createRetainedPaths());

        return deploymentFileDescription;
    }

    private Set<String> createIgnoredPaths()
    {
        return ImmutableSet.of(
            "/build.xml",
            "/deploy.yml",
            "/pom.xml",
            "/WEB-INF/logs",
            "/WEB-INF/misc-lib",
            "/WEB-INF/notes",
            "/WEB-INF/sql",
            "/WEB-INF/SQR",
            "/WEB-INF/src",
            "/WEB-INF/test-src",
            "/give-inst-checkout.iml"
        );
    }

    private Set<String> createRetainedPaths()
    {
        return ImmutableSet.of(
            "/WEB-INF/casLogoutRegistry.ser",
            "/WEB-INF/loggedInUsersFw.ser",
            "/WEB-INF/classes/Dbio.properties",
            "/WEB-INF/classes/Sblio.properties",
            "/WEB-INF/classes/log4j.properties",
            "/WEB-INF/classes/servlets.properties",
            "/WEB-INF/classes/WebMacro.properties",
            "/WEB-INF/classes/obiee.properties",
            "/WEB-INF/classes/siebel.properties",
            "/themes/"
        );
    }

    private Map<String, BasicEnvironment> createEnvironments()
    {
        Set<EmailAddress> deploymentSubscribers = ImmutableSet.of(
            EmailAddress.valueOf("william.randall@cru.org")
        );
        String applicationBasePath = "W$/Tomcat/instances/give-inst-checkout-new";
        List<Node> nodes = Lists.newArrayList(new Node("twdssa21", "twdssa21.net.ccci.org"));

        BasicEnvironment checkoutEnvironment
            = new BasicEnvironment("Tomcat-DSS-BC", 8480, applicationBasePath, nodes, deploymentSubscribers);

        return ImmutableMap.of("checkout", checkoutEnvironment);
    }

    private PingConfig createPingConfig()
    {
        return new PingConfig("/green.html", ".*OK.*", 60);
    }

    public static void main(String... args) throws Exception
    {
        DeploymentDriverTest test = new DeploymentDriverTest();
        test.setUp();
        test.testDeploy();
    }
}
