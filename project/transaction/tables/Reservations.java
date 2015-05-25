//implementation of the Reservations table
package transaction.tables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

public class Reservations implements Serializable{
	/**
	* This table represents the combination of Customers and their reservations
	* The table is represented with a Hash Table where the primary key is custName
	* and a list of pairs (resvType, resvKey)
	**/
	private Map<String, ArrayList<ResvPair>> table;
	public final static int RMINameFlights_TYPE=1;
	public final static int RMINameRooms_TYPE=2;
	public final static int RMINameCars_TYPE=3;

	public Reservations(){
		table = new HashMap<String, ArrayList<ResvPair>>();
	}

	public boolean addCustomer(String custName){
		if(table.containsKey(custName)){
			return false;
		}else{
			table.put(custName, new ArrayList<ResvPair>());
		}
		return true;
	}

	public boolean deleteCustomer(String custName){
		if(table.containsKey(custName)){
			table.remove(custName);
			return true;
		}else
			return false;
	}

	public boolean addReservation(String custName, int resvType, String resvKey){
		if(table.containsKey(custName)){
			//We assume that if the customer already exists we add a new reservation
			ArrayList<ResvPair> resvPairs = table.get(custName);
			resvPairs.add(new ResvPair(resvType, resvKey));
			table.put(custName, resvPairs);
		}else{
			ArrayList<ResvPair> resvPairs = new ArrayList<ResvPair>();
			resvPairs.add(new ResvPair(resvType, resvKey));
			table.put(custName, resvPairs);
		}
		return true;
	}

	public boolean deleteReservation(String custName, int resvType, String resvKey){
		if(!table.containsKey(custName)){
			return false;
		}

		ArrayList<ResvPair> reservations = table.get(custName);
		reservations.remove(new ResvPair(resvType, resvKey));
		return true;
	}

	public ArrayList<ResvPair> getReservations(String custName){
		return table.get(custName);
	}

	public ArrayList<ResvPair> getCloneReservations(String custName){
		ArrayList<ResvPair> res = new ArrayList<ResvPair>();
		ArrayList<ResvPair> reservations = table.get(custName);
		for (ResvPair pair : reservations){
			res.add(new ResvPair(pair.getResvType(), pair.getResvKey()));
		}

		return res;
	}

	public void addReservations(String custName, ArrayList<ResvPair> array){
		table.put(custName, array);
	}

	public boolean existsReservation(int resvType, String resvKey){
		for (String key : table.keySet()){
			ArrayList<ResvPair> reservations = table.get(key);
			for (ResvPair pair : reservations){
				if(resvType==pair.getResvType() && resvKey==pair.getResvKey())
					return true;
			}
		}
		return false;
	}

	public boolean containsCustomer(String custName){
		return table.containsKey(custName);
	}

	public Reservations clone(){
		Reservations res = new Reservations();

		for (String key : table.keySet()){
			ArrayList<ResvPair> reservations = table.get(key);
			for (ResvPair pair : reservations){
				res.addReservation(key, pair.getResvType(), pair.getResvKey());
			}
		}

		return res;
	}


}
