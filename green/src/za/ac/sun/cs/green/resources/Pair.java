package za.ac.sun.cs.green.resources;

import java.io.Serializable;

public class Pair <T,L> implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1218406282501581095L;
	public final T first;
	public final L second;
	
	public Pair(T first, L second){
		this.first = first;
		this.second = second;
	}
	
	@Override
	public String toString(){
		return first.toString()+", "+second.toString();
	}
	
	@Override
	public int hashCode(){
		return first.hashCode() + second.hashCode();
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof Pair){
			return first.equals(((Pair) other).first) && second.equals(((Pair)other).second);
		}
		return false;
	}
}
