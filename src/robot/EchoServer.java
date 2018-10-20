/*
******
Created on 21/09/2018
by Lev Kolomazov (levkach@ya.ru)
******
*/
package robot;

import java.io.*;
import java.net.*;

public class EchoServer {
    private ServerSocket server;

    public EchoServer(int portnum) {
        try {
            server = new ServerSocket(portnum);
        } catch (Exception err) {
            System.out.println(err);
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        try {
            EchoServer s = new EchoServer(port);
            s.serve();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void serve() {
        while (true) {
            Socket client = null;
            try {
                client = server.accept();
            } catch (Exception err) {
                err.printStackTrace();
            }
            System.err.println("new client");
            new Thread(new EchoConnectionInstance(client)).start();

        }
    }
}
