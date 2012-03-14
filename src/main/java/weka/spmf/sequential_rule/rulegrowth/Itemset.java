package weka.spmf.sequential_rule.rulegrowth;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an itemset (a set of items)
 * @author Philippe Fournier-Viger 
 */
public class Itemset{
	private final List<Integer> items = new ArrayList<Integer>(); // ordered
	
	public Itemset(){
	}
	
	public Itemset(Integer item){
		items.add(item);
	}

	public Itemset(Itemset itemset){
		items.addAll(itemset.getItems());
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

}
