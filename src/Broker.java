import java.util.ArrayList;

// Used to execute the commands of the system on accounts
// Author James Brock - 953238
public class Broker {
    private static boolean commandSuccessful = true;
    //Shared static locks between running threads
    //Only need to lock the rate when updating it
    private static final Object RATE_WRITE_LOCK = new Object();
    private static final Object ACCOUNTS_WRITE_LOCK = new Object();
    private static final Object ACCOUNTS_READ_LOCK = new Object();
    private static ArrayList<Account> clonedList = new ArrayList<>();

    //Used to create a new account, synchronised means that the
    public static void open (int accNum){
        //Check if the account already exists
        boolean exists = (findAccount(accNum) != null);
        if(!exists){
            //If it doesn't and the account number greater than 0, open the account
            if(accNum >= 0){
                //Create a cloned copy of the list so that other threads can still read the accounts list
                synchronized (ACCOUNTS_READ_LOCK){
                    clonedList = Server.getAccounts();
                }
                //Synchronised block ensures race conditions don't occur by making sure no other threads query the
                //Shared list in the open account method whilst it is being updated
                synchronized (ACCOUNTS_WRITE_LOCK){
                    Server.setAccounts(insertIntoArray(new Account(accNum), clonedList));
                }
                commandSuccessful = true;
            } else {
                //Account number invalid
                commandSuccessful = false;
            }
        } else{
            //Already exists
            commandSuccessful = false;
        }
    }

    public static String state (){
        String out = "";

        //Lock the accounts for reading and then release the lock once the list has been cloned
        synchronized (ACCOUNTS_READ_LOCK){
            clonedList = Server.getAccounts();
        }

        //Lock the writing of the accounts on the local cloned list whilst reading the accounts
        //The data of the accounts in the toString method can be changed whilst being queried
        for(Account account: clonedList){
            //Ensure there is no null pointer exception
            if(account != null){
                //Goes through each object in the shared data, doesn't lock any individual account
                //Allows other operations on the account to take place in other threads
                out += account.toString() + "\n";
            }
        }


        //Rate is a static variable shared between the threads,
        //No need to lock the rate when the most updated one is needed
        out += "Rate " + Server.getRate();

        return out;
    }

    //Updates the rate, uses a synchronised method so no operations should take place using the rate
    //Until is has been updated
    public static void rate (double newRate){
        if(newRate > 0){
            synchronized (RATE_WRITE_LOCK){
                Server.setRate(newRate);
            }
            commandSuccessful = true;
        } else {
            commandSuccessful = false;
        }
    }

    public static void convert (int accountNum, Float givenA, Float givenP) {
        //Finds the target account via a search of the account number
        //No need to lock
        Account target = findAccount(accountNum);
        double acquiredRate;
        //If the account is found
        if (target != null) {
            acquiredRate = Server.getRate();
            //Call the convert method for the found account
            target.convert(givenA, givenP, acquiredRate);
            commandSuccessful = true;

        } else {
            //Account wasn't found, give an error message
            commandSuccessful = false;
        }
    }

    public static void transfer (int sourceNum, int destNum, Float amountA, Float amountP) throws InterruptedException{
        //Find the two involved accounts by performing two searches
        Account source = findAccount(sourceNum);
        Account dest = findAccount(destNum);
        //If both accounts exist
        if(source != null && dest != null){
            //Call the transfer method of the source account
            source.transfer(dest, amountA, amountP);
            commandSuccessful = true;
        } else {
            //One or both of the accounts didn't exist
            commandSuccessful = false;
        }
    }

    //Boolean Value used to determine whether to print an error message or a success message
    public static boolean getSuccessful(){
        return commandSuccessful;
    }

    public static Account findAccount(int an){
        //For each account in the array list of account
        //Create a clone list and synchronise block reading the shared data
        //Prevents race conditions when finding an account
        synchronized (ACCOUNTS_READ_LOCK){
            clonedList = Server.getAccounts();
        }

        //Don't need to lock the list as the account number can't be changed whilst being read
        for(Account account: clonedList){
            //Ensure no null pointer exception is called if no account is queried
            if(account != null){
                //Check if the current account matches the given account number an
                if(account.getAccountNumber() == an){
                    //If it does return the account
                    return account;
                }
            }
        }

        //Else return null if not found
        return null;
    }

    //Used to make the output of the state command deterministic i.e. insert a new element into the shared
    //Array at the proper location such that it is in numerical order based on the account number
    public static ArrayList<Account> insertIntoArray(Account addInto, ArrayList<Account> destination){
        int counter = 0;
        //Used so that the max size queried doesn't alter after the new element is added in
        final int MAX_SIZE = destination.size();
        if(MAX_SIZE == 0){
            //If the array is empty, add the element in
            destination.add(addInto);
        } else {
            //Else, while not at the end of the currently size array
            while(counter < MAX_SIZE){
                //Get the number of the current account at the counter index
                int number = destination.get(counter).getAccountNumber();
                if(addInto.getAccountNumber() < number){
                    //If the number of the account in the list is bigger, insert the account in its place
                    //And shift the rest of the accounts along
                    destination.add(counter, addInto);
                    //Exits the while loop
                    counter = MAX_SIZE;
                } else {
                    //If the account to insert has a larger account number
                    //Increment the counter to test the next number
                    counter++;
                    //If at the end of the array, put the new account at the end as it
                    //Has the largest account number
                    if(counter == MAX_SIZE){
                        destination.add(addInto);
                    }
                }
            }
        }

        return destination;
    }

    //THis method is outside of the assignment speck and was used for personal testing
    //Method to rest all of the data in the system to the default
    //The rate becomes 10 again and all accounts are set to null and the ArrayList re-instantiated
    public static void resetData(){
        //Lock the cloned list for reading
        synchronized (ACCOUNTS_READ_LOCK){
            clonedList = Server.getAccounts();
        }

        //Release the read lock, allowing others threads to read the data before it is reset
        //If a thread tries to write to the array list after it has been reset, an account not found
        //Caught error will greet them
        //Lock the account for reading
        synchronized (ACCOUNTS_WRITE_LOCK) {
            //Nullify all of the accounts in the shared data
            for (Account account: clonedList){
                account = null;
            }
            //Reset the array list
            Server.setAccounts(new ArrayList<>());
        }

        //Lock the rate for writing and reset it
        synchronized (RATE_WRITE_LOCK){
            Server.setRate(10);
        }
    }
}
