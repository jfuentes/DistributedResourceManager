package transaction;

import java.rmi.*;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

/**
* Transaction Manager for the Distributed Travel Reservation System.
*
* Description: toy implementation of the TM
*/

public class TransactionManagerImpl
extends java.rmi.server.UnicastRemoteObject
implements TransactionManager {

   protected ResourceManager rmFlights = null;
   protected ResourceManager rmRooms = null;
   protected ResourceManager rmCars = null;
   protected ResourceManager rmCustomers = null;


   protected int xidCounter;

   //transactions
   private HashMap<Integer, Set<String>> activeTransactions;
   private Set<Integer> abortedTransactions;

   public static void main(String args[]) {
      System.setSecurityManager(new RMISecurityManager());

      String rmiPort = System.getProperty("rmiPort");
      if (rmiPort == null) {
         rmiPort = "";
      } else if (!rmiPort.equals("")) {
         rmiPort = "//:" + rmiPort + "/";
      }

      try {
         TransactionManagerImpl obj = new TransactionManagerImpl();
         Naming.rebind(rmiPort + TransactionManager.RMIName, obj);
         System.out.println("TM bound");
      }
      catch (Exception e) {
         System.err.println("TM not bound:" + e);
         System.exit(1);
      }
   }


   public TransactionManagerImpl() throws RemoteException {
      activeTransactions = new HashMap<Integer, Set<String>>();
      xidCounter=0;
   }

   public boolean reconnect(){
      System.setSecurityManager(new RMISecurityManager());

      String rmiPort = System.getProperty("rmiPort");
      if (rmiPort == null) {
         rmiPort = "";
      } else if (!rmiPort.equals("")) {
         rmiPort = "//:" + rmiPort + "/";
      }

      try {
         rmFlights = (ResourceManager)Naming.lookup(rmiPort + ResourceManager.RMINameFlights);
         System.out.println(RMIName + " bound to "+ ResourceManager.RMINameFlights);
         rmRooms = (ResourceManager)Naming.lookup(rmiPort + ResourceManager.RMINameRooms);
         System.out.println(RMIName + " bound to "+ ResourceManager.RMINameRooms);
         rmCars = (ResourceManager)Naming.lookup(rmiPort + ResourceManager.RMINameCars);
         System.out.println(RMIName + " bound to "+ResourceManager.RMINameCars);
         rmCustomers = (ResourceManager)Naming.lookup(rmiPort + ResourceManager.RMINameCustomers);
         System.out.println(RMIName + " bound to "+ ResourceManager.RMINameCustomers);

      }
      catch (Exception e) {
         System.err.println(RMIName + " cannot bind to a component:" + e);
         return false;
      }

      return true;
   }

   public boolean dieNow()
   throws RemoteException {
      System.exit(1);
      return true; // We won't ever get here since we exited above;
      // but we still need it to please the compiler.
   }

   public int start() throws RemoteException {

   //   if(!shutdown){ //if there is no shutdown request
         if(rmFlights==null || rmCars==null || rmRooms==null || rmCustomers==null)
         while (!reconnect()) {
            // would be better to sleep a while
            try {
               Thread.sleep(1000);
            } catch(InterruptedException ex) {
               Thread.currentThread().interrupt();
            }
         }

         xidCounter++;

         activeTransactions.put(xidCounter, new HashSet<String>());

         System.out.println("Transaction "+xidCounter+" started");
         return xidCounter;
   //   }else
   //   return -1;

   }

   public boolean commit(int xid) throws RemoteException, TransactionAbortedException,
   InvalidTransactionException {
      checkTransactionID(xid);
      System.out.println("Committing");
      boolean updateFlight=false, updateCar=false, updateHotel=false, updateReservation=false;

      //reflect the changes over the non-active database
      Set<String> operations = activeTransactions.get(xid);

      for(String op: operations){
         //go over each operation and merge it on the non-active tables
         //we assume that each RM component accesses to its active/non-active db only
         switch(op){
            case ResourceManager.RMINameFlights:
            rmFlights.commit(xid);
            break;

            case ResourceManager.RMINameCars:
            rmCars.commit(xid);
            break;

            case ResourceManager.RMINameRooms:
            rmRooms.commit(xid);
            break;

            case ResourceManager.RMINameCustomers:
            rmCustomers.commit(xid);
            break;

            default: throw new InvalidTransactionException(xid, "Problem merging values to non-active db");

         }
      }

      activeTransactions.remove(xid);
      //updateTransactionsOnDisk();

      //if(dieBeforeSwitch)
      //System.exit(0);


      //if(dieAfterSwitch)
      //System.exit(0);


      return true;
   }


   public void abort(int xid)throws RemoteException, InvalidTransactionException {
      Set<String> operations = activeTransactions.get(xid);

      for(String op: operations){
         //go over each operation and merge it on the non-active tables
         //we assume that each RM component accesses to its active/non-active db only
         switch(op){
            case ResourceManager.RMINameFlights:
            rmFlights.abort(xid);
            break;

            case ResourceManager.RMINameCars:
            rmCars.abort(xid);
            break;

            case ResourceManager.RMINameRooms:
            rmRooms.abort(xid);
            break;

            case ResourceManager.RMINameCustomers:
            rmCustomers.abort(xid);
            break;

            default: throw new InvalidTransactionException(xid, "Problem merging values to non-active db");

         }
      }
      //updateTransactionsOnDisk();
      activeTransactions.remove(xid);
      abortedTransactions.add(xid);



      return;
   }

   public void enlist(int xid, String component)throws RemoteException{
      if(!activeTransactions.containsKey(xid)){
         Set<String> ops= new HashSet<String>();
         ops.add(component);
         activeTransactions.put(xid, ops);
      }else{
         Set<String> ops = activeTransactions.get(xid);
         ops.add(component);
         activeTransactions.put(xid,ops);
      }
   }

   public void checkTransactionID(int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException{
      if(!activeTransactions.containsKey(xid) && activeTransactions.size()==0){
         throw new TransactionAbortedException(xid, "xid transaction aborted previously");
      }else  if(!activeTransactions.containsKey(xid)){
         throw new InvalidTransactionException(xid, "xid transaction invalid");
      }

   }

   public boolean addCustomerReservation(int xid, String custName, int type, String key)throws TransactionAbortedException, RemoteException, InvalidTransactionException{

      return rmCustomers.addCustomerReservation(xid, custName, type, key);
   }

}
