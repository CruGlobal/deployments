import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.testng.annotations.Test;

import com.google.common.base.Throwables;


public class ConnectionTest
{

    @Test
    public void testConnect()
    {

        try
        {
            Socket socket = new Socket("hart-a012.net.ccci.org", 9180);
            System.out.println("connected");
            socket.close();
        }
        catch (UnknownHostException e1)
        {
            throw Throwables.propagate(e1);
        }
        catch (IOException e1)
        {
            throw Throwables.propagate(e1);
        }
        
    }
    
    public static void main(String... args)
    {
        new ConnectionTest().testConnect();
    }
}
