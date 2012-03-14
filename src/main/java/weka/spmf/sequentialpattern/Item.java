package weka.spmf.sequentialpattern;


public class Item{
	
	private final int id;
	
	public Item(int id){
		this.id = id;
	}

	public int getId() {
		return id;
	}

	@Override
	public String toString(){
		return "" + getId();
	}
	
	@Override
	public boolean equals(Object object){
		Item item = (Item) object;
		if((item.getId() == this.getId())){
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		String string = ""+getId(); // This could be improved.
		return string.hashCode();
	}
	
}
