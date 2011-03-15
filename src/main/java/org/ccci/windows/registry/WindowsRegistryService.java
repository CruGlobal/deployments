package org.ccci.windows.registry;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jinterop.dcom.common.IJIAuthInfo;
import org.jinterop.dcom.common.JIException;
import org.jinterop.winreg.IJIWinReg;
import org.jinterop.winreg.JIPolicyHandle;
import org.jinterop.winreg.JIWinRegFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

public class WindowsRegistryService
{

    private final IJIWinReg registry;
    
    
    public WindowsRegistryService(IJIAuthInfo authInfo, String serverName)
    {
        try
        {
            registry = JIWinRegFactory.getSingleTon().getWinreg(authInfo, serverName, true);
        }
        catch (UnknownHostException e)
        {
            throw Throwables.propagate(e);
        }
    }


    public <T> T getHKeyLocalMachineRegistryValue(Class<T> expectedType, String keyPath, String valueName)
    {
        Preconditions.checkArgument(expectedType == Integer.class || expectedType == String.class, "unsupported type: " + expectedType);
        try
        {
            JIPolicyHandle hkeyLocalMachinePolicy = registry.winreg_OpenHKLM();
            try
            {
                return getRegistryValue(expectedType, keyPath, valueName, hkeyLocalMachinePolicy);
            }
            finally
            {
                registry.winreg_CloseKey(hkeyLocalMachinePolicy);
            }
        }
        catch (JIException e)
        {
            throw Throwables.propagate(e);
        }

    }

    private <T> T getRegistryValue(Class<T> expectedType, String keyPath, String valueName,
                                   JIPolicyHandle hkeyLocalMachinePolicy) throws JIException
    {
        JIPolicyHandle keyHandle = registry.winreg_OpenKey(hkeyLocalMachinePolicy, keyPath, IJIWinReg.KEY_READ);
        try
        {
            return getRegistryValueWithPolicyHandle(expectedType, valueName, keyHandle);
        }
        finally
        {
            registry.winreg_CloseKey(keyHandle);
        }
    }


    private <T> T getRegistryValueWithPolicyHandle(Class<T> expectedType, String valueName,
                                                   JIPolicyHandle requestedHandle) throws JIException
    {
        int bufferSize = expectedType == Integer.class ? 4 : 1024;
        
        Object[] typeAndValue = registry.winreg_QueryValue(requestedHandle, valueName, bufferSize);
        Integer type = (Integer) typeAndValue[0];
        byte[] value = (byte[]) typeAndValue[1];
        
        if (expectedType == Integer.class)
        {
            int valueAsInt;
            if (type == IJIWinReg.REG_DWORD)
            {
                valueAsInt = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).getInt(); 
            }
            else if (type == IJIWinReg.REG_SZ)
            {
                String valueAsString = getValueAsString(value);
                valueAsInt = Integer.parseInt(valueAsString);
            }
            else
            {
                throw new RuntimeException("Unexpected registry type: " + type + " for integer value");
            }
            return expectedType.cast(valueAsInt);
        }
        
        else if (expectedType == String.class)
        {
            if (type != IJIWinReg.REG_SZ && type != IJIWinReg.REG_EXPAND_SZ)
            {
                throw new RuntimeException("Unexpected registry type: " + type + " for string value");
            }
            String valueAsString = getValueAsString(value);
            return expectedType.cast(valueAsString);
        }
        else
        {
            throw new AssertionError("precondition should have prevented this");
        }
    }


    private String getValueAsString(byte[] value)
    {
        String valueAsString = new String(value, Charsets.UTF_16LE);
        return valueAsString;
    }

    Logger log = Logger.getLogger(getClass().getName());

    public void close()
    {
        try
        {
            registry.closeConnection();
        }
        catch (JIException e)
        {
            log.log(Level.WARNING, "exception destroying registry", e);
        }
    }
    
}
