import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.*;

// Code to connect brokers to the program and execute threads connected to the socket
// Used to query the commands entered by the user in the terminal
// Author James Brock - 953238

public class Server {
    private static ArrayList<Account> accounts = new ArrayList<>();
    private static double rate = 10.0;

    /**
     * Runs the server. When a client connects, the server spawns a new thread to do
     * the servicing.
     */
    public static void main(String[] args) throws Exception {
        //multiThreadTest();
        try (ServerSocket listener = new ServerSocket(4242)) {
            ExecutorService pool = Executors.newFixedThreadPool(1000);
            while (true) {
                pool.execute(new Talk(listener.accept()));
            }
        }
    }

    public static ArrayList<Account> getAccounts(){
        return accounts;
    }

    public static void setAccounts(ArrayList<Account> updated){
        accounts = updated;
    }

    public static double getRate(){
        return rate;
    }

    public static void setRate(double rate) {
        Server.rate = rate;
    }

    public static Float[] convertToFloat(String in){
        //The given numbers surrounded by brackets and separated by commas are extracted
        //Try catch if there are no floats

        //Create a default array
        Float[] outArray = {0.0f, 0.0f};
        try {
            //Split the given string into just the data without the brackets and comma by splitting back on the given delimiters
            String[] values = in.trim().split("[(,)]");
            //First value is always whitespace that won't go away so it is being ignored
            //If the user entered anything after the brackets it is discarded and the command executes
            String num1 = values[1];
            String num2 = values[2];
            //Convert the strings to floats
            outArray[0] = Float.valueOf(num1.trim());
            outArray[1] = Float.valueOf(num2.trim());
            return outArray;
        } catch (NumberFormatException e){
            //The data provided wasn't correct
            System.out.println("Error converting a value to float, no changes have been made with respect to that value");
        }
        return outArray;
    }

    //Used to test multiple threads running at once outside of terminal use
    public static void multiThreadTest() throws InterruptedException {
        ThreadTester t1 = new ThreadTester("t1");
        t1.start();
        Thread.sleep(500);

        for(int i = 0; i < 10; i++){
            ThreadTester t2 = new ThreadTester("t2");
            ThreadTester t3 = new ThreadTester("t3");
            t2.start();
            t3.start();
        }

        Thread.sleep(2000);

        System.out.println(Broker.state());
    }

    private static class Talk implements Runnable {
        private Socket socket;

        Talk(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            System.out.println("Connected: " + socket);
            try {
                Scanner in = new Scanner(socket.getInputStream());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                while (in.hasNextLine()) {
                    //Create a new scanner of the current scanner input line
                    Scanner line = new Scanner(in.nextLine());
                    //Get the entered command
                    String command = line.next();
                    //Try catch blocks on the command and input types to prevent system crash with incorrect inputs
                    try {
                        try {
                            //Switch statement on the first string which is the command
                            switch (command){
                                case ("Open"):
                                    //Get the correct data and execute the open command with proper feedback
                                    int accNum = line.nextInt();
                                    Broker.open(accNum);
                                    if(Broker.getSuccessful()){
                                        out.println("Opened account " + accNum);
                                    } else {
                                        out.println("Account already exists or the number wasn't valid e.g. negative");
                                    }
                                    break;
                                case ("State"):
                                    //Execute the state command with proper feedback
                                    out.println(Broker.state());
                                    break;
                                case ("Rate"):
                                    //Get the correct data and execute the rate command with proper feedback
                                    Float newRate = line.nextFloat();
                                    Broker.rate(newRate);
                                    if(Broker.getSuccessful()){
                                        out.println("Rate changed");
                                    } else {
                                        out.println("The rate cannot be less than or equal to 0");
                                    }
                                    break;
                                case("Convert"):
                                    //Get the correct data and execute the convert command with proper feedback
                                    int convertNumAccount = line.nextInt();
                                    String rest = line.nextLine();

                                    //Properly scan the inputted data for the floats
                                    Float[] floats = convertToFloat(rest);
                                    Float convertA = floats[0];
                                    Float convertP = floats[1];
                                    Broker.convert(convertNumAccount, convertA, convertP);

                                    if(Broker.getSuccessful()) {
                                        out.println("Converted");
                                    } else {
                                        out.println("Account not found");
                                    }
                                    break;
                                case("Transfer"):
                                    //Get the correct data and execute the transfer command with proper feedback
                                    int from = line.nextInt();
                                    int to = line.nextInt();
                                    String restOfLine = line.nextLine();

                                    //Properly scan the inputted data for the floats
                                    Float[] transferFloats = convertToFloat(restOfLine);
                                    Float transferA = transferFloats[0];
                                    Float transferP = transferFloats[1];
                                    Broker.transfer(from, to, transferA, transferP);

                                    if(Broker.getSuccessful()) {
                                        out.println("Transferred");
                                    } else {
                                        out.println("One or both accounts not found");
                                    }
                                    break;
                                case("Reset"):
                                    Broker.resetData();
                                    break;
                                default:
                                    //Execute nothing and give feedback if the command isn't one of the above
                                    out.println("Command not recognised");
                                    break;
                            }
                        } catch (InputMismatchException ime){
                            //Incorrectly entered data for the command
                            out.println("Input mismatch when entering command, check inputs and try again");
                        }
                    } catch (NoSuchElementException nsee) {
                        //Command not spelt right or nothing entered
                        out.println("Command entered incorrectly, try again");
                    }
                    line.close();
                }
                in.close();
                //Scanners closed
            } catch (Exception e) {
                System.out.println("Error:" + socket);
            } finally {
                try { socket.close(); } catch (IOException e) {}
                System.out.println("Closed: " + socket);
            }
        }
    }
}