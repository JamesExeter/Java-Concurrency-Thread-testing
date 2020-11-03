
// Thread testing class that is used to test various actions of the program and see how well they run concurrently
// THis testing has helped to test my error handling, solutions to race conditions and deadlocking
// Author James Brock - 953238

public class ThreadTester implements Runnable {
    //Each thread testing instance has a name and is an instance of a thread
    private Thread t;
    private String threadName;

    //Set the name of the thread in the constructor
    public ThreadTester (String name){
        threadName = name;
    }

    public void run(){
        try {
            //This commented out code was used to run the example client session
            //But across three threads and see the result, usually commands from threads 2 and 3 are run
            //Before an account has been created by the first thread result in a caught error

            /*System.out.println("Running " + threadName);
            if(threadName.equals("t1")){
                Broker.open(1905);
                Broker.open(1935);
                Broker.open(1953);
                System.out.println(Broker.state());
            } else if(threadName.equals("t2")){
                Broker.transfer(1905, 1953, 10.0f, 20.0f);
                System.out.println(Broker.state());
                Broker.convert(1953, 1.0f, 0.0f);
                System.out.println(Broker.state());
            } else {
                System.out.println(Broker.state());
                Broker.rate(5.0f);
                Broker.convert(1935, 0.0f, 10.0f);
                System.out.println(Broker.state());
            }*/

            //Output the running thread name
            //This code is used to test deadlocks and race conditions
            System.out.println("Running " + threadName);
            if(threadName.equals("t1")){
                Broker.open(1);
                Broker.open(2);
                Broker.open(3);
            } else if(threadName.equals("t2")) {
                Broker.transfer(1, 2, 20.0f, 0.0f);
                Broker.transfer(3, 2, 1.0f, 0.0f);
                Broker.rate(5);
                Broker.convert(1,  1.0f, 0.0f);
                System.out.println(Broker.state());
            } else {
                Broker.transfer(2, 1, 0.0f, 10.0f);
                System.out.println(Broker.state());
                Broker.transfer(2, 3, 0.0f, 5.0f);
                Broker.rate(10);
                Broker.convert(2, 0.0f, 5.0f);
            }
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    //Starts the thread, if the thread is null, create it with the given name and start it
    public void start (){
        System.out.println("Starting " + threadName);
        if (t == null){
            t = new Thread(this);
            t.start();
            t.setName(threadName);
        }
    }
}
