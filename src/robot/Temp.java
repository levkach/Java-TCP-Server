///*
//******
//Created on 10/10/2018
//by Lev Kolomazov (levkach@ya.ru)
//******
//*/
//package robot;
//
//import java.io.DataInputStream;
//import java.util.Random;
//
//public class Temp  {
//
//    private DataInputStream in = new DataInputStream(System.in);
//
//    private String readData() {
//        char readedChar;
//
//        StringBuilder stringBuilder = new StringBuilder();
//
//        try {
//
//            while (true) {
//
//                if (!timeoutTimer.isAbleToRun()) {
//                    System.err.println(Thread.currentThread().getId() + " timeout during readData");
//                    timeout();
//                    break;
//                }
//
//                readedChar = (char) in.read();
//
//
//                if (stringBuilder.toString().endsWith("\r") && readedChar == '\n') {
//                    stringBuilder.setLength(stringBuilder.length() - 1);
//                    break;
//                }
//
//                stringBuilder.append(readedChar);
//
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return stringBuilder.toString();
//    }
//
//
//}
