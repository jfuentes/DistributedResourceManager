package transaction;

import java.rmi.*;
import java.util.*;

/**
* Workflow Controller for the Distributed Travel Reservation System.
*
* Description: toy implementation of the WC.  In the real
* implementation, the WC should forward calls to either RM or TM,
* instead of doing the things itself.
*/

public class WorkflowControllerImpl
extends java.rmi.server.UnicastRemoteObject
implements WorkflowController {
   protected int flightcounter, flightprice, carscounter, carsprice, roomscounter, roomsprice;
   protected int xidCounter;


   protected ResourceManager rmFlights = null;
   protected ResourceManager rmRooms = null;
   protected ResourceManager rmCars = null;
   protected ResourceManager rmCustomers = null;
   protected TransactionManager tm = null;

   public static void main(String args[]) {
      System.setSecurityManager(new RMISecurityManager());

      String rmiPort = System.getProperty("rmiPort");
      if (rmiPort == null) {
         rmiPort = "";
      } else if (!rmiPort.equals("")) {
         rmiPort = "//:" + rmiPort + "/";
      }

      try {
         WorkflowControllerImpl obj = new WorkflowControllerImpl();
         Naming.rebind(rmiPort + WorkflowController.RMIName, obj);
         System.out.println("WC bound");
      }
      catch (Exception e) {
         System.err.println("WC not bound:" + e);
         System.exit(1);
      }
   }


   public WorkflowControllerImpl() throws RemoteException {


      while (!reconnect()) {
         // would be better to sleep a while
         try {
            Thread.sleep(1000);
         } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
         }
      }
   }


   // TRANSACTION INTERFACE
   public int start()
   throws RemoteException {
      return tm.start();
   }

   public boolean commit(int xid)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      System.out.println("Committing");
      return tm.commit(xid);
   }

   public void abort(int xid)
   throws RemoteException,
   InvalidTransactionException {
      tm.abort(xid);
      return;
   }


   // ADMINISTRATIVE INTERFACE
   public boolean addFlight(int xid, String flightNum, int numSeats, int price)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmFlights.addFlight(xid, flightNum, numSeats, price);
   }

   public boolean deleteFlight(int xid, String flightNum)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmFlights.deleteFlight(xid, flightNum);
   }

   public boolean addRooms(int xid, String location, int numRooms, int price)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmRooms.addRooms(xid, location, numRooms, price);
   }

   public boolean deleteRooms(int xid, String location, int numRooms)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmRooms.deleteRooms(xid, location, numRooms);
   }

   public boolean addCars(int xid, String location, int numCars, int price)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmCars.addCars(xid, location, numCars, price);
   }

   public boolean deleteCars(int xid, String location, int numCars)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmCars.deleteCars(xid, location, numCars);
   }

   public boolean newCustomer(int xid, String custName)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmCustomers.newCustomer(xid, custName);
   }

   public boolean deleteCustomer(int xid, String custName)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmCustomers.deleteCustomer(xid, custName);
   }


   // QUERY INTERFACE
   public int queryFlight(int xid, String flightNum)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmFlights.queryFlight(xid, flightNum);
   }

   public int queryFlightPrice(int xid, String flightNum)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmFlights.queryFlightPrice(xid, flightNum);
   }

   public int queryRooms(int xid, String location)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmRooms.queryRooms(xid, location);
   }

   public int queryRoomsPrice(int xid, String location)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmRooms.queryRoomsPrice(xid, location);
   }

   public int queryCars(int xid, String location)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmCars.queryCars(xid, location);
   }

   public int queryCarsPrice(int xid, String location)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmCars.queryCarsPrice(xid, location);
   }

   public int queryCustomerBill(int xid, String custName)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmCustomers.queryCustomerBill(xid, custName);
   }


   // RESERVATION INTERFACE
   public boolean reserveFlight(int xid, String custName, String flightNum)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmFlights.reserveFlight(xid, custName, flightNum);
   }

   public boolean reserveCar(int xid, String custName, String location)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmCars.reserveCar(xid, custName, location);
   }

   public boolean reserveRoom(int xid, String custName, String location)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      return rmRooms.reserveRoom(xid, custName, location);
   }

   public boolean reserveItinerary(int xid, String custName, List flightNumList, String location, boolean needCar, boolean needRoom)
   throws RemoteException,
   TransactionAbortedException,
   InvalidTransactionException {
      for(int i=0; i< flightNumList.size(); i++){
         String s= (String) flightNumList.get(i);
         if(!rmFlights.reserveFlight(xid, custName, s)){
            rmFlights.abort(xid);
            return false;
         }
      }
      if(needCar){
         if(!rmCars.reserveCar(xid, custName, location)){
            rmFlights.abort(xid);
            rmCars.abort(xid);
            return false;
         }
      }
      if(needRoom){
         if(!rmRooms.reserveRoom(xid, custName, location)){
            rmFlights.abort(xid);
            if(needCar) rmCars.abort(xid);
            rmRooms.abort(xid);
            return false;
         }
      }

      return true;
   }

   // TECHNICAL/TESTING INTERFACE
   public boolean reconnect()
   throws RemoteException {
      String rmiPort = System.getProperty("rmiPort");
      if (rmiPort == null) {
         rmiPort = "";
      } else if (!rmiPort.equals("")) {
         rmiPort = "//:" + rmiPort + "/";
      }

      try {
         rmFlights =
         (ResourceManager)Naming.lookup(rmiPort +
         ResourceManager.RMINameFlights);
         System.out.println("WC bound to RMFlights");
         rmRooms =
         (ResourceManager)Naming.lookup(rmiPort +
         ResourceManager.RMINameRooms);
         System.out.println("WC bound to RMRooms");
         rmCars =
         (ResourceManager)Naming.lookup(rmiPort +
         ResourceManager.RMINameCars);
         System.out.println("WC bound to RMCars");
         rmCustomers =
         (ResourceManager)Naming.lookup(rmiPort +
         ResourceManager.RMINameCustomers);
         System.out.println("WC bound to RMCustomers");
         tm =
         (TransactionManager)Naming.lookup(rmiPort +
         TransactionManager.RMIName);
         System.out.println("WC bound to TM");
      }
      catch (Exception e) {
         System.err.println("WC cannot bind to some component:" + e);
         return false;
      }

      try {
         if (rmFlights.reconnect() && rmRooms.reconnect() &&
         rmCars.reconnect() && rmCustomers.reconnect()) {
            return true;
         }
      } catch (Exception e) {
         System.err.println("Some RM cannot reconnect:" + e);
         return false;
      }

      return false;
   }

   public boolean dieNow(String who)
   throws RemoteException {
      if (who.equals(TransactionManager.RMIName) ||
      who.equals("ALL")) {
         try {
            tm.dieNow();
         } catch (RemoteException e) {}
         }
         if (who.equals(ResourceManager.RMINameFlights) ||
         who.equals("ALL")) {
            try {
               rmFlights.dieNow();
            } catch (RemoteException e) {}
            }
            if (who.equals(ResourceManager.RMINameRooms) ||
            who.equals("ALL")) {
               try {
                  rmRooms.dieNow();
               } catch (RemoteException e) {}
               }
               if (who.equals(ResourceManager.RMINameCars) ||
               who.equals("ALL")) {
                  try {
                     rmCars.dieNow();
                  } catch (RemoteException e) {}
                  }
                  if (who.equals(ResourceManager.RMINameCustomers) ||
                  who.equals("ALL")) {
                     try {
                        rmCustomers.dieNow();
                     } catch (RemoteException e) {}
                     }
                     if (who.equals(WorkflowController.RMIName) ||
                     who.equals("ALL")) {
                        System.exit(1);
                     }
                     return true;
                  }
                  public boolean dieRMAfterEnlist(String who)
                  throws RemoteException {
                     return true;
                  }
                  public boolean dieRMBeforePrepare(String who)
                  throws RemoteException {
                     return true;
                  }
                  public boolean dieRMAfterPrepare(String who)
                  throws RemoteException {
                     return true;
                  }
                  public boolean dieTMBeforeCommit()
                  throws RemoteException {
                     return true;
                  }
                  public boolean dieTMAfterCommit()
                  throws RemoteException {
                     return true;
                  }
                  public boolean dieRMBeforeCommit(String who)
                  throws RemoteException {
                     return true;
                  }
                  public boolean dieRMBeforeAbort(String who)
                  throws RemoteException {
                     return true;
                  }
               }
