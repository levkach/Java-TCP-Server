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
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
//        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
        while (true) {
            Socket client = null;
            try {
                client = server.accept();
            } catch (Exception err) {
                err.printStackTrace();
            }
            new Thread(new TCPServer(client)).start();
        }
    }


    public class TCPServer implements Runnable {

        private Socket client;
        private BufferedInputStream inp;
        private PrintWriter w;
        private StringBuilder convo;

        public TCPServer(Socket client) {
            this.client = client;
            //Initialize Variables
            try {
                w = new PrintWriter(client.getOutputStream());
                inp = new BufferedInputStream(client.getInputStream());
                convo = new StringBuilder();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                // Execute time period communication loop
                // Test account
                // Robot345
                // 674
                while (true) {
                    // Read login
                    log("200");
                    int actualChecksum = readUsername();
                    //Read passphrase
                    log("201");
                    int password = readPassword();
                    // Check login syntax
                    if (actualChecksum == -1 || password == -1) {
                        System.err.println("BAD SYNTAX. CLIENT DISCONNECTED.");
                        log("500");
                        break;
                    } else {
                        if (actualChecksum == password) {
                            System.err.println("CLIENT " + client.getInetAddress().toString() + " CONNECTED");
                            log("202");
                        } else {
                            System.err.println("CLIENT " + client.getInetAddress().toString() + "DISCONNECTED. WRONG PASSWORD");
                            log("500");
                            break;
                        }
                    }
                    // Main communication with client
                    while (true) {
                        if (!client.isClosed()) {
                            readCommand();
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }


        // Reads username; Returns username checksum or -1, if bad syntax
        private int readUsername() {
            int temp;
            int counter = 0;
            final String right = "Robot";
            boolean incorrect = false;
            int sum = 0;
            char before = ' ';
            char last = ' ';
            try {
                while (!(before == '\r' && last == '\n')) {
                    before = last;
                    temp = inp.read();
                    last = (char) temp;
                    if (counter < 5 && right.charAt(counter++) != last) {
                        incorrect = true;
                    }
                    sum += temp;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sum -= (byte) '\r' + (byte) '\n';
            return incorrect ? -1 : sum;
        }

        // Reads password. Returns number, or -1 if bad syntax
        private int readPassword() {
            StringBuilder line = new StringBuilder();
            int sum = 0;
            boolean incorrect = false;
            try {
                while (!line.toString().contains("\r\n")) {
                    line.append((char) inp.read());
                }
                sum = Integer.parseInt(line.substring(0, line.length() - 2));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                incorrect = true;
            } catch (Exception stE) {
                stE.printStackTrace();
            }
            return incorrect ? -1 : sum;
        }


        // Reads a command from input and performs syntax checking
        private void readCommand() {
            try {
                StringBuilder command = new StringBuilder();
                StringBuilder message = new StringBuilder();
                StringBuilder enteredChecksum = new StringBuilder();

                int[] data = null;
                int temp;
                boolean matched = false;
                boolean readyForData = false;
                char before = ' ';
                char last = ' ';
                int size = 0;
                int ind = 0;
                int counted = 0;
                while (!(before == '\r' && last == '\n')) {
                    before = last;
                    temp = inp.read();
                    last = (char) temp;

                    if (!matched) {
                        command.append((char) temp);
                        // Check syntax, as user enters command
                        if (!Pattern.matches(
                                "(I)|(IN)|(INF)|(INFO)|(INFO )" +
                                        "|" +
                                        "(F)|(FO)|(FOT)|(FOTO)|(FOTO )", command)) {
                            log("501");
                        }
                    }

                    if (command.toString().equals("INFO ")) {
                        matched = true;
                        message.append(last);
                    } else if (command.toString().equals("FOTO ")) {
                        matched = true;
                        if (readyForData) {
                            //Foto already read, now read checksum
                            if (ind == size) {
                                enteredChecksum.append((char) temp);
                            } else {
                                //Reading foto and adding to checksum in DEC
                                data[ind++] = temp;
                                counted += temp;
                            }
                        } else {
                            message.append(last);
                            if (message.lastIndexOf(" ") != 0) {
                                // Size read. Parse it and begin reading of foto.
                                size = Integer.parseInt(message.toString().trim());
                                data = new int[size];
                                readyForData = true;
                            }
                        }
                    }
                }


                if (matched) {
                    if (command.toString().equals("FOTO ")) {
                        if (data == null) {
                            log("501");
                        } else {
                            enteredChecksum.setLength(enteredChecksum.length() - 2);
                            long checksum = Long.parseLong(enteredChecksum.toString(), 16);
                            System.err.println("COUNTED: " + counted + "ENTERED: " + checksum);
                            if (checksum == counted) {
                                savePhoto(data);
                                log("202");
                            } else {
                                log("300");
                            }
                        }
                    } else {
                        String msg = "From " + client.getInetAddress().toString() + " recieved: " + message + "\n";
                        convo.append(msg);
                        log("202");
                    }
                }

            } catch (NumberFormatException ex) {
                log("501");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        void log(String s) {
            String message = "";
            boolean toDisconnect = false;
            switch (s) {
                case "200":
                    message = "200 LOGIN";
                    break;

                case "201":
                    message = "201 PASSWORD";
                    break;

                case "202":
                    message = "202 OK";
                    break;

                case "300":
                    message = "300 BAD CHECKSUM";
                    break;

                case "500":
                    message = "500 LOGIN FAILED";
                    toDisconnect = true;
                    break;

                case "501":
                    message = "501 SYNTAX ERROR";
                    toDisconnect = true;
                    break;

                case "502":
                    message = "502 TIMEOUT";
                    toDisconnect = true;
                    break;

            }

            try {
                w.print(message);
                w.print("\r\n");
                w.flush();
                if (toDisconnect) {
                    client.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void savePhoto(int[] data) {

        }
    }
}
