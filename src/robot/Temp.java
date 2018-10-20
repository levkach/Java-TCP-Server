/*
******
Created on 10/10/2018
by Lev Kolomazov (levkach@ya.ru)
******
*/
package robot;

import java.util.Random;

public class Temp implements Runnable {

    @Override
    public void run() {
        Random random = new Random();
        long start = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(random.nextInt(700));
                System.out.println("Some action that happened at " + (System.currentTimeMillis() - start) + " ms after start");
            } catch (Exception e) {
            }
        }

    }
}
