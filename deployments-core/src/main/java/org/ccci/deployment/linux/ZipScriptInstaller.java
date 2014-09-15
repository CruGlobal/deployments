package org.ccci.deployment.linux;

import org.apache.log4j.Logger;
import org.ccci.deployment.ExceptionBehavior;
import org.ccci.deployment.spi.AppserverInterface;
import org.ccci.ssh.SshSession;

import java.io.IOException;

public class ZipScriptInstaller
{


    private final SshSession session;
    private final String runtimeUser;
    private final AppserverInterface appserverInterface;

    private Logger log = Logger.getLogger(getClass());

    public ZipScriptInstaller(
        SshSession session,
        String runtimeUser,
        AppserverInterface appserverInterface)
    {
        this.session = session;
        this.runtimeUser = runtimeUser;
        this.appserverInterface = appserverInterface;
    }

    void install(
        String stagingDirectory,
        String jbossInstallationPackedName,
        String installerScriptName,
        ExceptionBehavior nonfatalExceptionBehavior) throws IOException
    {
        SshSession.AsUser asJboss = session
            .as(runtimeUser);
        log.info("clearing old backup if necessary");
        asJboss.executeSingleCommand("rm -rf " + stagingDirectory + "/installation");
        asJboss.executeSingleCommand("rm -rf " + stagingDirectory + "/jboss-as* " + stagingDirectory + "/jboss-eap* ");
        log.info("unzipping installation archive");
            /* '-q' means 'quiet', '-d' means target directory for extraction */
        asJboss.executeSingleCommand(
            "unzip -q " + stagingDirectory + "/" + jbossInstallationPackedName + " -d " +
            stagingDirectory);
        log.info("stopping jboss");
        appserverInterface.shutdownServer();
        log.info("running installer");
        asJboss.executeSingleCommand(
            "sh " + stagingDirectory + "/installation/" + installerScriptName);
        log.info("starting jboss");
        appserverInterface.startupServer(nonfatalExceptionBehavior);
    }
}