import java.io.*;
import java.util.*;
import java.lang.*;

public class Semaphore {
    protected int value=0;

    protected Semaphore(){
        value=0;
    }

    protected Semaphore(int initial) {
        value = initial;
    }
    public synchronized void P() {
        value--;
        if (value < 0) {
         System.out.println(name + " arrived and waiting");
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public synchronized void V() {
        value++;
        if (value <= 0)
            notify();
    }
}
