package weka.spmf.sequentialpattern.prefixspan_with_strings;



/**
 * This class represents a sequence from a projected database (as based in PrefixSpan).
 * Since it is a projected sequence, it makes reference to the original sequence.
 * 
 * This class also include several methods for calculating the maximum periods, 
 * semi-maximum perdiods, etc. as required by the BIDE+ algorithm.
 * @author Philippe
 */

public class PseudoSequence {

	private Sequence sequence;
	private int firstItemset;
	private int firstItem;
	
	public PseudoSequence(PseudoSequence sequence, int indexItemset, int indexItem){

		this.sequence = sequence.sequence;
		this.firstItemset = indexItemset + sequence.firstItemset;
		if(this.firstItemset == sequence.firstItemset){
			this.firstItem = indexItem + sequence.firstItem;
		}else{
			this.firstItem = indexItem; // ?????????? NÉCESSAIRE??
		}
	}
	
	public PseudoSequence(PseudoSequence sequence, int indexItemset, int indexItem, int lastItemset, int lastItem){
		this.sequence = sequence.sequence;
		this.firstItemset = indexItemset + sequence.firstItemset;
		if(this.firstItemset == sequence.firstItemset){
			this.firstItem = indexItem + sequence.firstItem;
		}else{
			this.firstItem = indexItem; // ?????????? necessary??
		}
	}
	
	public  PseudoSequence(Sequence sequence, int indexItemset, int indexItem){
		this.sequence = sequence;
		this.firstItemset = indexItemset;
		this.firstItem = indexItem;
	}
	
	public int size(){
		int size = sequence.size() - firstItemset ;
		if(size == 1 && sequence.getItemsets().get(firstItemset).size() == 0){
			return 0;
		}
		return size;
	}
	
	public int getSizeOfItemsetAt(int index){
		int size = sequence.getItemsets().get(index + firstItemset).size();

		if(isFirstItemset(index)){
			size -=  firstItem;
		}
		return size;
	}

	
	//	return true if this itemset is cut at its left.
	public boolean isPostfix(int indexItemset){
		return indexItemset == 0  && firstItem !=0;
	}
	
	public boolean isFirstItemset(int index){
		return index == 0;
	}
	
	
	public String getItemAtInItemsetAt(int indexItem, int indexItemset){
//		if((firstItemset + indexItemset) > lastItemset){// Protection
//			throw new RuntimeException("Out of bound itemset!");
//		}
//		if(isLastItemset(indexItemset)){// Protection
//			if(isFirstItemset(indexItemset) && (firstItem + indexItem) > lastItem){
//				throw new RuntimeException("Out of bound item!");
//			}else if (indexItem > lastItem){
//				throw new RuntimeException("Out of bound item!");
//			}
//		}
		if(isFirstItemset(indexItemset)){
			return getItemset(indexItemset).get(indexItem + firstItem);
		}else{
			return getItemset(indexItemset).get(indexItem);
		}
	}

	
	private Itemset getItemset(int index){
		return sequence.get(index+firstItemset);
	}

	public int getId() {
		return sequence.getId();
	}

	public void print() {
		System.out.print(toString());
	}
	
//	public String toString(){
//		StringBuffer r = new StringBuffer();
//		for(int i=0; i < size(); i++){
//			for(int j=0; j < getSizeOfItemsetAt(i); j++){
//				if(!isLastItemset(i) || (j <= lastItem)){
//					r.append(getItemAtInItemsetAt(j, i).toString());
//					if(isPostfix(i)){
//						r.append('*');
//					}
//					r.append(' ');
//				}
//			}
//			r.append("}");
//		}
//		r.append("  ");
//		return r.toString();
//	}

	public int indexOf(int indexItemset, String idItem) {
		for(int i=0; i < getSizeOfItemsetAt(indexItemset); i++){
			if(getItemAtInItemsetAt(i, indexItemset).equals(idItem)){
				return i;
			}
		}
		return -1;
	}
	

	
}