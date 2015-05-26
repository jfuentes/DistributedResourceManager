package transaction;

import java.rmi.*;

/**
 * Interface for the Transaction Manager of the Distributed Travel
 * Reservation System.
 * <p>
 * Unlike WorkflowController.java, you are supposed to make changes
 * to this file.
 */




public interface TransactionManager extends Remote {

    public boolean dieNow()
	throws RemoteException;

   /*
   * Method to keep track a new transaction 
   */

   public void enlist(int xid, String component)throws RemoteException;

   /*
   * new method to add reservation to Customers table through TransactionManager
   */
   public boolean addCustomerReservation(int xid, String custName, int type, String key)
   throws TransactionAbortedException, RemoteException, InvalidTransactionException;


   //////////
   // TRANSACTION INTERFACE
   //////////
   /**
    * Start a new transaction, and return its transaction id.
    *
    * @return A unique transaction ID > 0.
    *
    * @throws RemoteException on communications failure.
    */
   public int start()
   throws RemoteException;
   /**
    * Commit transaction.
    *
    * @param xid id of transaction to be committed.
    * @return true on success, false on failure.
    *
    * @throws RemoteException on communications failure.
    * @throws TransactionAbortedException if transaction was aborted.
    * @throws InvalidTransactionException if transaction id is invalid.
    */
   public boolean commit(int xid)
   throws RemoteException,
         TransactionAbortedException,
         InvalidTransactionException;
   /**
    * Abort transaction.
    *
    * @param xid id of transaction to be aborted.
    *
    * @throws RemoteException on communications failure.
    * @throws InvalidTransactionException if transaction id is invalid.
    */
   public void abort(int xid)
   throws RemoteException,
         InvalidTransactionException;


    /** The RMI name a TransactionManager binds to. */
    public static final String RMIName = "TM";
}
