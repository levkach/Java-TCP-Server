/*
******
Created on 25/09/2018
by Lev Kolomazov (levkach@ya.ru)
******
*/
package robot;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.regex.Pattern;

public class EchoConnectionInstance implements Runnable {

    private Socket client;

    public EchoConnectionInstance(Socket client) {
        this.client = client;
    }

    public void run() {
        try {

            //Initialize Variables
            BufferedReader r = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter w = new PrintWriter(client.getOutputStream());
            // Execute time period communication loop
            long period = 45000L;
            long start = System.currentTimeMillis();

            while (System.currentTimeMillis() - start < period) {

                // Test account
                // Robot345
                // 674

                // Read login
                log("200 LOGIN\r\n", w);
                String username = readInput(r, w);
                assert username != null;
                username = username.substring(0, username.length() - 2);

                //Read passphrase
                log("201 PASSWORD\r\n", w);
                String passphrase = readInput(r, w);
                assert passphrase != null;
                // Check login syntax
                if (!passphrase.matches("\\d+\r\n") || !username.matches("Robot.*")) {
                    System.err.println("BAD SYNTAX. CLIENT DISCONNECTED.");
                    log("500 LOGIN FAILED \r\n", w);
                    client.close();
                } else {
                    int password = Integer.parseInt(passphrase.substring(0, passphrase.length() - 2));
                    if (getChecksum(username) == password) {
                        log("202 OK\r\n", w);
                        System.err.println("CLIENT " + username + " CONNECTED");
                    } else {
                        System.err.println("CLIENT DISCONNECTED. CHECKSUM: " + getChecksum(username) + "; ENTERED: " + password);
                        log("500 LOGIN FAILED \r\n", w);
                        client.close();
                    }
                }

                // Main communication with client

                // Messages backlog
                StringBuilder convo = new StringBuilder();


                while (true) {
                    String command = readCommand(r, w);
                    assert command != null;
                    command = command.substring(0, command.length() - 2);
                    if (command.matches("INFO .+")) {
                        // INFO command

                        convo.append(command);
                        log("202 OK \r\n", w);
                    } else if (command.matches("FOTO .+")) {
                        // FOTO command

                        //Retrieve arguments
                        String[] arguments = command.split(" ");
                        int size = Integer.parseInt(arguments[1]);
                        String temp = arguments[2];
                        //Compute actual checksum
                        byte[] data = temp.substring(0,size).getBytes();
                        String dataChecksum = Integer.toHexString(getChecksum(data));

                        //Compare with entered checksum
                        String enteredChecksum = temp.substring(size);


                        //Check checksum

                        // Create and save the image
                        // TODO: What's the image transport protocol?
                        // Until found, save as TXT

                        log("202 OK \r\n", w);
                    }
                }
            }
            //45 secs exceeded
            log("502 TIMEOUT\r\n", w);
            //Connection closure
            client.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }


    // Reads a line of input
    private String readInput(BufferedReader r, PrintWriter w) {
        try {
            String line = "";
            char symbol = ' ';
            while (!line.contains("\r\n")) {
                symbol = (char) r.read();
                line += symbol;
            }
            return line;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Reads a command from input and performs syntax checking
    private String readCommand(BufferedReader r, PrintWriter w) {
        try {
            String line = "";
            char symbol = ' ';
            while (!line.contains("\r\n")) {
                symbol = (char) r.read();
                line += symbol;
                if (!line.isEmpty() && !Pattern.matches(
                        "(I)|(IN)|(INF)|(INFO)|(INFO )|(INFO \\S)|(INFO \\S.+)|(INFO \\S.+\r)|(INFO \\S.+\r\n)" +
                                "|" +
                                "(F)|(FO)|(FOT)|(FOTO)|(FOTO )|(FOTO [1-9][0-9]*)|(FOTO [1-9][0-9]* )|(FOTO [1-9][0-9]* \\S+)|(FOTO [1-9][0-9]* \\S+\r)|(FOTO [1-9][0-9]* \\S+\r\n)", line)) {
                    // INFO command Syntax: INFO Teplotní senzor č.3 se porouchal.\r\n
                    // FOTO command Syntax: FOTO 2775 @xfv8aw**<%#cd^aa ...(celkem 2775 bytů binárních dat) (4 byty kontrolního součtu)
                    // Bad command. Send 501 and quit.
                    log("501 SYNTAX ERROR \r\n", w);
                    client.close();
                    return null;
                }
            }
            return line;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void log(String message, PrintWriter w) {
        try {
            w.print(message);
            w.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getChecksum(String s) {
        int sum = 0;
        for (int i = 0; i < s.length(); i++) {
            sum += (byte) s.charAt(i);
        }
        return sum;
    }

    private int getChecksum(byte[] arr) {
        int sum = 0;
        for (byte anArr : arr) {
            sum += anArr;
        }
        return sum;
    }
}
