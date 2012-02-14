package org.ccci.deployment;

public interface FailoverDatabaseControlInterface
{

    void recoverDataFromFailoverDatabase();

    void prepareDataForFailover();

}
