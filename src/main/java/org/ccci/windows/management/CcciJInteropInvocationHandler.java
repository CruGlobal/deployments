package org.ccci.windows.management;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.kohsuke.jinterop.JIProxy;
import org.kohsuke.jinterop.JInteropInvocationHandler;

import com.google.common.base.Throwables;

/**
 * Khosuke's invocation handler forgets to check for {@link IJIComObject} method
 * invocations.  So, for example, calling {@link IJIComObject#setInstanceLevelSocketTimeout(int)} 
 * results in a DCOM invocation with the same name, which results in a 'Unknown name' error.
 * 
 * So this works around that by checking for this case.
 * 
 * TODO: submit a patch
 * 
 * @author Matt Drees
 */
public class CcciJInteropInvocationHandler implements InvocationHandler
{

    private final JInteropInvocationHandler delegate;
    private final IJIDispatch obj;
    
    
    public CcciJInteropInvocationHandler(JInteropInvocationHandler delegate, IJIDispatch obj)
    {
        this.delegate = delegate;
        this.obj = obj;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (method.getDeclaringClass() == IJIComObject.class)
        {
            return method.invoke(obj, args);
        }
        else 
        {
            Class<?> proxyType;
            if(method.getDeclaringClass()==JIProxy.class) {
                // cast method
                proxyType = (Class<?>) args[0];
            }
            else
            {
                proxyType = method.getReturnType();
            }
            return replaceProxy(delegate.invoke(proxy, method, args), proxyType);
        }
    }

    private Object replaceProxy(Object returnValue, Class<?> proxyType)
    {
        if (returnValue == null)
            return null;
        if (Proxy.isProxyClass(returnValue.getClass()))
        {
            InvocationHandler handler = Proxy.getInvocationHandler(returnValue);
            if (handler instanceof JInteropInvocationHandler)
            {
                JInteropInvocationHandler jInteropInvocationHandler = (JInteropInvocationHandler) handler;
                IJIDispatch core = getCore(jInteropInvocationHandler);
                return wrap(proxyType.asSubclass(JIProxy.class), core);
            }
            else
            {
                return returnValue;
            }
        }
        else
        {
            return returnValue;
        }
    }
    
    private IJIDispatch getCore(JInteropInvocationHandler invocationHandler)
    {
        Field coreField;
        try
        {
            coreField = JInteropInvocationHandler.class.getDeclaredField("core");
        }
        catch (NoSuchFieldException e)
        {
            throw Throwables.propagate(e);
        }
        coreField.setAccessible(true);
        try
        {
            return (IJIDispatch) coreField.get(invocationHandler);
        }
        catch (IllegalAccessException e)
        {
            throw new AssertionError("field was made accessible");
        }
    }



    /**
     * See {@link JInteropInvocationHandler#wrap(Class, IJIDispatch)}
     */
    public static <T extends JIProxy> T wrap(Class<T> type, IJIDispatch obj) {
        return type.cast(Proxy.newProxyInstance(
            type.getClassLoader(), 
            new Class[]{type}, 
            new CcciJInteropInvocationHandler(new JInteropInvocationHandler(obj,type), obj)));
    }

    /**
     * See {@link JInteropInvocationHandler#wrap(Class, IJIComObject)}
     */
    public static <T extends JIProxy> T wrap(Class<T> type, IJIComObject obj) throws JIException {
        return wrap(type,(IJIDispatch) JIObjectFactory.narrowObject(obj.queryInterface(IJIDispatch.IID)));
    }


}
