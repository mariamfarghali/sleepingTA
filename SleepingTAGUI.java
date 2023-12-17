/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package zeinagui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SleepingTAGUI extends JFrame {
    private JTextField barbersField, chairsField, customersField;
    private JTextField sleepingField, workingField, waitingField, retryField;
    private JButton startButton;
    private Bshop shop;

    public SleepingTAGUI() {
        setTitle("Sleeping TA Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(8, 2));

        add(new JLabel("Number of TAs (M):"));
        barbersField = new JTextField();
        add(barbersField);

        add(new JLabel("Number of Waiting Chairs (N):"));
        chairsField = new JTextField();
        add(chairsField);

        add(new JLabel("Number of Students:"));
        customersField = new JTextField();
        add(customersField);

        add(new JLabel("TAs Sleeping:"));
        sleepingField = new JTextField();
        sleepingField.setEditable(false);
        add(sleepingField);

        add(new JLabel("TAs Working:"));
        workingField = new JTextField();
        workingField.setEditable(false);
        add(workingField);

        add(new JLabel("Students Waiting:"));
        waitingField = new JTextField();
        waitingField.setEditable(false);
        add(waitingField);

        add(new JLabel("Students Left and Will Retry:"));
        retryField = new JTextField();
        retryField.setEditable(false);
        add(retryField);

        startButton = new JButton("Start Simulation");
        add(startButton);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSimulation();
            }
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void startSimulation() {
        int noOfBarbers = Integer.parseInt(barbersField.getText());
        int noOfChairs = Integer.parseInt(chairsField.getText());
        int noOfCustomers = Integer.parseInt(customersField.getText());

        shop = new Bshop(noOfBarbers, noOfChairs);
        shop.setGui(this); // Pass a reference to the GUI to the Bshop instance

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ExecutorService exec = Executors.newFixedThreadPool(12);

                for (int i = 1; i <= noOfBarbers; i++) {
                    Barber barber = new Barber(shop, i);
                    Thread thBarber = new Thread(barber);
                    exec.execute(thBarber);
                }

                Random r = new Random();

                for (int i = 0; i < noOfCustomers; i++) {
                    Customer customer = new Customer(shop);
                    customer.setInTime(new Date());
                    Thread thCustomer = new Thread(customer);
                    customer.setCustomerId(i + 1);
                    exec.execute(thCustomer);

                    try {
                        int millisDelay = r.nextInt(1000) + 500;
                        Thread.sleep(millisDelay);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }

                    publish(); // Trigger intermediate result to update GUI
                }

                exec.shutdown();
                try {
                    exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void process(List<Void> chunks) {
                updateTextFieldValues(); // Update the GUI in real-time
            }

            @Override
            protected void done() {
                updateTextFieldValues(); // Final update after simulation completion
                startButton.setEnabled(false);
            }
        };

        worker.execute();
    }

    protected void updateTextFieldValues() {
        SwingUtilities.invokeLater(() -> {
            sleepingField.setText(String.valueOf(shop.getBarbersSleeping().get()));
            workingField.setText(String.valueOf(shop.getBarbersWorking().get()));
            waitingField.setText(String.valueOf(shop.getCustomersWaiting().get()));
            retryField.setText(String.valueOf(shop.getCustomersLeftAndWillRetry().get()));
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SleepingTAGUI());
    }
}

class Barber implements Runnable {
    private Bshop shop;
    private int barberId;

    public Barber(Bshop shop, int barberId) {
        this.shop = shop;
        this.barberId = barberId;
    }

    @Override
    public void run() {
        while (true) {
            shop.cutHair(barberId);
        }
    }
}

class Customer implements Runnable {
    private int customerId;
    private Date inTime;
    private Bshop shop;

    public Customer(Bshop shop) {
        this.shop = shop;
    }

    public int getCustomerId() {
        return customerId;
    }

    public Date getInTime() {
        return inTime;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public void setInTime(Date inTime) {
        this.inTime = inTime;
    }

    @Override
    public void run() {
        goForHairCut();
    }

    private synchronized void goForHairCut() {
        shop.add(this);
    }
}

class Bshop {
    private SleepingTAGUI gui;
    private AtomicInteger totalHairCuts = new AtomicInteger(0);
    private AtomicInteger customersLost = new AtomicInteger(0);
    private AtomicInteger barbersWorking = new AtomicInteger(0);
    private AtomicInteger barbersSleeping = new AtomicInteger(0);
    private AtomicInteger customersWaiting = new AtomicInteger(0);
    private AtomicInteger customersLeftAndWillRetry = new AtomicInteger(0);

    int nchair, noOfBarbers, availableBarbers;
    List<Customer> listCustomer;

    Random r = new Random();

    public Bshop(int noOfBarbers, int noOfChairs) {
        this.nchair = noOfChairs;
        listCustomer = new LinkedList<>();
        this.noOfBarbers = noOfBarbers;
        availableBarbers = noOfBarbers;
    }

    public void setGui(SleepingTAGUI gui) {
        this.gui = gui;
    }

    public AtomicInteger getBarbersWorking() {
        return barbersWorking;
    }

    public AtomicInteger getBarbersSleeping() {
        return barbersSleeping;
    }

    public AtomicInteger getCustomersWaiting() {
        return customersWaiting;
    }

    public AtomicInteger getCustomersLeftAndWillRetry() {
        return customersLeftAndWillRetry;
    }

   public void cutHair(int barberId) {
        Customer customer;
        synchronized (listCustomer) {
            while (listCustomer.isEmpty()) {
                System.out.println("Barber " + barberId + " is waiting for the customer and sleeps in his chair");
                gui.updateTextFieldValues();
                
                try {
                    barbersSleeping.incrementAndGet();
                    listCustomer.wait();
                    barbersSleeping.decrementAndGet();
                } catch (InterruptedException iex) {
                    iex.printStackTrace();
                }
            }

            customer = listCustomer.remove(0);
            customersWaiting.decrementAndGet();

            if (listCustomer.isEmpty()) {
                System.out.println("Customer " + customer.getCustomerId() +
                        " finds the barber asleep and wakes up the barber " + barberId);
                gui.updateTextFieldValues();
            }
        }

        int millisDelay = 0;
        try {
            barbersWorking.incrementAndGet();
            availableBarbers--;
            gui.updateTextFieldValues();

            System.out.println("Barber " + barberId + " cutting hair of " + customer.getCustomerId());

            millisDelay = r.nextInt(2000) + 4000;
            Thread.sleep(millisDelay);

            System.out.println("Completed Cutting hair of " + customer.getCustomerId() + " by barber " + barberId);

            totalHairCuts.incrementAndGet();

            availableBarbers++;
            barbersWorking.decrementAndGet();

            gui.updateTextFieldValues();
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }

    public void add(Customer customer) {
        synchronized (listCustomer) {
            while (listCustomer.size() == nchair) {
                customersLost.incrementAndGet();
                customersLeftAndWillRetry.incrementAndGet();
                System.out.println("No chair available for customer " + customer.getCustomerId() +
                        " so the customer leaves the shop");
                gui.updateTextFieldValues();
                
                try {
                    listCustomer.wait(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                customersLeftAndWillRetry.decrementAndGet();
            }

            customersWaiting.incrementAndGet();

            if (availableBarbers > 0) {
                listCustomer.add(customer);
                listCustomer.notify();
            } else {
                listCustomer.add(customer);
                gui.updateTextFieldValues();
                if (listCustomer.size() == 1)
                    listCustomer.notify();
            }
        }
    }
}

