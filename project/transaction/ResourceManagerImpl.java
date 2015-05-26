/***
*
* CS223 Resource Manager
* Authors: Joel Fuentes - Ling Ji
*
****/

package transaction;

import transaction.tables.*;
import lockmgr.*;
import java.rmi.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.File;
import java.io.IOException;

/**
* Resource Manager for the Distributed Travel Reservation System.
*
* Description: toy implementation of the RM
*/

public class ResourceManagerImpl
extends java.rmi.server.UnicastRemoteObject
implements ResourceManager {

   protected String myRMIName = null; // Used to distinguish this RM from other RMs
   protected TransactionManager tm = null;










   protected int xidCounter;
   private String rmiName;

   //get instances of tables
   private Cars cars;
   private Hotels hotels;
   private Flights flights;
   private Reservations reservations;

   //copy for active database
   private Cars actCars;
   private Hotels actHotels;
   private Flights actFlights;
   private Reservations actReservations;

   //lock Manager
   private static LockManager lm;

   //transactions
   private HashMap<Integer, ArrayList<OperationPair>> activeTransactions;
   private Set<Integer> abortedTransactions;

   //first part of Data id
   /*   public static final String RMINameFlights = "RMFlights";
   public static final String RMINameCars = "RMCars";
   public static final String RMINameRooms = "RMHotels";
   public static final String RMINameCustomers = "RMReservations";
   public static final String RMINameCustomers = "RMCustomers"; */

   //paths for db on disk
   public static final String DIR_DB = "data";
   public static final String RMINameFlights_DB = DIR_DB+"/"+RMINameFlights+".db";
   public static final String RMINameFlights_ACTIVE_DB = DIR_DB+"/"+RMINameFlights+"_active.db";
   public static final String RMINameCars_DB = DIR_DB+"/"+RMINameCars+".db";
   public static final String RMINameCars_ACTIVE_DB = DIR_DB+"/"+RMINameCars+"_active.db";
   public static final String RMINameRooms_DB = DIR_DB+"/"+RMINameRooms+".db";
   public static final String RMINameRooms_ACTIVE_DB = DIR_DB+"/"+RMINameRooms+"_active.db";
   public static final String RMINameCustomers_DB = DIR_DB+"/"+RMINameCustomers+".db";
   public static final String RMINameCustomers_ACTIVE_DB = DIR_DB+"/"+RMINameCustomers+"_active.db";
   public static final String Flights_TRANSACTIONS_DB = DIR_DB+"/"+RMINameFlights+"transactions.db";
   public static final String Cars_TRANSACTIONS_DB = DIR_DB+"/"+RMINameCars+"transactions.db";
   public static final String Rooms_TRANSACTIONS_DB = DIR_DB+"/"+RMINameRooms+"transactions.db";
   public static final String Customers_TRANSACTIONS_DB = DIR_DB+"/"+RMINameCustomers+"transactions.db";

   //shutdown flags
   private boolean shutdown = false;
   private boolean dieBeforeSwitch=false;
   private boolean dieAfterSwitch=false;



   public static void main(String args[]) {
      System.setSecurityManager(new RMISecurityManager());

      String rmiName = System.getProperty("rmiName");
      if (rmiName == null || rmiName.equals("")) {
         System.err.println("No RMI name given");
         System.exit(1);
      }

      String rmiPort = System.getProperty("rmiPort");
      if (rmiPort == null) {
         rmiPort = "";
      } else if (!rmiPort.equals("")) {
         rmiPort = "//:" + rmiPort + "/";
      }

      try {
         ResourceManagerImpl obj = new ResourceManagerImpl(rmiName);
         Naming.rebind(rmiPort + rmiName, obj);
         System.out.println(rmiName + " bound");
      }
      catch (Exception e) {
         System.err.println(rmiName + " not bound:" + e);
         System.exit(1);
      }
   }

   public ResourceManagerImpl(String rmiName) throws RemoteException {
      myRMIName = rmiName;

      while (!reconnect()) {
         // would be better to sleep a while
         try {
            Thread.sleep(1000);
         } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
         }
      }

      checkDirectoryDB();
      if(!recover()){  //recover db
         System.out.println("There is an error recovering the DB");
         System.exit(1);
      }
      activeTransactions = new HashMap<Integer, ArrayList<OperationPair>>();
      abortedTransactions = new HashSet<Integer>();


      lm = new LockManager();

      xidCounter = 0;

      System.out.println("ResourceManager started");
   }

   public boolean reconnect()
   throws RemoteException {
      String rmiPort = System.getProperty("rmiPort");
      if (rmiPort == null) {
         rmiPort = "";
      } else if (!rmiPort.equals("")) {
         rmiPort = "//:" + rmiPort + "/";
      }

      try {
         tm = (TransactionManager)Naming.lookup(rmiPort + TransactionManager.RMIName);
         System.out.println(myRMIName + " bound to TM");
      }
      catch (Exception e) {
         System.err.println(myRMIName + " cannot bind to TM:" + e);
         return false;
      }

      return true;
   }


   /*
   public ResourceManagerImpl(String rmiName) throws RemoteException {
   this.rmiName=rmiName;

   checkDirectoryDB();
   recover();  //recover db

   activeTransactions = new HashMap<Integer, ArrayList<OperationPair>>();
   abortedTransactions = new HashSet<Integer>();


   lm = new LockManager();

   xidCounter = 0;

   System.out.println("ResourceManager started");
} */



