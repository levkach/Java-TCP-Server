/*
******
Created on 21/09/2018
by Lev Kolomazov (levkach@ya.ru)
******
*/
package robot;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Pattern;

public class Robot {

    private ServerSocket server;

    public Robot(int portnum) {
        try {
            server = new ServerSocket(portnum);
        } catch (Exception err) {
            System.out.println(err);
        }
    }

    public static void main(String[] args) {

        try {
            int port = Integer.parseInt(args[0]);
            if (port >= 3000 && port <= 3999) {
                Robot s = new Robot(port);
                s.serve();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void serve() {
        while (true) {
            Socket client = null;
            try {
                client = server.accept();
            } catch (Exception err) {
                err.printStackTrace();
            }
            System.err.println("new client");
            new Thread(new TCPServer(client)).start();
        }
    }


    public class TCPServer implements Runnable {

        private Socket client;
        private BufferedInputStream inp;
        private PrintWriter w;
        private Timer timer;


        class Timer {

            private long period;
            private long start;

            Timer(long period) {
                this.period = period;
            }

            void start() {
                this.start = System.currentTimeMillis();
            }

            boolean isTimedout() {
                return System.currentTimeMillis() - start > period;
            }
        }

        public TCPServer(Socket client) {
            this.client = client;
            //Initialize Variables
            try {
                w = new PrintWriter(client.getOutputStream());
                inp = new BufferedInputStream(client.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                // Execute time period communication loop
                this.timer = new Timer(10000);
                timer.start();
                // Test account
                // Robot345
                // 674
                while (!timer.isTimedout()) {
                    // Read login
                    log("200 LOGIN");
                    int actualChecksum = readUsername();
                    //Read passphrase
                    log("201 PASSWORD");
                    int password = readPassword();
                    // Check login syntax
                    if (actualChecksum == -1 || password == -1) {
                        System.err.println("BAD SYNTAX. CLIENT DISCONNECTED.");
                        log("500 LOGIN FAILED");
                        client.close();
                        break;
                    } else {
                        if (actualChecksum == password) {
                            log("202 OK");
                            System.err.println("CLIENT CONNECTED");
                        } else {
                            System.err.println("CLIENT DISCONNECTED. BAD CHECKSUM.");
                            log("500 LOGIN FAILED");
                            client.close();
                            break;
                        }
                    }

                    // Main communication with client

                    // Messages backlog
                    StringBuilder convo = new StringBuilder();


                    while (true) {
                        String command = readCommand();

                        //If null is returned, syntax was bad and message already sent, just break here
                        if (command == null) {
                            break;
                        }

                        if (command.matches("INFO [\\s\\S]*")) {
                            // INFO command

                            convo.append(command);
                            log("202 OK");
                        } else if (command.matches("FOTO .+")) {
                            // FOTO command

                            //Retrieve arguments
                            String[] arguments = command.split(" ");
                            int size = Integer.parseInt(arguments[1]);
                            String temp = arguments[2];
                            // Check if number of bytes entered matches the entered size
                            if (temp.length() != size) {
                                log("300 BAD CHECKSUM");
                            }
                            //Compute actual checksum
                            byte[] data = temp.substring(0, size).getBytes();
                            String dataChecksum = Integer.toHexString(getChecksum(data));

                            //Compare with entered checksum
                            String enteredChecksum = temp.substring(size);


                            //Check checksum

                            // Create and save the image
                            // TODO: What's the image transport protocol?
                            // Until found, save as TXT

                            log("202 OK");
                        }
                    }
                }
                //45 secs exceeded
                if (timer.isTimedout()) {
                    timeout();
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }


        private void timeout() {
            try {
                if (!client.isClosed()) {
                    log("502 TIMEOUT");
                    client.close();
                    //45 secs exceeded
                    System.exit(502);
                }
            } catch (Exception e){
                e.printStackTrace();
            }

        }

        // Reads username; Returns username checksum or -1, if bad syntax
        private int readUsername() {
            int temp = 0;
            int counter = 0;
            final String right = "Robot";
            int sum = 0;
            char before = ' '; char last = ' ';
            try {
                while (!(before == '\r' && last =='\n')) {
                    before = (char) temp;
                    temp = inp.read();
                    last = (char) temp;
                    if (counter < 5 && right.charAt(counter++) != last) {
                        return -1;
                    }
                    sum += (byte) last;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sum -= (byte)'\r' + (byte)'\n';
            return sum;
        }

        // Reads password. Returns number, or -1 if bad syntax
        private int readPassword() {
            StringBuilder line = new StringBuilder();
            int sum = 0;
            char before = ' '; char last = ' ';
            try {
                while (!line.toString().contains("\r\n")) {
                    line.append((char) inp.read());
                }
                sum = Integer.parseInt(line.substring(0,line.length() - 2));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return -1;
            } catch (Exception stE) {
                stE.printStackTrace();
            }
            return sum;
        }


        // Reads a command from input and performs syntax checking
        private String readCommand() {
            try {
                StringBuilder line = new StringBuilder();
                char symbol = ' ';
                while (!line.toString().endsWith("\r\n")) {
                    symbol = (char) inp.read();
                    line.append(symbol);
                    if (!Pattern.matches(
                            "(I)|(IN)|(INF)|(INFO)|(INFO )|(INFO [\\s\\S]*)" +
                                    "|" +
                                    "(F)|(FO)|(FOT)|(FOTO)|(FOTO )|(FOTO [1-9][0-9]*)|(FOTO [1-9][0-9]* )|(FOTO [1-9][0-9]* \\S+)|(FOTO [1-9][0-9]* \\S+\r)|(FOTO [1-9][0-9]* \\S+\r\n)", line)) {
                        // INFO command Syntax: INFO Teplotni senzor c.3 se porouchal.\r\n
                        // FOTO command Syntax: FOTO 2775 @xfv8aw**<%#cd^aa ...(celkem 2775 bytu binarnich dat) (4 byty kontrolniho souctu)
                        // Bad command. Send 501 and quit.
                        log("501 SYNTAX ERROR");
                        client.close();
                        return null;
                    }
                }
                return line.substring(0, line.length() - 2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }


        private void log(String message) {
            try {
                w.print(message);
                w.print("\r\n");
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


}
