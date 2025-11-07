import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

// ----- Semaphore Class -----
class Semaphore {
    protected int value;

    public Semaphore(int initial) {
        value = initial;
    }

    public synchronized void P(String name) {
        value--;
        if (value < 0) {
            ServiceStationGUI.logMessage(name + " waiting for space...");
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
        if (value <= 0) {
            notifyAll();
        }
    }
}

// ----- WaitingArea Class -----
class WaitingArea {
    int bufferSize;
    Queue<Car> waitingCars = new LinkedList<>();

    Semaphore spaces;
    Semaphore mutex;
    Semaphore cars;

    public WaitingArea(int bufferSize) {
        this.bufferSize = bufferSize;
        spaces = new Semaphore(bufferSize);
        mutex = new Semaphore(1);
        cars = new Semaphore(0);
    }

    public void addCar(Car car) {
        // Try to enter waiting area
        spaces.P("🚗 C" + car.CarID);

        // Critical section: add to queue
        mutex.P();
        waitingCars.add(car);

        // 🟢 Log event when car enters waiting area
        ServiceStationGUI.logMessage("🚗 C" + car.CarID + " entered waiting area (" +
                waitingCars.size() + "/" + bufferSize + ")");

        // Update GUI display
        ServiceStationGUI.updateWaitingArea(waitingCars.size());

        mutex.V();
        cars.V();
    }

    public Car getNextCar() {
        mutex.P();
        Car car = waitingCars.poll();
        if (car != null) {
            ServiceStationGUI.logMessage("🚘 C" + car.CarID + " leaves waiting area for service");
        }
        ServiceStationGUI.updateWaitingArea(waitingCars.size());
        mutex.V();
        return car;
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
        ServiceStationGUI.logMessage("🚗 C" + CarID + " arrived");
        wa.addCar(this);
    }
}

// ----- Pump (Consumer) Class -----
class Pump extends Thread {
    WaitingArea wa;     //the shared waiting queue
    Semaphore bays;     //available service bays
    int pumpID;
    JLabel pumpLabel;   //GUI label used to show pump status

    //Constructor
    public Pump(int id, WaitingArea wa, Semaphore bays, JLabel pumpLabel) {
        this.pumpID = id;
        this.wa = wa;
        this.bays = bays;
        this.pumpLabel = pumpLabel;
    }

    public void run() {
        //loops until the thread is interrupted
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Car car = wa.getNextCar();
                if (car != null) {
                    setPumpBusy("C" + car.CarID);
                    ServiceStationGUI.logMessage("Pump " + pumpID + ": C" + car.CarID + " begins service");
                    wa.spaces.V();   //signal that one waiting space freed up when the car moved to a pump.

                    Thread.sleep(2000); // simulate service time

                    ServiceStationGUI.logMessage("Pump " + pumpID + ": C" + car.CarID + " finishes service");
                    setPumpFree();
                    bays.V();
                } else {
                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void setPumpBusy(String carName) {
        SwingUtilities.invokeLater(() -> {
            pumpLabel.setText("Pump " + pumpID + ": " + carName);
            pumpLabel.setBackground(Color.YELLOW);
        });
    }

    public void setPumpFree() {
        SwingUtilities.invokeLater(() -> {
            pumpLabel.setText("Pump " + pumpID + ": Free");
            pumpLabel.setBackground(Color.GREEN);
        });
    }
}

// ----- GUI + Main -----
public class ServiceStationGUI extends JFrame {
    static JTextArea logArea;
    static JPanel waitingPanel;
    static JLabel[] pumpLabels;

    public ServiceStationGUI(int waitingCap, int numBays, int numCars) {
        setTitle("⛽ Car Wash Simulation");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Waiting area panel
        waitingPanel = new JPanel();
        waitingPanel.setBorder(BorderFactory.createTitledBorder("🚗 Waiting Area"));
        waitingPanel.setLayout(new FlowLayout());
        add(waitingPanel, BorderLayout.NORTH);

        // Pumps panel
        JPanel pumpPanel = new JPanel();
        pumpPanel.setBorder(BorderFactory.createTitledBorder("⛽ Pumps"));
        pumpPanel.setLayout(new GridLayout(1, numBays, 10, 10));
        pumpLabels = new JLabel[numBays];
        for (int i = 0; i < numBays; i++) {
            JLabel lbl = new JLabel("Pump " + (i + 1) + ": Free", SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setBackground(Color.GREEN);
            lbl.setPreferredSize(new Dimension(150, 40));
            lbl.setFont(new Font("Arial", Font.BOLD, 14));
            pumpPanel.add(lbl);
            pumpLabels[i] = lbl;
        }
        add(pumpPanel, BorderLayout.CENTER);

        // Log area
        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("📜 Events Log"));
        add(scroll, BorderLayout.SOUTH);

        setVisible(true);

        startSimulation(waitingCap, numBays, numCars);
    }

    public void startSimulation(int waitingCap, int numBays, int numCars) {
        WaitingArea wa = new WaitingArea(waitingCap);
        Semaphore bays = new Semaphore(numBays);

        // Start pumps
        Pump[] pumps = new Pump[numBays];
        for (int i = 0; i < numBays; i++) {
            pumps[i] = new Pump(i + 1, wa, bays, pumpLabels[i]);
            pumps[i].start();
        }

        // Start cars (arrive gradually)
        new Thread(() -> {
            try {
                for (int i = 0; i < numCars; i++) {
                    Car car = new Car(i + 1, wa);
                    car.start();
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

    }

    // --- GUI Update Methods ---
    public static void logMessage(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + time + "] " + msg + "\n");
        });
    }

    public static void updateWaitingArea(int carCount) {
        SwingUtilities.invokeLater(() -> {
            waitingPanel.removeAll();
            for (int i = 0; i < carCount; i++) {
                JLabel carLabel = new JLabel("🚗");
                carLabel.setFont(new Font("Arial", Font.BOLD, 20));
                waitingPanel.add(carLabel);
            }
            waitingPanel.revalidate();
            waitingPanel.repaint();
        });
    }

    // ----- MAIN -----
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            int waitingCap = Integer.parseInt(JOptionPane.showInputDialog("Enter waiting area capacity:"));
            int numBays = Integer.parseInt(JOptionPane.showInputDialog("Enter number of service bays (pumps):"));
            int numCars = Integer.parseInt(JOptionPane.showInputDialog("Enter number of arriving cars:"));
            new ServiceStationGUI(waitingCap, numBays, numCars);
        });
    }
}
