package ca.pfv.spmf.sequentialpatterns.prefixspan_saveToFile;

import java.util.HashSet;
import java.util.Set;

/**
 * This class is used by PrefixSpanItem. It represents
 * a pair of an (1) Item  and the information if it is contained in an itemset that was cut or not. 
 * It is used for calculating the support of item in a database.
 * @author Philippe Fournier-Viger 
 */
class Pair{
	private final boolean postfix; // is cut at left
	private final Integer item;
	
	// List of the its of all the patterns that contains this one.
	private Set<Integer> sequencesID = new HashSet<Integer>();

	
	// for prefixspan
	Pair(boolean postfix, Integer item){
		this.postfix = postfix;
		this.item = item;
	}
	
	public boolean equals(Object object){
		Pair paire = (Pair) object;
		if((paire.postfix == this.postfix) 
				&& (paire.item.equals(this.item))){
			return true;
		}
		return false;
	}
	
	public int hashCode()
	{// Ex: 127333,P,X,1  127333,N,Z,2
		StringBuffer r = new StringBuffer();
		r.append((postfix ? 'P' : 'N')); // the letters here have no meanings. they are just used for the hashcode
		r.append(item);
		return r.toString().hashCode();
	}

	public boolean isPostfix() {
		return postfix;
	}

	public Integer getItem() {
		return item;
	}

	public int getCount() {
		return sequencesID.size();
	}		

	public Set<Integer> getSequencesID() {
		return sequencesID;
	}

	public void setSequencesID(Set<Integer> sequencesID) {
		this.sequencesID = sequencesID;
	}

}