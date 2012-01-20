package org.ccci.deployment;

import java.util.ServiceLoader;
import java.util.Set;

import org.ccci.deployment.spi.Application;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class ApplicationLookup
{

    public Application lookupApplication(String name)
    {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader thisClassesClassloader = getClass().getClassLoader();
        
        Set<Application> foundApplications = Sets.newHashSet();
        findApplicationsOnClassloader(name, contextClassLoader, foundApplications);
        if (contextClassLoader != thisClassesClassloader)
        {
            findApplicationsOnClassloader(name, thisClassesClassloader, foundApplications);
        }
        
        Preconditions.checkArgument(!foundApplications.isEmpty(),
            "no application named %s is on the classpath", 
            name);
        
        Preconditions.checkState(foundApplications.size() <= 1,
            "multiple applications named %s are on the classpath: %s", 
            name,
            foundApplications);
        
        return Iterables.getOnlyElement(foundApplications);
    }

    private void findApplicationsOnClassloader(String name, ClassLoader classLoader,
                                               Set<Application> foundApplications)
    {
        ServiceLoader<Application> loader = ServiceLoader.load(Application.class, classLoader);
        for (Application app : loader)
        {
            if (applicationNamed(app, name))
            {
                foundApplications.add(app);
            }
        }
    }
    
    private boolean applicationNamed(Application app, String name)
    {
        return app.getName().toUpperCase().equals(name.toUpperCase().replace('_', ' '));
    }
}