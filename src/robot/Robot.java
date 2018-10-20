/*
******
Created on 21/09/2018
by Lev Kolomazov (levkach@ya.ru)
******
*/
package robot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Robot {
    public static void main(String[] args) {
//        String s = "FOTO 8 ABCDEFGH\\x00\\x00\\x02\\x24";
//        System.out.println(s);
//
//        String[] arguments = s.split(" ");
//
//        int size = Integer.parseInt(arguments[1]);
//        System.out.println("Size: " + size);
//
//        byte[] command = arguments[2].getBytes();
//
//        System.out.println("data: ");
//        byte[] data = Arrays.copyOfRange(command,0, command.length - 4);
//        for (byte b:
//                data) {
//            System.out.print(b + " ");
//        }
//
//        System.out.println("\r\nchecksum:");
//        byte[] checksum = Arrays.copyOfRange(command, command.length - 4, command.length);
//        for (byte b:
//                checksum) {
//            System.out.print(b + " ");
//        }

//        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
//        try {
//            String line = "";
//            char symbol = ' ';
//            while (!line.contains("\r\n")){
//                symbol = (char) r.read();
//                line += symbol;
//                System.out.println("line:" + line);
//                System.out.println(Pattern.compile("(INFO \\S+)|(FOTO [1-9][0-9]* .+)").matcher(line).find());
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println((byte)'R');

        long period = 10000L;
        long start = System.currentTimeMillis();
        Random random = new Random();
        while (System.currentTimeMillis() - start < period) {
            new Thread(new Temp()).start();
        }

    }
}
