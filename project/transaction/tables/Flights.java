//implementation of the Flights table
package transaction.tables;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.Serializable;

public class Flights implements Serializable{
  //attributes
  private Map<String, Flight> table;
  //perhaps we will add more attributes to guarantee ACID


  //constructor
  public Flights(){
    table= new HashMap<String, Flight>();
  }


  /**
  * Methods to add, update, remove and search tuples on the table
  **/

  public boolean addFlight(String flightNum, int price, int numSeats){
    if(table.containsKey(flightNum)){
      //the flight already exists, update
      Flight flight = table.get(flightNum);
		if(price>=0)
         flight.setPrice(price); //update to the new price
		flight.setNumSeats(numSeats+flight.getNumSeats()); //add the new seats
      flight.setNumAvail(numSeats+flight.getNumAvail()); //add the new seats
			table.put(flightNum, flight);
    }else
      table.put(flightNum, new Flight(flightNum, price, numSeats, numSeats));
    return true;
  }

  public boolean reserveSeat(String flightNum, int numSeats){
     if(table.containsKey(flightNum)){
      //the flight already exists, update
      Flight flight = table.get(flightNum);
      if(flight.getNumAvail()-numSeats>=0){
         flight.setNumAvail(flight.getNumAvail()-numSeats); //add the new seats
         table.put(flightNum, flight);
         return true;
      }else
      return false;
     }else
         return false;
 }

  public void addFlight(String flightNum, Flight flight){
    table.put(flightNum, flight);
  }

  public boolean deleteFlight(String flightNum){
    if(table.get(flightNum)==null){
      //the flight does not exist
      return false;
    }
    table.remove(flightNum);
    return true;
  }

  public Flight getFlight(String flightNum){
    return table.get(flightNum);
  }

  public boolean containsFlight(String flightNum){
    return table.containsKey(flightNum);
  }


  public boolean cancelReservation(String flightNum){
     if(table.containsKey(flightNum)){
      //the flight already exists, update
      Flight flight = table.get(flightNum);
      flight.setNumAvail(1+flight.getNumAvail());
      table.put(flightNum, flight);
      return true;
     }else
      return false;
  }

  public String toString(){
    String s="";
    Set<String> keys = table.keySet();
    for(String key: keys){
      s+="| "+key+" | "+table.get(key).toString()+" |\n";
    }
    return s;
  }
}
