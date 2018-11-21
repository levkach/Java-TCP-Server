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
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;


public class Robot {

    private ServerSocket server;
    private final Logger LOGGER = Logger.getLogger(Robot.class.getName());


    /**
     * @param args: [0] - number of port
     *            Starts the server.
     */
    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);
            if (port >= 3000 && port <= 3999) {
                Robot s = new Robot(port);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param portnum nuffsaid
     * Creates server and listens for new clients. By default 15 clients.
     */
    public Robot(int portnum) {
        final int NUMBER_OF_CLIENTS = 15;
        try {
            server = new ServerSocket(portnum);
        } catch (Exception err) {
            LOGGER.severe("Could not create server on port " + portnum);
            LOGGER.info(err.getLocalizedMessage());
            return;
        }

        // Create a pool for 15 clients
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(NUMBER_OF_CLIENTS);
        while (true) {
            try {
                final Socket socket = server.accept();
                final Future task = scheduler.submit(new TCPServer(socket)); // Client

                scheduler.schedule(new Runnable() { // Thread to interrupt Client and disconnect if timeout reached
                    @Override
                    public void run() {
                        task.cancel(true);
                        try {
                            // Send timeout message directly to client
                            OutputStream clientOutput = socket.getOutputStream();
                            clientOutput.write("502 TIMEOUT\r\n".getBytes());
                            clientOutput.flush();
                            clientOutput.close();
                        } catch (Exception exc) {
                            if (!socket.isClosed()) {
                                LOGGER.severe("Could not close connection after timeout");
                            }
                        }
                    }
                }, 45100, TimeUnit.MILLISECONDS); // 45.1 seconds is more sustainable
            } catch (Exception err) {
                LOGGER.severe("Error occured");
                LOGGER.info(err.getLocalizedMessage());
            }
        }

    }

}

/**
 *
 */
class TCPServer implements Runnable {

    private Socket client;
    private BufferedInputStream inp;
    private PrintWriter w;
    private StringBuilder convo;
    private final Logger LOGGER = Logger.getLogger(Robot.class.getName());
    private static int PHOTO_COUNTER = 0;
    private static String CLIENT_ADDR = "";

    /**
     * @param client socket
     *               Initializes variables, input stream and output writer
     */
    public TCPServer(Socket client) {
        this.client = client;
        CLIENT_ADDR = client.getInetAddress().toString();
        //Initialize Variables
        try {
            w = new PrintWriter(client.getOutputStream());
            inp = new BufferedInputStream(client.getInputStream());
            convo = new StringBuilder();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * main loop.
     * authorises and reads commands while can.
     */
    public void run() {
        try {
            authorization();
            // Main communication with client
            while (!client.isClosed()) {
                readCommand();
            }
        } catch (Exception e) {
            LOGGER.severe("Error occured");
        }
    }

    /**
     * Prompts user to enter credentials and compares username checksum with password
     */
    private void authorization() {

        LOGGER.info("Client " + CLIENT_ADDR + " connected");

        log("200");
        int username = readUsername();

        log("201");
        int pass = readPassword();

        if (username == -1 || pass == -1) {
            log("500");
        } else {
            if (username == pass) {
                log("202");
                LOGGER.info("Client " + CLIENT_ADDR + " authorized");
            } else {
                LOGGER.severe(" Unathorized client " + CLIENT_ADDR + " disconnected");
                log("500");
            }
        }

    }


    /**
     * @return checksum of user's input or -1, if bad syntax
     */
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
            LOGGER.severe("Username read error");
        }
        sum -= (int) '\r' + (int) '\n';
        return incorrect ? -1 : sum;
    }


    /**
     * @return password casted to int or -1 if bad syntax
     */
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
            LOGGER.severe("Could not parse password");
            incorrect = true;
        } catch (Exception stE) {
            LOGGER.severe("Password read error");
        }
        return incorrect ? -1 : sum;
    }


    /**
     * prompts user to enter a command, controlling input by char
     * handles info command
     */
    private void readCommand() {
        try {
            StringBuilder command = new StringBuilder();
            StringBuilder message = new StringBuilder();

            char before = ' ';
            char last = ' ';
            boolean matched = false;
            int temp;


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

                if (command.toString().equals("INFO ")) { // assembling the message
                    matched = true;
                    message.append(last);
                } else if (command.toString().equals("FOTO ")) { // handle FOTO
                    matched = true;
                    readPhotoCommand();
                    break;
                }
            }

            if (matched && command.toString().equals("INFO ")) { // handle INFO
                log("202");
                String toSave = "FROM CLIENT " + CLIENT_ADDR + "GOT MESSAGE " + message.toString() + "\n";
                convo.append(toSave);
            }
        } catch (SocketException closed) {
            LOGGER.severe("Socket was closed due to bad syntax");
        } catch (IOException e) {
            LOGGER.severe("IO exception");
        }
    }


    /**
     * reads photo
     */
    private void readPhotoCommand() {
        int size = 0;
        long counted = 0;
        long checksum = 0;
        int temp;
        StringBuilder message = new StringBuilder();

        try {
            //read number of bytes
            while (true) {
                char read = (char) inp.read();
                if (read == ' ') {
                    break;
                } else {
                    message.append(read);
                }
                if (!message.toString().matches("\\d+")) { //check if size is number
                    throw new NumberFormatException(); // bad number
                }
            }
            size = Integer.parseInt(message.toString().trim());

            //file handle routine
            String fileName = String.format("foto" + "%03d" + ".png", PHOTO_COUNTER++); // get photo name
            try {
                Files.delete(Paths.get(fileName)); // delete file if exists.
            } catch (Exception fnf) {
                LOGGER.info("");
            }
            OutputStream file = null;
            try {
                file = new FileOutputStream(fileName, true); // create file
            } catch (Exception fnc) {
                LOGGER.warning("Photo could not be created");
            }

            //read photo by bytes and write to file
            for (int i = 0; i < size; i++) {
                temp = inp.read();
                counted += temp; // counting cheksum
                if (file != null) {
                    file.write(temp);
                }
            }

            //read checksum
            for (int i = 0; i < 4; i++) {
                temp = inp.read();
                checksum += temp * Math.pow(16, 2 * (3 - i)); // converting from hex to dec
            }
            LOGGER.info(String.valueOf("size: " + size + "; expected: " + checksum + "; counted: " + counted));

            //check if checksum matches counted checksum
            if (counted == checksum) {
                if (file != null) {
                    file.flush();
                }
                log("202");
            } else {
                try {
                    if (file != null) {
                        file.flush();
                        file.close();
                    }
                    Files.delete(Paths.get(fileName));
                } catch (Exception exc) {
                    LOGGER.warning("Error deleting photo");
                }
                log("300");
                LOGGER.info("Bad checksum");
            }

        } catch (FileNotFoundException fnf) {
            LOGGER.warning("File could not be created");
        } catch (NumberFormatException badNumber) { // handling bad syntax
            log("501");
            LOGGER.severe("Syntax error while reading photo.");
        } catch (Exception exc) {
            LOGGER.severe("Error happened");
            LOGGER.info(exc.getLocalizedMessage());
        }
    }


    /**
     * @param s message code
     *          prints the log to the output
     */
    private void log(String s) {
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

}