private void checkDirectoryDB(){
   File theDir = new File(DIR_DB);

   // if the directory does not exist, create it
   if (!theDir.exists()) {
      try{
         theDir.mkdir();
      }
      catch(SecurityException se){
         //handle it
      }

   }
}

private boolean recover(){

   //flights
   if(myRMIName.equals(RMINameFlights)){
      File file = new File(RMINameFlights_DB);

      try{
         //check if pointer file exists
         if(!file.exists()){
            flights = new Flights();
         }else{
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(RMINameFlights_DB));
            flights = (Flights) in.readObject();
            in.close();

            //check for active table (pendient)
         }
      } catch(IOException ioe) {
         ioe.printStackTrace();
         return false;
      } catch(ClassNotFoundException ioe) {
         ioe.printStackTrace();
         return false;
      }

      //Active DB

      //flights
      file = new File(RMINameFlights_ACTIVE_DB);

      try{
         //check if pointer file exists
         if(!file.exists()){
            actFlights = new Flights();
         }else{
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(RMINameFlights_ACTIVE_DB));
            actFlights = (Flights) in.readObject();
            in.close();

            //check for active table (pendient)
         }
      } catch(IOException ioe) {
         ioe.printStackTrace();
         return false;
      } catch(ClassNotFoundException ioe) {
         ioe.printStackTrace();
         return false;
      }
   }else if(myRMIName.equals(RMINameCars)){


      //cars
      File file = new File(RMINameCars_DB);

      try{
         //check if pointer file exists
         if(!file.exists()){
            cars = new Cars();
         }else{
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(RMINameCars_DB));
            Object obj = in.readObject();
            in.close();

            cars  = (Cars) obj;

            //check for active table (pendient)
         }
      } catch(IOException ioe) {
         ioe.printStackTrace();
         return false;
      } catch(ClassNotFoundException ioe) {
         ioe.printStackTrace();
         return false;
      }


      //cars
      file = new File(RMINameCars_ACTIVE_DB);

      try{
         //check if pointer file exists
         if(!file.exists()){
            actCars = new Cars();
         }else{
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(RMINameCars_ACTIVE_DB));
            Object obj = in.readObject();
            in.close();

            actCars  = (Cars) obj;

            //check for active table (pendient)
         }
      } catch(IOException ioe) {
         ioe.printStackTrace();
         return false;
      } catch(ClassNotFoundException ioe) {
         ioe.printStackTrace();
         return false;
      }

   }else if(myRMIName.equals(RMINameRooms)){

      //hotels
      File file = new File(RMINameRooms_DB);

      try{
         //check if pointer file exists
         if(!file.exists()){
            hotels = new Hotels();
         }else{
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(RMINameRooms_DB));
            Object obj = in.readObject();
            in.close();

            hotels  = (Hotels) obj;

            //check for active table (pendient)
         }
      } catch(IOException ioe) {
         ioe.printStackTrace();
         return false;
      } catch(ClassNotFoundException ioe) {
         ioe.printStackTrace();
         return false;
      }


      //hotels
      file = new File(RMINameRooms_ACTIVE_DB);

      try{
         //check if pointer file exists
         if(!file.exists()){
            actHotels = new Hotels();
         }else{
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(RMINameRooms_ACTIVE_DB));
            Object obj = in.readObject();
            in.close();

            actHotels  = (Hotels) obj;

            //check for active table (pendient)
         }
      } catch(IOException ioe) {
         ioe.printStackTrace();
         return false;
      } catch(ClassNotFoundException ioe) {
         ioe.printStackTrace();
         return false;
      }

   }else if(myRMIName.equals(RMINameCustomers)){
      //reservations
      File file = new File(RMINameCustomers_DB);

      try{
         //check if pointer file exists
         if(!file.exists()){
            reservations = new Reservations();
         }else{
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(RMINameCustomers_DB));
            Object obj = in.readObject();
            in.close();

            reservations  = (Reservations) obj;

            //check for active table (pendient)
         }
      } catch(IOException ioe) {
         ioe.printStackTrace();
         return false;
      } catch(ClassNotFoundException ioe) {
         ioe.printStackTrace();
         return false;
      }


      //reservations
      file = new File(RMINameCustomers_ACTIVE_DB);

      try{
         //check if pointer file exists
         if(!file.exists()){
            actReservations = new Reservations();
         }else{
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(RMINameCustomers_ACTIVE_DB));
            Object obj = in.readObject();
            in.close();

            actReservations  = (Reservations) obj;

            //check for active table (pendient)
         }
      } catch(IOException ioe) {
         ioe.printStackTrace();
         return false;
      } catch(ClassNotFoundException ioe) {
         ioe.printStackTrace();
         return false;
      }

   }
   return true;

}

