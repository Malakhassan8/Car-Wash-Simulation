public class Car extends Thread
{
    int CarID;
    WaitingArea wa;

    public Car(int id, WaitingArea wa) {
        this.CarID = id;
        this.wa = wa;
    }

    public void run() {
        System.out.println("C" + CarID + " arrived");
        wa.addCar(this);}
}
