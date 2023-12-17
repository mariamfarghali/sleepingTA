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
    private JTextField barbersField, chairsField, studentsField;
    private JTextField sleepingField, workingField, waitingField, retryField;
    private JButton startButton;
    private Broom room;

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
        studentsField = new JTextField();
        add(studentsField);

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
        int noOfTAs = Integer.parseInt(barbersField.getText());
        int noOfChairs = Integer.parseInt(chairsField.getText());
        int noOfStudents = Integer.parseInt(studentsField.getText());

        room = new Broom(noOfTAs, noOfChairs);
        room.setGui(this); // Pass a reference to the GUI to the Broom instance

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ExecutorService exec = Executors.newFixedThreadPool(12);

                for (int i = 1; i <= noOfTAs; i++) {
                    TA barber = new TA(room, i);
                    Thread thTA = new Thread(barber);
                    exec.execute(thTA);
                }

                Random r = new Random();

                for (int i = 0; i < noOfStudents; i++) {
                    Student student = new Student(room);
                    student.setInTime(new Date());
                    Thread thStudent = new Thread(student);
                    student.setStudentId(i + 1);
                    exec.execute(thStudent);

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
            sleepingField.setText(String.valueOf(room.getTAsSleeping().get()));
            workingField.setText(String.valueOf(room.getTAsWorking().get()));
            waitingField.setText(String.valueOf(room.getStudentsWaiting().get()));
            retryField.setText(String.valueOf(room.getStudentsLeftAndWillRetry().get()));
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SleepingTAGUI());
    }
}

class TA implements Runnable {
    private Broom room;
    private int barberId;

    public TA(Broom room, int barberId) {
        this.room = room;
        this.barberId = barberId;
    }

    @Override
    public void run() {
        while (true) {
            room.cutHair(barberId);
        }
    }
}

class Student implements Runnable {
    private int studentId;
    private Date inTime;
    private Broom room;

    public Student(Broom room) {
        this.room = room;
    }

    public int getStudentId() {
        return studentId;
    }

    public Date getInTime() {
        return inTime;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public void setInTime(Date inTime) {
        this.inTime = inTime;
    }

    @Override
    public void run() {
        goForHairCut();
    }

    private synchronized void goForHairCut() {
        room.add(this);
    }
}

class Broom {
    private SleepingTAGUI gui;
    private AtomicInteger totalHairCuts = new AtomicInteger(0);
    private AtomicInteger studentsLost = new AtomicInteger(0);
    private AtomicInteger barbersWorking = new AtomicInteger(0);
    private AtomicInteger barbersSleeping = new AtomicInteger(0);
    private AtomicInteger studentsWaiting = new AtomicInteger(0);
    private AtomicInteger studentsLeftAndWillRetry = new AtomicInteger(0);

    int nchair, noOfTAs, availableTAs;
    List<Student> listStudent;

    Random r = new Random();

    public Broom(int noOfTAs, int noOfChairs) {
        this.nchair = noOfChairs;
        listStudent = new LinkedList<>();
        this.noOfTAs = noOfTAs;
        availableTAs = noOfTAs;
    }

    public void setGui(SleepingTAGUI gui) {
        this.gui = gui;
    }

    public AtomicInteger getTAsWorking() {
        return barbersWorking;
    }

    public AtomicInteger getTAsSleeping() {
        return barbersSleeping;
    }

    public AtomicInteger getStudentsWaiting() {
        return studentsWaiting;
    }

    public AtomicInteger getStudentsLeftAndWillRetry() {
        return studentsLeftAndWillRetry;
    }

   public void cutHair(int barberId) {
        Student student;
        synchronized (listStudent) {
            while (listStudent.isEmpty()) {
                System.out.println("TA " + barberId + " is waiting for the student and sleeps in his chair");
                gui.updateTextFieldValues();
                
                try {
                    barbersSleeping.incrementAndGet();
                    listStudent.wait();
                    barbersSleeping.decrementAndGet();
                } catch (InterruptedException iex) {
                    iex.printStackTrace();
                }
            }

            student = listStudent.remove(0);
            studentsWaiting.decrementAndGet();

            if (listStudent.isEmpty()) {
                System.out.println("Student " + student.getStudentId() +
                        " finds the barber asleep and wakes up the barber " + barberId);
                gui.updateTextFieldValues();
            }
        }

        int millisDelay = 0;
        try {
            barbersWorking.incrementAndGet();
            availableTAs--;
            gui.updateTextFieldValues();

            System.out.println("TA " + barberId + " cutting hair of " + student.getStudentId());

            millisDelay = r.nextInt(2000) + 4000;
            Thread.sleep(millisDelay);

            System.out.println("Completed Cutting hair of " + student.getStudentId() + " by barber " + barberId);

            totalHairCuts.incrementAndGet();

            availableTAs++;
            barbersWorking.decrementAndGet();

            gui.updateTextFieldValues();
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }

    public void add(Student student) {
        synchronized (listStudent) {
            while (listStudent.size() == nchair) {
                studentsLost.incrementAndGet();
                studentsLeftAndWillRetry.incrementAndGet();
                System.out.println("No chair available for student " + student.getStudentId() +
                        " so the student leaves the room");
                gui.updateTextFieldValues();
                
                try {
                    listStudent.wait(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                studentsLeftAndWillRetry.decrementAndGet();
            }

            studentsWaiting.incrementAndGet();

            if (availableTAs > 0) {
                listStudent.add(student);
                listStudent.notify();
            } else {
                listStudent.add(student);
                gui.updateTextFieldValues();
                if (listStudent.size() == 1)
                    listStudent.notify();
            }
        }
    }
}



