package transaction.tables;

import java.io.Serializable;
//implementation of a Tuple for Flights table

public class Car implements Serializable{
	//attributes
	private String location;
	private int price;
	private int numCars;
	private int numAvail;

	public Car(String location, int price, int numCars, int numAvail){
		this.location = location;
		this.price = price;
		this.numCars = numCars;
		this.numAvail = numAvail;
	}

	public String getLocation(){
		return location;
	}

	public void setLocation(String location){
		this.location = location;
	}

	public int getPrice(){
		return price;
	}

	public void setPrice(int price){
		this.price = price;
	}

	public int getNumCars(){
		return numCars;
	}

	public void setNumCars(int numCars){
		this.numCars = numCars;
	}

	public int getNumAvail(){
		return numAvail;
	}

	public void setNumAvail(int numAvail){
		this.numAvail = numAvail;
	}

	public String toString(){
    return location+"  "+price+"  "+numCars+"  "+numAvail;
   }

	public Car clone(){
		return new Car(location, price, numCars, numAvail);
	}
}
