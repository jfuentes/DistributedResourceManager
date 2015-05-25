package transaction;

import java.rmi.*;

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

      while (!reconnect()) {
         // would be better to sleep a while
      }
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
         System.out.println(RMIName + " bound to RMFlights");
         rmRooms = (ResourceManager)Naming.lookup(rmiPort + ResourceManager.RMINameRooms);
         System.out.println(RMIName + " bound to RMRooms");
         rmCars = (ResourceManager)Naming.lookup(rmiPort + ResourceManager.RMINameCars);
         System.out.println(RMIName + " bound to RMCars");
         rmCustomers = (ResourceManager)Naming.lookup(rmiPort + ResourceManager.RMINameCustomers);
         System.out.println(RMIName + " bound to RMCustomers");

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

}
