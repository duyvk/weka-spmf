package ca.pfv.spmf.sequential_rules.topseqrules;

import java.util.ArrayList;
import java.util.List;



/**
 * Implementation of a sequence.
 * A sequence is a list of itemsets.
 * @author Philippe Fournier-Viger 
 **/
public class Sequence{
	
	private final List<Integer[]> itemsets = new ArrayList<Integer[]>();

	public Sequence(){

	}

	public void addItemset(Object[] itemset) {
		Integer[] itemsetInt = new Integer[itemset.length];
		System.arraycopy(itemset, 0, itemsetInt, 0, itemset.length);
		itemsets.add(itemsetInt);
	}
	
	public void print() {
		System.out.print(toString());
	}
	
	public String toString() {
		StringBuffer r = new StringBuffer("");
		for(Integer[] itemset : itemsets){
			r.append('(');
			for(int i=0; i< itemset.length; i++){
				String string = itemset[i].toString();
				r.append(string);
				r.append(' ');
			}
			r.append(')');
		}

		return r.append("    ").toString();
	}

	public List<Integer[]> getItemsets() {
		return itemsets;
	}
	
	public Integer[] get(int index) {
		return itemsets.get(index);
	}
	
	public int size(){
		return itemsets.size();
	}
}
