import java.util.Queue;
import java.util.LinkedList;

public class WaitingArea {

int bufferSize = 5;    //Size of the waiting area buffer

Queue<Car> waitingCars = new LinkedList<>();   //Queue to hold waiting cars

Semaphore spaces = new Semaphore(bufferSize);
    
Semaphore mutex = new Semaphore(1);    //Binary semaphore for mutual exclusion

Semaphore cars = new Semaphore(0);     //Counts number of cars waiting

public void addCar(Car car)
{
spaces.P("C" + car.CarID );   //Wait for an empty space
mutex.P();                   //Enter critical section (lock)
waitingCars.add(car);       //Add car to waiting area
mutex.V();                  //Exit critical section (unlock)
cars.V();                  //Signal that a car is waiting

}
}
