import java.io.*;
import java.util.*;
import java.lang.*;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.Queue;


// ----- Semaphore Class -----
class Semaphore {
    protected int value = 0;

    protected Semaphore() {
        value = 0;
    }

    protected Semaphore(int initial) {
        value = initial;
    }

    public synchronized void P(String name) {
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

    public synchronized void P() {
        value--;
        if (value < 0) {
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



// ----- WaitingArea Class -----
class WaitingArea {
    int bufferSize = 5;                 // Capacity of waiting area
    Queue<Car> waitingCars = new LinkedList<>();

    Semaphore spaces = new Semaphore(bufferSize);
    Semaphore mutex = new Semaphore(1);
    Semaphore cars = new Semaphore(0);

    public void addCar(Car car) {
        spaces.P("C" + car.CarID);       // Wait for space
        mutex.P();                        // Enter critical section
        waitingCars.add(car);             // Add car to queue
        mutex.V();                        // Exit critical section
        cars.V();                         // Signal car is waiting
    }
}


// ----- Car Class -----
class Car extends Thread {
    int CarID;
    WaitingArea wa;

    public Car(int id, WaitingArea wa) {
        this.CarID = id;
        this.wa = wa;
    }

    public void run() {
        System.out.println("C" + CarID + " arrived");
        wa.addCar(this);
    }
}


// ----- Pump (Consumer) Class -----
class Pump extends Thread {
    WaitingArea wa;
    Semaphore bays;
    int pumpID;

    public Pump(int id, WaitingArea wa, Semaphore bays) {
        this.pumpID = id;
        this.wa = wa;
        this.bays = bays;
    }

    public void run() {
        while (true) {
            try {
                Car car = null;
                wa.mutex.P();          // Enter critical section
                try {
                    car = wa.waitingCars.poll(); // Remove car from queue
                } finally {
                    wa.mutex.V();       // Always unlock
                }

                if (car != null) {      // Only proceed if a car exists
                    System.out.println("Pump " + pumpID + ": C" + car.CarID + " login");
                    System.out.println("Pump " + pumpID + ": C" + car.CarID + " begins service at Bay " + pumpID);

                    wa.spaces.V();      // Free a waiting space

                    Thread.sleep(2000); // Simulate service time

                    System.out.println("Pump " + pumpID + ": C" + car.CarID + " finishes service");
                    System.out.println("Pump " + pumpID + ": Bay " + pumpID + " is now free");

                    bays.V();           // Release the bay
                }
                // Release the bay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;              // Exit loop on interruption
            }
        }
    }
}




// ----- Main Simulation Class -----//

    public class ServiceStation{
        public static void main(String[] args) {
            Scanner input = new Scanner(System.in);

            // Take user input
            System.out.print("Enter waiting area capacity: ");
            int waitingCapacity = input.nextInt();

            System.out.print("Enter number of service bays (pumps): ");
            int numBays = input.nextInt();

            System.out.print("Enter number of arriving cars: ");
            int numCars = input.nextInt();


            // Initialize waiting area
            WaitingArea wa = new WaitingArea();
            wa.bufferSize = waitingCapacity;
            wa.spaces = new Semaphore(wa.bufferSize);
            wa.mutex = new Semaphore(1);
            wa.cars = new Semaphore(0);

            // Initialize service bays
            Semaphore bays = new Semaphore(numBays); // 3 service bays

            // Create and start Pump threads
            Pump[] pumps = new Pump[numBays];
            for (int i = 0; i < numBays; i++) {
                pumps[i] = new Pump(i + 1, wa, bays);
                pumps[i].start();
            }


            // Create car threads in order C1 - C5
            Car[] cars = new Car[numCars];
            for (int i = 0; i < numCars; i++) {
                cars[i] = new Car(i + 1, wa);
            }

            // Start car threads with small delay to simulate arrival order
            try {
                for (Car car : cars) {
                    car.start();
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Wait for all cars to finish adding to the waiting area
            try {
                for (Car car : cars) {
                    car.join();
                }

                // Wait for pumps to finish processing cars
                Thread.sleep(15000); // adjust based on service time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

// Stop pumps gracefully
            for (Pump pump : pumps) {
                pump.interrupt();
            }

            System.out.println("All cars processed; simulation ends");
            input.close();
        }
    }


