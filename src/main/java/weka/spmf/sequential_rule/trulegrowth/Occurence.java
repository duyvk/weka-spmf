package weka.spmf.sequential_rule.trulegrowth;

import java.util.ArrayList;
import java.util.List;


public class Occurence {
	public int transactionID =-1;
	public List<Short> occurences = new ArrayList<Short>();
	
	public Occurence(int transactionID){
		this.transactionID = transactionID;
	}
	
	public void add(short occurence){
		occurences.add(occurence);
	}
	
	public short getFirst(){
		return occurences.get(0);
	}
	
	public short getLast(){
		return occurences.get(occurences.size()-1);
	}
	
	@Override
	public boolean equals(Object obj) {
		return ((Occurence)obj).transactionID == transactionID;
	}

	@Override
	public int hashCode() {
		return transactionID;
	}
}
