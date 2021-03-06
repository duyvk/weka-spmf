package weka.spmf.sequentialpattern.prefixspan;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class represents an itemset from a sequence from a sequence database.
 * The itemset can thus have a timestamp.
 * @author Philippe Fournier-Viger 
 */
public class Itemset{

	private final List<Integer> items = new ArrayList<Integer>(); // ordered list.
	
	public Itemset(Integer item){
		addItem(item);
	}
	
	public Itemset(){
	}

	public void addItem(Integer value){
			items.add(value);
	}
	
	public List<Integer> getItems(){
		return items;
	}
	
	public Integer get(int index){
		return items.get(index);
	}
	
	public void print(){
		System.out.print(toString());
	}
	
	@Override
	public String toString(){
		StringBuffer r = new StringBuffer ();
		for(Integer attribute : items){
			r.append(attribute.toString());
			r.append(' ');
		}
		return r.toString();
	}

	
	public int size(){
		return items.size();
	}
	
	public Itemset cloneItemSetMinusItems(Map<Integer, Set<Integer>> mapSequenceID, double minsuppRelatif) {
		Itemset itemset = new Itemset();
		for(Integer item : items){
			if(mapSequenceID.get(item).size() >= minsuppRelatif){
				itemset.addItem(item);
			}
		}
		return itemset;
	}
	
	public Itemset cloneItemSet(){
		Itemset itemset = new Itemset();
		itemset.getItems().addAll(items);
		return itemset;
	}
	
	public boolean containsAll(Itemset itemset2){
		return items.containsAll(itemset2.items);
	}
}
