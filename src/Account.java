import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Account class that stores information for each account with getter and setter methods as well as convert and transfer methods
// Author James Brock - 953238
public class Account {
    private int accountNumber;
    private Float arians;
    private Float pres;
    private static final Float INIT_AMOUNT = 0.0f;

    //Re-entrant lock for deadlocking and random number (time) for timeouts
    //True as the parameter means that the longest waiting thread acquires the lock thread
    private final Lock LOCK = new ReentrantLock(true);
    //Constructor for the account, calls the setArians and setPres methods with the initial amount of zero
    public Account (int accNum){
        setAccountNumber(accNum);
        setArians(INIT_AMOUNT);
        setPres(INIT_AMOUNT);
    }

    //Getters and setters and a toString method
    public void setArians(Float arians) {
        this.arians = arians;
    }

    public Float getArians() {
        return arians;
    }

    public void setPres(Float pres) {
        this.pres = pres;
    }

    public Float getPres() {
        return pres;
    }

    public int getAccountNumber(){
        return accountNumber;
    }

    public void setAccountNumber (int accountNumber){
        this.accountNumber = accountNumber;
    }

    public String toString() {
        return getAccountNumber() + ": Arian " + getArians() + ", Pres " + getPres();
    }

    //Backend implementation of convert done in the account class
    //Allows just the object to be locked when altering the account data
    //The object is locked such that race conditions can be prevented
    public void convert (Float givenA, Float givenP, double rate){
        //Synchronising on the object itself
        synchronized (this){
            //Updates the arians and pres according to the rate and the given conversion algorithm
            setArians((float) (getArians() - givenA + (givenP/rate)));
            setPres((float) (getPres() - givenP + (givenA*rate)));
        }
    }

    //Backend implementation of transfer done in the account class
    //Allows just the object source and target to be locked during the transfer
    public void transfer (Account dest, Float amountA, Float amountP) throws InterruptedException{
        //Set the account source as the object calling the method
        Account source = this;
        boolean transferComplete = false;
        //Keep trying to acquire the locks after a timeout
        //If only one of the locks can by acquired, then all locks are released to prevent deadlocking
        while(!transferComplete){
            if (source.LOCK.tryLock()){
                //First lock acquired
                //If any errors occur within the execution of the method, release the locks and make no changes
                try {
                    if(dest.LOCK.tryLock()){
                        //Both locks acquired
                        try {
                            //Execute the transaction
                            //Ensures no inconsistent transactions in the transfer
                            source.setArians(source.getArians() - amountA);
                            source.setPres(source.getPres() - amountP);
                            dest.setArians(dest.getArians() + amountA);
                            dest.setPres(dest.getPres() + amountP);
                            transferComplete = true;
                        } finally {
                            //Unlock the second account
                            dest.LOCK.unlock();
                        }
                    }
                } finally {
                    //Unlock the first account
                    source.LOCK.unlock();
                }
            }
            //If the locks couldn't be acquired or an error occurred,
            //Timeout the thread and try again
            //Only timeout the thread if it couldn't acquire the locks
            if(!transferComplete){
                System.out.println("Thread waiting to reacquire lock");
                //Threads times out for a random time between half a second to a second
                Random randNum = new Random();
                int n = randNum.nextInt(500);
                Thread.sleep(500 + n);
            }
        }
    }
}
