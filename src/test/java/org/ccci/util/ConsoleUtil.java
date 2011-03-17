package org.ccci.util;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleUtil
{


    public static String readPasswordFromInput() throws IOException
    {
        System.out.print("password: ");
        Console console = System.console();
        if (console == null)
        {
            return new BufferedReader(new InputStreamReader(System.in)).readLine();
        }
        else
        {
            return new String(console.readPassword());
        }
    }
    
}