private void checkAfterFailure()throws RemoteException, InvalidTransactionException{
   Set<Integer> transSet= activeTransactions.keySet();
   for(Integer xid: transSet){ //there is an uncommited transaction after failure, it must abort
      abort(xid);
   }
}


// TRANSACTION INTERFACE
public int start()
throws RemoteException {

   if(!shutdown){ //if there is no shutdown request

      xidCounter++;

      activeTransactions.put(xidCounter, new ArrayList<OperationPair>());

      System.out.println("Transaction "+xidCounter+" started");
      return xidCounter;
   }else
   return -1;

}

public void checkTransactionID(int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException{
   /*if(!activeTransactions.containsKey(xid) && activeTransactions.size()==0){
      throw new TransactionAbortedException(xid, "xid transaction aborted previously");
   }else  if(!activeTransactions.containsKey(xid)){
      throw new InvalidTransactionException(xid, "xid transaction invalid");
   }*/

   if(!activeTransactions.containsKey(xid))
      activeTransactions.put(xid, new ArrayList<OperationPair>());

}

public boolean commit(int xid) throws RemoteException, TransactionAbortedException,
InvalidTransactionException {
   checkTransactionID(xid);
   System.out.println("Committing");
   boolean updateFlight=false, updateCar=false, updateHotel=false, updateReservation=false;

   //reflect the changes over the non-active database
   ArrayList<OperationPair> operations = activeTransactions.get(xid);

   for(OperationPair op: operations){
      //go over each operation and merge it on the non-active tables
      //we assume that each RM component accesses to its active/non-active db only
      switch(op.getTable()){
         case RMINameFlights:
         if(actFlights.containsFlight(op.getKey())){ //if the flight exits, we just update it
            Flight f = actFlights.getFlight(op.getKey());
            flights.addFlight(op.getKey(), new Flight(f.getFlightNum(), f.getPrice(), f.getNumSeats(), f.getNumAvail()));
            actFlights.addFlight(op.getKey(), f);
         }else  //otherwise we need to delete it in non-active db
         flights.deleteFlight(op.getKey());
         updateFlight=true;
         break;

         case RMINameCars:
         if(actCars.containsCar(op.getKey())){
            Car c = actCars.getCar(op.getKey());
            cars.addCar(op.getKey(), new Car(c.getLocation(), c.getPrice(), c.getNumCars(), c.getNumAvail()));
            actCars.addCar(op.getKey(), c);
         }else
         cars.deleteCar(op.getKey());
         updateCar=true;
         break;

         case RMINameRooms:
         if(actHotels.containsHotel(op.getKey())){
            Hotel h = actHotels.getHotel(op.getKey());
            hotels.addHotel(op.getKey(), new Hotel(h.getLocation(), h.getPrice(), h.getNumRooms(), h.getNumAvail()));
            actHotels.addHotel(op.getKey(), h);
         }else
         hotels.deleteHotel(op.getKey());
         updateHotel=true;
         break;

         case RMINameCustomers:
         if(actReservations.containsCustomer(op.getKey())){
            reservations.addReservations(op.getKey(), actReservations.getCloneReservations(op.getKey()));
         }else
         reservations.deleteCustomer(op.getKey());
         updateReservation=true;
         break;

         default: throw new InvalidTransactionException(xid, "Problem merging values to non-active db");

      }
   }

   activeTransactions.remove(xid);
   //updateTransactionsOnDisk();

   if(dieBeforeSwitch)
   System.exit(0);

   //update tables on disk (active and non-active)
   if(updateFlight)
   updateTableOnDisk(RMINameFlights);
   if(updateCar)
   updateTableOnDisk(RMINameCars);
   if(updateHotel)
   updateTableOnDisk(RMINameRooms);
   if(updateReservation)
   updateTableOnDisk(RMINameCustomers);

   lm.unlockAll(xid); // release the lock


   if(dieAfterSwitch)
   System.exit(0);


   return true;
}

public void abort(int xid)throws RemoteException, InvalidTransactionException {
   //PENDIENT: not commit, but undo active database?
   //checkTransactionID(xid);

   undoOperations(xid);
   //updateTransactionsOnDisk();
   activeTransactions.remove(xid);
   abortedTransactions.add(xid);

   lm.unlockAll(xid); // release the lock

   return;
}

private boolean undoOperations(int xid)throws RemoteException,
InvalidTransactionException {

   System.out.println("Undo");


   //reflect the changes over the non-active database
   ArrayList<OperationPair> operations = activeTransactions.get(xid);

   for(OperationPair op: operations){
      //go over each operation and merge it on the non-active tables
      switch(op.getTable()){
         case RMINameFlights:
         if(flights.containsFlight(op.getKey())){ //if the flight exits, we just update it
            Flight f = flights.getFlight(op.getKey());
            actFlights.addFlight(op.getKey(), new Flight(f.getFlightNum(), f.getPrice(), f.getNumSeats(), f.getNumAvail()));
         }else  //otherwise we need to delete it in non-active db
         actFlights.deleteFlight(op.getKey());
         break;

         case RMINameCars:
         if(cars.containsCar(op.getKey())){
            Car c = cars.getCar(op.getKey());
            actCars.addCar(op.getKey(), new Car(c.getLocation(), c.getPrice(), c.getNumCars(), c.getNumAvail()));
         }else
         actCars.deleteCar(op.getKey());
         break;

         case RMINameRooms:
         if(hotels.containsHotel(op.getKey())){
            Hotel h = hotels.getHotel(op.getKey());
            actHotels.addHotel(op.getKey(), new Hotel(h.getLocation(), h.getPrice(), h.getNumRooms(), h.getNumAvail()));

         }else
         actHotels.deleteHotel(op.getKey());
         break;

         case RMINameCustomers:
         if(reservations.containsCustomer(op.getKey()))
         actReservations.addReservations(op.getKey(), reservations.getCloneReservations(op.getKey()));
         else
         actReservations.deleteCustomer(op.getKey());
         break;

         default: throw new InvalidTransactionException(xid, "Problem merging values to non-active db");

      }
   }
   return true;

}

public void updateTransactionsOnDisk(){
   try{
      if(myRMIName.equals(RMINameFlights)){
         File file = new File(Flights_TRANSACTIONS_DB);
         if(!file.exists()) {
            file.createNewFile();
         }
         FileOutputStream fout = new FileOutputStream(file, false);
         ObjectOutputStream oos = new ObjectOutputStream(fout);
         oos.writeObject(activeTransactions);
         oos.close();
      }else if(myRMIName.equals(RMINameCars)){
         File file = new File(Cars_TRANSACTIONS_DB);
         if(!file.exists()) {
            file.createNewFile();
         }
         FileOutputStream fout = new FileOutputStream(file, false);
         ObjectOutputStream oos = new ObjectOutputStream(fout);
         oos.writeObject(activeTransactions);
         oos.close();
      }else if(myRMIName.equals(RMINameRooms)){
         File file = new File(Rooms_TRANSACTIONS_DB);
         if(!file.exists()) {
            file.createNewFile();
         }
         FileOutputStream fout = new FileOutputStream(file, false);
         ObjectOutputStream oos = new ObjectOutputStream(fout);
         oos.writeObject(activeTransactions);
         oos.close();
      }else if(myRMIName.equals(RMINameCustomers)){
         File file = new File(Customers_TRANSACTIONS_DB);
         if(!file.exists()) {
            file.createNewFile();
         }
         FileOutputStream fout = new FileOutputStream(file, false);
         ObjectOutputStream oos = new ObjectOutputStream(fout);
         oos.writeObject(activeTransactions);
         oos.close();
      }

   }catch(Exception ex){
      ex.printStackTrace();
   }
}

private void updateTableOnDisk(String table){
   String currentDir = System.getProperty("user.dir");
   System.out.println("Current dir using System:" +currentDir);
   if(table.equals(RMINameFlights)){
      try{
         //non-active
         File file = new File(RMINameFlights_DB);
         if(!file.exists()) {
            file.createNewFile();
         }
         FileOutputStream fout = new FileOutputStream(file, false);
         ObjectOutputStream oos = new ObjectOutputStream(fout);
         oos.writeObject(flights);
         oos.close();

         //active
         file = new File(RMINameFlights_ACTIVE_DB);
         if(!file.exists()) {
            file.createNewFile();
         }
         fout = new FileOutputStream(RMINameFlights_ACTIVE_DB, false);
         oos = new ObjectOutputStream(fout);
         oos.writeObject(actFlights);
         oos.close();
      }catch(Exception ex){
         ex.printStackTrace();
      }
   }else if(table.equals(RMINameCars)){
      try{
         //non-active
         File file = new File(RMINameCars_DB);
         if(!file.exists()) {
            file.createNewFile();
         }
         FileOutputStream fout = new FileOutputStream(RMINameCars_DB,false);
         ObjectOutputStream oos = new ObjectOutputStream(fout);
         oos.writeObject(cars);
         oos.close();

         //active
         file = new File(RMINameCars_ACTIVE_DB);
         if(!file.exists()) {
            file.createNewFile();
         }
         fout = new FileOutputStream(RMINameCars_ACTIVE_DB, false);
         oos = new ObjectOutputStream(fout);
         oos.writeObject(actCars);
         oos.close();
      }catch(Exception ex){
         ex.printStackTrace();
      }
   }else if(table.equals(RMINameRooms)){
      try{
         //non-active
         File file = new File(RMINameRooms_DB);
         if(!file.exists()) {
            file.createNewFile();
         }
         FileOutputStream fout = new FileOutputStream(RMINameRooms_DB, false);
         ObjectOutputStream oos = new ObjectOutputStream(fout);
         oos.writeObject(hotels);
         oos.close();

         //active
         file = new File(RMINameRooms_ACTIVE_DB);
         if(!file.exists()) {
            file.createNewFile();
         }
         fout = new FileOutputStream(RMINameRooms_ACTIVE_DB, false);
         oos = new ObjectOutputStream(fout);
         oos.writeObject(actHotels);
         oos.close();
      }catch(Exception ex){
         ex.printStackTrace();
      }
   }else if(table.equals(RMINameCustomers)){
      try{
         //non-active
         File file = new File(RMINameCustomers_DB);
         if(!file.exists()) {
            file.createNewFile();
         }
         FileOutputStream fout = new FileOutputStream(RMINameCustomers_DB, false);
         ObjectOutputStream oos = new ObjectOutputStream(fout);
         oos.writeObject(reservations);
         oos.close();

         //active
         file = new File(RMINameCustomers_ACTIVE_DB);
         if(!file.exists()) {
            file.createNewFile();
         }
         fout = new FileOutputStream(RMINameCustomers_ACTIVE_DB, false);
         oos = new ObjectOutputStream(fout);
         oos.writeObject(actReservations);
         oos.close();
      }catch(Exception ex){
         ex.printStackTrace();
      }
   }
}


// ADMINISTRATIVE INTERFACE
public boolean addFlight(int xid, String flightNum, int numSeats, int price)
throws RemoteException, TransactionAbortedException, InvalidTransactionException {
   if(!checkRmiName(RMINameFlights))
   throw new TransactionAbortedException(xid, "Access to invalid component rmiName="+rmiName);

   checkTransactionID(xid);


   //first get a lock for updating the table
   try{
      if(!lm.lock(xid, RMINameFlights + flightNum, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   //the transaction got the X-lock
   //add/uptade the flight in the table
   actFlights.addFlight(flightNum, price, numSeats);


   //keep tracking of operations
   ArrayList<OperationPair> operations = activeTransactions.get(xid);
   operations.add(new OperationPair(RMINameFlights, flightNum));
   activeTransactions.put(xid, operations);

   //TM needs also to keep track
   tm.enlist(xid, RMINameFlights);

   return true;
}

public boolean deleteFlight(int xid, String flightNum)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameFlights))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   //check if there is any reservation over this flight
   //A s-lock for checking reservation is needed?
   if(queryFlightHasReservation(xid, flightNum)){
      return false;
   }
   //otherwise the flight can be deleted

   //first get a lock a update the table
   try{
      if(!lm.lock(xid, RMINameFlights + flightNum, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   //the transaction got the X-lock
   //add/uptade the flight in the table
   actFlights.deleteFlight(flightNum);

   //keep tracking of operations
   ArrayList<OperationPair> operations = activeTransactions.get(xid);
   operations.add(new OperationPair(RMINameFlights, flightNum));
   activeTransactions.put(xid, operations);

   //TM needs also to keep track
   tm.enlist(xid, RMINameFlights);

   return true;
}

public boolean addRooms(int xid, String location, int numRooms, int price)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameRooms))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   //first get a lock for updating the table
   try{
      if(!lm.lock(xid, RMINameRooms + location, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   //the transaction got the X-lock
   //add/uptade the hotel in the table
   actHotels.addRooms(location, price, numRooms);


   //keep tracking of operations
   ArrayList<OperationPair> operations = activeTransactions.get(xid);
   operations.add(new OperationPair(RMINameRooms, location));
   activeTransactions.put(xid, operations);

   //TM needs also to keep track
   tm.enlist(xid, RMINameRooms);

   return true;
}

public boolean deleteRooms(int xid, String location, int numRooms)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameRooms))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   //first get a lock for updating the table
   try{
      if(!lm.lock(xid, RMINameRooms + location, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }
   //the transaction got the X-lock
   //add/uptade the hotel in the table
   if(actHotels.deleteRooms(location, numRooms)){
      //keep tracking of operations
      ArrayList<OperationPair> operations = activeTransactions.get(xid);
      operations.add(new OperationPair(RMINameRooms, location));
      activeTransactions.put(xid, operations);

      //TM needs also to keep track
      tm.enlist(xid, RMINameRooms);

      return true;

   }else
   return false;
}

public boolean addCars(int xid, String location, int numCars, int price)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameCars))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   //first get a lock for updating the table
   try{
      if(!lm.lock(xid, RMINameCars + location, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   //the transaction got the X-lock
   //add/uptade the Car in the table
   actCars.addCars(location, price, numCars);

   //keep tracking of operations
   ArrayList<OperationPair> operations = activeTransactions.get(xid);
   operations.add(new OperationPair(RMINameCars, location));
   activeTransactions.put(xid, operations);

   //TM needs also to keep track
   tm.enlist(xid, RMINameCars);

   return true;
}

public boolean deleteCars(int xid, String location, int numCars)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameCars))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   //first get a lock for updating the table
   try{
      if(!lm.lock(xid, RMINameCars + location, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }
   //the transaction got the X-lock
   //add/uptade the car in the table
   if(actCars.deleteCars(location, numCars)){
      //keep tracking of operations
      ArrayList<OperationPair> operations = activeTransactions.get(xid);
      operations.add(new OperationPair(RMINameCars, location));
      activeTransactions.put(xid, operations);

      //TM needs also to keep track
      tm.enlist(xid, RMINameCars);

      return true;
   }else
   return false;
}

public boolean newCustomer(int xid, String custName)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameCustomers) && !checkRmiName(RMINameCustomers))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   //first get a lock for updating the table
   try{
      if(!lm.lock(xid, RMINameCustomers + custName, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   //the transaction got the X-lock
   //add/uptade the reservation in the table
   if(actReservations.addCustomer(custName)){
      //keep tracking of operations
      ArrayList<OperationPair> operations = activeTransactions.get(xid);
      operations.add(new OperationPair(RMINameCustomers, custName));
      activeTransactions.put(xid, operations);

      //TM needs also to keep track
      tm.enlist(xid, RMINameCustomers);

      return true;
   }else
   return false;
}

public boolean deleteCustomer(int xid, String custName)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameCustomers))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   //first get a lock for updating the table
   try{
      if(!lm.lock(xid, RMINameCustomers + custName, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }
   //the transaction got the X-lock

   ArrayList<OperationPair> operations = activeTransactions.get(xid);

   //all the reservations must be cancel
   ArrayList<ResvPair> res=actReservations.getReservations(custName);

   for(ResvPair pair: res){
      if(pair.getResvType()==Reservations.RMINameFlights_TYPE){
         try{
            if(!lm.lock(xid, RMINameFlights + pair.getResvKey(), LockManager.WRITE)){
               return false;
            }
         }catch(DeadlockException e){
            //deal with the deadlock
            //abort the transaction
            abort(xid);
            throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
         }

         actFlights.cancelReservation(pair.getResvKey());
         operations.add(new OperationPair(RMINameFlights, pair.getResvKey()));
         tm.enlist(xid, RMINameFlights);
      }else if(pair.getResvType()==Reservations.RMINameRooms_TYPE){
         try{
            if(!lm.lock(xid, RMINameRooms + pair.getResvKey(), LockManager.WRITE)){
               return false;
            }
         }catch(DeadlockException e){
            //deal with the deadlock
            //abort the transaction
            abort(xid);
            throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
         }

         actHotels.cancelReservation(pair.getResvKey());
         operations.add(new OperationPair(RMINameRooms, pair.getResvKey()));
         //TM needs also to keep track
         tm.enlist(xid, RMINameRooms);
      }else if(pair.getResvType()==Reservations.RMINameCars_TYPE){
         try{
            if(!lm.lock(xid, RMINameCars + pair.getResvKey(), LockManager.WRITE)){
               return false;
            }
         }catch(DeadlockException e){
            //deal with the deadlock
            //abort the transaction
            abort(xid);
            throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
         }

         actCars.cancelReservation(pair.getResvKey());
         operations.add(new OperationPair(RMINameCars, pair.getResvKey()));
         //TM needs also to keep track
         tm.enlist(xid, RMINameCars);
      }


   }

   //add/uptade the reservation in the table
   if(actReservations.deleteCustomer(custName)){
      //keep tracking of operations
      operations.add(new OperationPair(RMINameCustomers, custName));
      activeTransactions.put(xid, operations);
      //TM needs also to keep track
      tm.enlist(xid, RMINameCustomers);
      return true;
   }else
   return false;
}


// QUERY INTERFACE
public int queryFlight(int xid, String flightNum)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameFlights))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   try{
      if(!lm.lock(xid, RMINameFlights + flightNum, LockManager.READ)){
         return -1;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   //transaction got the S-lock
   if(actFlights.containsFlight(flightNum)){
      return actFlights.getFlight(flightNum).getNumAvail();
   }else
   return -1;

}

public int queryFlightPrice(int xid, String flightNum)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameFlights))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   try{
      if(!lm.lock(xid, RMINameFlights + flightNum, LockManager.READ)){
         return -1;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   //transaction got the S-lock
   if(actFlights.containsFlight(flightNum)){
      return actFlights.getFlight(flightNum).getPrice();
   }else
   return -1;
}

public boolean queryFlightHasReservation(int xid, String flightNum)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameFlights))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   try{
      if(!lm.lock(xid, RMINameFlights + flightNum, LockManager.READ)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   //transaction got the S-lock
   if(!actFlights.containsFlight(flightNum))
   return false;
   else
   return actFlights.getFlight(flightNum).getNumSeats()>actFlights.getFlight(flightNum).getNumAvail();
}

public int queryRooms(int xid, String location)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameRooms))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   try{
      if(!lm.lock(xid, RMINameRooms + location, LockManager.READ)){
         return -1;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }


   if(!actHotels.containsHotel(location))
   return -1;
   else
   return actHotels.getHotel(location).getNumAvail();
}

