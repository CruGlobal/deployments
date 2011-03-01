package org.ccci.deployment;

import java.io.File;

import org.ccci.deployment.Main.SystemConveter;

import com.beust.jcommander.Parameter;

public class Options
{
    
    @Parameter(
        names = {"--application", "-a"}, 
        description = "which web application to deploy", 
        converter = SystemConveter.class, 
        required = true)
    public Application application;
    
    @Parameter(
        names = {"--environment", "-e"}, 
        description = "which environment (dev, staging, production, etc) to deploy to. Each application defines its own set of possible environments", 
        required = true)
    public String environment;

    @Parameter(
        names = {"--sourceDirectory", "-s"}, 
        description = "which directory contains the application to deploy")
    public File sourceDirectory;
    
    @Parameter(
        names = {"--username", "-u"}, 
        description = "username with rights to copy files and restart services", 
        required = true)
    public String username;
    
    @Parameter(
        names = {"--password", "-p"}, 
        description = "password associated with username", 
        password = true)
    public String password;

    @Parameter(
        names = {"--domain", "-d"}, 
        description = "the Active Directory domain for the username, if this is a deployment to a windows machine", 
        required = true)
    public String domain;

    @Parameter(
        names = {"--continuousIntegrationUrl", "-c"}, 
        description = "the url for the continuous integration server running this deployment")
    public String continuousIntegrationUrl;
}