package org.ccci.deployment;

public enum RestartType
{
    FULL_PROCESS_RESTART("full"), 
    QUICK_WEBAPP_RESTART("quick");
    
    public final String code;

    private RestartType(String code)
    {
        this.code = code;
    }
    
}