public int queryRoomsPrice(int xid, String location)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameRooms))
   throw new TransactionAbortedException(xid, "Access to invalid component");


   checkTransactionID(xid);

   try{
      if(!lm.lock(xid, RMINameRooms + location, LockManager.READ)){
         return -1;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   //transaction got the S-lock


   if(!actHotels.containsHotel(location))
   return -1;
   else
   return actHotels.getHotel(location).getPrice();
}

public boolean queryHotelHasReserve(int xid, String location)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameRooms))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);



   try{
      if(!lm.lock(xid, RMINameRooms + location, LockManager.READ)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   //transaction got the S-lock

   if(!actHotels.containsHotel(location))
   return false;
   else
   return actHotels.getHotel(location).getNumRooms()>actHotels.getHotel(location).getNumAvail();
}

public int queryCars(int xid, String location)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameCars))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   try{
      if(!lm.lock(xid, RMINameCars + location, LockManager.READ)){
         return -1;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   //transaction got the S-lock
   if(actCars.containsCar(location)){
      return actCars.getCar(location).getNumAvail();
   }else
   return -1;
}

public int queryCarsPrice(int xid, String location)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameCars))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   try{
      if(!lm.lock(xid, RMINameCars + location, LockManager.READ)){
         return -1;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   //transaction got the S-lock
   if(actCars.containsCar(location)){
      return actCars.getCar(location).getPrice();
   }else
   return -1;
}

public boolean queryCarHasReserve(int xid, String location)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameCars))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   try{
      if(!lm.lock(xid, RMINameRooms + location, LockManager.READ)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   //transaction got the S-lock

   if(!actCars.containsCar(location))
   return false;
   else
   return actCars.getCar(location).getNumCars()>actCars.getCar(location).getNumAvail();
}

public int queryCustomerBill(int xid, String custName)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameCustomers) && ! checkRmiName(RMINameCustomers))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   int bill=0;

   try{
      if(!lm.lock(xid, RMINameCustomers + custName, LockManager.READ)){
         return -1;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   if(!actReservations.containsCustomer(custName))
   return -1;

   ArrayList<ResvPair> reservs = actReservations.getReservations(custName);

   for(ResvPair pair: reservs){
      switch(pair.getResvType()){
         case Reservations.RMINameFlights_TYPE:	bill+=this.queryFlightPrice(xid, pair.getResvKey());
         break;
         case Reservations.RMINameRooms_TYPE:     bill+=this.queryRoomsPrice(xid, pair.getResvKey());
         break;
         case Reservations.RMINameCars_TYPE:      bill+=this.queryCarsPrice(xid, pair.getResvKey());
         break;
      }
   }

   return bill;
}


// RMINameCustomers INTERFACE
public boolean reserveFlight(int xid, String custName, String flightNum)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameFlights))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);


   if(!actFlights.containsFlight(flightNum)) // the flight was recently removed?
   return false;

   if(queryFlight(xid, flightNum)<1){
      //there is no seat available
      return false;
   }
   //otherwise, we can make the reservation
   try{
      if(!lm.lock(xid, RMINameCustomers + custName, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   try{ //get the lock to reserve the seat
      if(!lm.lock(xid, RMINameFlights + flightNum, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   if(actFlights.reserveSeat(flightNum, 1)){
      if(tm.addCustomerReservation(xid, custName, Reservations.RMINameFlights_TYPE, flightNum)){
         //keep tracking of operations
         ArrayList<OperationPair> operations = activeTransactions.get(xid);
         operations.add(new OperationPair(RMINameFlights, flightNum));
         activeTransactions.put(xid, operations);

         return true;
      }
   }
   return false;

}

public boolean reserveCar(int xid, String custName, String location)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameCars))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   if(!actCars.containsCar(location)) // the flight was recently removed?
   return false;

   if(queryCars(xid, location)<1){
      //there is no seat available
      return false;
   }
   //otherwise, we can make the reservation
   try{
      if(!lm.lock(xid, RMINameCustomers + custName, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }


   try{ //get the lock to reserve the car
      if(!lm.lock(xid, RMINameCars + location, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   if(actCars.reserveCar(location, 1)){
      if(tm.addCustomerReservation(xid, custName, Reservations.RMINameCars_TYPE, location)){
         //keep tracking of operations
         ArrayList<OperationPair> operations = activeTransactions.get(xid);
         operations.add(new OperationPair(RMINameCars, location));
         activeTransactions.put(xid, operations);
         return true;
      }
   }




   return false;

}

public boolean reserveRoom(int xid, String custName, String location)
throws RemoteException,
TransactionAbortedException,
InvalidTransactionException {
   if(!checkRmiName(RMINameRooms))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   if(!actHotels.containsHotel(location)) // the flight was recently removed?
   return false;

   if(queryRooms(xid, location)<1){
      //there is no seat available
      return false;
   }
   //otherwise, we can make the reservation
   try{
      if(!lm.lock(xid, RMINameCustomers + custName, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   try{ //get the lock to reserve the room
      if(!lm.lock(xid, RMINameRooms + location, LockManager.WRITE)){
         return false;
      }
   }catch(DeadlockException e){
      //deal with the deadlock
      //abort the transaction
      abort(xid);
      throw new TransactionAbortedException(xid, "Transaction aborted by deadlock issue");
   }

   if(actHotels.reserveRoom(location, 1)){
      if(tm.addCustomerReservation(xid, custName, Reservations.RMINameRooms_TYPE, location)){
         //keep tracking of operations
         ArrayList<OperationPair> operations = activeTransactions.get(xid);
         operations.add(new OperationPair(RMINameRooms, location));
         activeTransactions.put(xid, operations);
         return true;
      }
   }
   return false;
}


// TECHNICAL/TESTING INTERFACE
public boolean shutdown()
throws RemoteException {
   //wait for all active transactions end.
   //do not allow more transactions
   shutdown=true;
   waitForShutDown();
   System.exit(0);
   return true;
}

public boolean dieNow()
throws RemoteException {
   //remove the DB files to get new tests


   System.exit(1);
   return true; // We won't ever get here since we exited above;
   // but we still need it to please the compiler.
}


public boolean dieBeforePointerSwitch()
throws RemoteException {
   dieBeforeSwitch=true;
   return true;
}

public boolean dieAfterPointerSwitch()
throws RemoteException {
   dieAfterSwitch=true;
   return true;
}

private boolean waitForShutDown(){
   while(activeTransactions.size()>0){
      try {
         Thread.sleep(1000);       //wait for 1000 milliseconds
      } catch(InterruptedException ex) {
         Thread.currentThread().interrupt();
      }
   }
   return true;
}


private boolean checkRmiName(String operation){
   if(myRMIName.equals(ResourceManager.DefaultRMIName) || myRMIName.equals(operation))
   return true;
   else return false;
}

public boolean addCustomerReservation(int xid, String custName, int type, String key)throws TransactionAbortedException, RemoteException, InvalidTransactionException{
   if(!checkRmiName(RMINameCustomers))
   throw new TransactionAbortedException(xid, "Access to invalid component");

   checkTransactionID(xid);

   ArrayList<OperationPair> operations = activeTransactions.get(xid);
   operations.add(new OperationPair(RMINameCustomers, custName));

   return actReservations.addReservation(custName, type, key);


}

public String toStringNonActiveDB(String nameTable){
   String s="table: "+nameTable+"\n";
   switch(nameTable){
      case RMINameFlights: s+=flights.toString();
      break;
      case RMINameCars: s+=cars.toString();
      break;
      case RMINameRooms: s+=hotels.toString();
      break;
      case RMINameCustomers: s+=reservations.toString();
      break;
      default:     break;
   }
   return s;
}
}
