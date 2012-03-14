package ca.pfv.spmf.sequential_rules.topseqrules;
 
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ca.pfv.spmf.general.datastructures.redblacktree.RedBlackTree;

/**
 * Top-K-SeqRules: a modified version of RuleGrowth for mining the top-k sequential rules.
 * This code is copyright by Philippe Fournier-Viger.
 * See the SPMF website for the full license agreement for using this code.
 * http://www.philippe-fournier-viger.com/spmf/
 * 
 * @author Philippe Fournier-Viger, 2010
 */
public class AlgoTopSeqRules {
	long timeStart = 0;
	long timeEnd = 0;
	
	double minConfidence; 
	int minsuppRelative;
	int k=0;
	SequenceDatabase database;
	
	RedBlackTree<Rule> kRules;  // the top k rules found until now 
	RedBlackTree<Rule> candidates;  // the candidates for expansion
	
	double maxMemory = 0;
	int maxCandidateCount = 0;
	
	Map<Integer, Short>  arrayMapItemCountFirst[];  // item, <tid, occurence>
	Map<Integer, Short>  arrayMapItemCountLast[];  // item, <tid, occurence>

	public AlgoTopSeqRules() {
	}

	private void checkMemory() {
		double currentMemory = ( (double)((double)(Runtime.getRuntime().totalMemory()/1024)/1024))- ((double)((double)(Runtime.getRuntime().freeMemory()/1024)/1024));
		if(currentMemory > maxMemory){
			maxMemory = currentMemory;
		}
	}
	
	public RedBlackTree runAlgorithm(int k, SequenceDatabase database, double minConfidence) {
		this.database = database;
		this.minConfidence = minConfidence;
		this.maxMemory = 0;
		this.maxCandidateCount = 0;
		this.k = k;
		
		this.minsuppRelative = 1;
		
		arrayMapItemCountFirst = new Map[database.maxItem+1];
		arrayMapItemCountLast = new Map[database.maxItem+1];
		
		kRules = new RedBlackTree<Rule>();
		candidates = new RedBlackTree<Rule>();

		timeStart = System.currentTimeMillis(); // for stats
		scanDatabase(database);	
		start();
		timeEnd = System.currentTimeMillis(); // for stats
		
		return kRules;
	}

	private void start() {		
		// FOR EACH FREQUENT ITEM WE COMPARE WITH OTHER FREQUENT ITEM TO 
		// TRY TO GENERATE A CANDIDATE RULE 1-1.
main1:	for(int itemI=database.minItem; itemI<= database.maxItem; itemI++){
			Map<Integer, Short> occurencesIfirst = arrayMapItemCountFirst[itemI];
			
			if(occurencesIfirst == null){
				continue main1;
			}
			Set<Integer> tidsI = occurencesIfirst.keySet();
			if(tidsI.size() < minsuppRelative){
				continue main1;
			}
			
main2:		for(int itemJ=itemI+1; itemJ <= database.maxItem; itemJ++){
				Map<Integer, Short> occurencesJfirst = (Map<Integer, Short>) arrayMapItemCountFirst[itemJ];
				if(occurencesJfirst == null){
					continue main2;
				}
				Set<Integer> tidsJ = occurencesJfirst.keySet();
				if(tidsJ.size() < minsuppRelative){
					continue main2;
				}
				
				// (1) Build list of common  tids  and count occurences 
				// of i before j  and  j before i.
				Set<Integer> tidsIJ = new HashSet<Integer>();
				Set<Integer> tidsJI = new HashSet<Integer>();

				Map<Integer, Short> occurencesJlast = (Map<Integer, Short>) arrayMapItemCountLast[itemJ];
				Map<Integer, Short> occurencesIlast = arrayMapItemCountLast[itemI];

				if(tidsI.size() > tidsJ.size()){ 
					int left = tidsJ.size();
					for(Integer tid : occurencesJfirst.keySet()){
						Short occIFirst = occurencesIfirst.get(tid);
						if(occIFirst !=  null){
							Short occJFirst = occurencesJfirst.get(tid);
							Short occJLast = occurencesJlast.get(tid);
							if(occIFirst < occJLast){
								tidsIJ.add(tid);
							}
							Short occILast = occurencesIlast.get(tid);
							if(occJFirst < occILast){
								tidsJI.add(tid);
							}
						}
						left--;
						if(((left + tidsIJ.size()) < minsuppRelative) && 
								((left + tidsJI.size()) < minsuppRelative)){
							continue main2;
						}
					}
				}else{
					int left = tidsI.size();
					for(Integer tid : occurencesIfirst.keySet()){
						Short occJFirst = occurencesJfirst.get(tid);
						if(occJFirst !=  null){
							Short occIFirst = occurencesIfirst.get(tid);
							Short occILast = occurencesIlast.get(tid);
							if(occJFirst < occILast){
								tidsJI.add(tid);
							}
							Short occJLast = occurencesJlast.get(tid);
							if(occIFirst < occJLast){
								tidsIJ.add(tid);
							}
						}
						left--;
						if(((left + tidsIJ.size()) < minsuppRelative) && 
								((left + tidsJI.size()) < minsuppRelative)){
							continue main2;
						}
					}
				}
				
				// (2) check if the two itemsets have enough common tids
				// if not, we don't need to generate a rule for them.
				// create rule IJ
				int supIJ = tidsIJ.size();
				if(supIJ >= minsuppRelative){
					double confIJ = ((double)tidsIJ.size()) / occurencesIfirst.size();
					int[] itemsetI = new int[1];
					itemsetI[0]= itemI;
					int[] itemsetJ = new int[1];
					itemsetJ[0]= itemJ;
					
					Rule ruleIJ = new Rule(itemsetI, itemsetJ, confIJ, supIJ, tidsI, tidsJ, tidsIJ, occurencesIfirst, occurencesJlast);
					if(confIJ >= minConfidence){
						save(ruleIJ, supIJ); 
					}
					registerAsCandidate(true, ruleIJ);
				}

				int supJI = tidsJI.size();
				// create rule JI
				if(supJI >= minsuppRelative){
					int[] itemsetI = new int[1];
					itemsetI[0]= itemJ;
					int[] itemsetJ = new int[1];
					itemsetJ[0]= itemI;
					double confJI = ((double)tidsJI.size()) / occurencesJfirst.size();
					Rule ruleJI = new Rule(itemsetJ, itemsetI, confJI, supJI, tidsJ, tidsI, tidsJI, occurencesJfirst, occurencesIlast);
					if(confJI >= minConfidence){
						save(ruleJI, supJI);
					}
					registerAsCandidate(true, ruleJI);
				}
			}
		}
		
		// RECURSIVELY EXPAND ALL CANDIDATES, THE MOST PROMISING CANDIDATES FIRST...
		while(!candidates.isEmpty()){
			Rule rule = candidates.popMaximum();
			// if there is no more candidates with enough support, then we stop
			if(rule.getAbsoluteSupport() < minsuppRelative){
				break;
			}
			if(rule.expandLR){
				expandL(rule);
				expandR(rule);
			}else{
				expandL(rule);
			}
		}
	}

	private void save(Rule rule, int support) {
		kRules.add(rule);
		if(kRules.size() > k ){
			if(support > this.minsuppRelative ){
				Rule lower;
				do{
					lower = kRules.lower(new Rule(null, null, 0, this.minsuppRelative+1, null, null, null, null, null));
					if(lower == null){
						break;  /// IMPORTANT
					}
					kRules.remove(lower);
				}while(kRules.size() > k);
			}
			this.minsuppRelative = kRules.minimum().getAbsoluteSupport();
		}
//		System.out.println(this.minsuppRelative);
	}
	
	private void registerAsCandidate(boolean expandLR, Rule ruleLR) {
		ruleLR.expandLR = expandLR;
		
		candidates.add(ruleLR);
		if(candidates.size() >= maxCandidateCount){
			maxCandidateCount = candidates.size();
		}
		checkMemory();
	}

	/**
	 * This method search for items for expanding left side of a rule I --> J 
	 * with any item c. This results in rules of the form I U {c} --> J. The method makes sure that:
	 *   - c  is not already included in I or J
	 *   - c appear at least minsup time in tidsIJ before last occurence of J
	 *   - c is lexically bigger than all items in I
	 */
    private void expandL(Rule rule ) {    	
    	// map-key: item   map-value: set of tids containing the item
    	Map<Integer, Set<Integer>> frequentItemsC  = new HashMap<Integer, Set<Integer>>();  
    	
    	// we scan the sequence where I-->J appear to search for items c that we could add.
    	// for each sequence containing I-->J
    	int left = rule.tidsIJ.size();
    	for(Integer tid : rule.tidsIJ){
    		Sequence sequence = database.getSequences().get(tid);
			Short end = rule.occurencesJlast.get(tid);
			
			// for each itemset before the last occurence of J
itemLoop:	for(int k=0; k < end; k++){
				Integer[] itemset = sequence.get(k);
				// for each item
				for(int m=0; m< itemset.length; m++){
					Integer itemC = itemset[m];
					// if lexical order is not respected or c is included in the rule already.			
					if(containsLEXPlus(rule.getItemset1(), itemC) ||  containsLEX(rule.getItemset2(), itemC)){
						continue;
					}
						
					Set<Integer> tidsItemC = frequentItemsC.get(itemC);
					if(tidsItemC == null){ 
						if(left < minsuppRelative){
							continue itemLoop;
						}	
					}else if(tidsItemC.size() + left < minsuppRelative){
						tidsItemC.remove(itemC);
						continue itemLoop;
					}
					if(tidsItemC == null){
						tidsItemC = new HashSet<Integer>(rule.tidsIJ.size());
						frequentItemsC.put(itemC, tidsItemC);
					}
					tidsItemC.add(tid);			
				}
			}
    		left--;
		}
    	
     	// for each item c found, we create a rule	 	
    	for(Entry<Integer, Set<Integer>> entry : frequentItemsC.entrySet()){
    		Set<Integer> tidsIC_J = entry.getValue();
    		
    		// if the support is enough      Sup(R)  =  sup(IC -->J)
    		if(tidsIC_J.size() >= minsuppRelative){ 
        		Integer itemC = entry.getKey();
        		
    			// Calculate tids containing IC
    			Set<Integer> tidsIC = new HashSet<Integer>(rule.tidsI.size());
    	    	for(Integer tid: rule.tidsI){
    	    		if(arrayMapItemCountFirst[itemC].containsKey(tid)){
    	    			tidsIC.add(tid);
    	    		}
    	    	}
    			
    			// Create a rule and calculate its confidence:  Conf(r) = sup(IUC -->J) /  sup(IUC)			
				double confIC_J = ((double)tidsIC_J.size()) / tidsIC.size();
				int [] itemsetIC = new int[rule.getItemset1().length+1];
				System.arraycopy(rule.getItemset1(), 0, itemsetIC, 0, rule.getItemset1().length);
				itemsetIC[rule.getItemset1().length] = itemC;
				
				// if confidence is enough
				Rule candidate = new Rule(itemsetIC,rule.getItemset2(), confIC_J, tidsIC_J.size(), tidsIC, null, tidsIC_J, null, rule.occurencesJlast);
				if(confIC_J >= minConfidence){
					save(candidate, tidsIC_J.size());
				}
				registerAsCandidate(false, candidate); 
    		}
    	}
    	checkMemory();
	}
    
	/**
	 * This method search for items for expanding left side of a rule I --> J 
	 * with any item c. This results in rules of the form I --> J U {c}. The method makes sure that:
	 *   - c  is not already included in I or J
	 *   - c appear at least minsup time in tidsIJ after the first occurence of I
	 *   - c is lexically bigger than all items in J
	 */
    private void expandR(Rule rule) {
	
    	// map-key: item   map-value: set of tids containing the item
    	Map<Integer, Set<Integer>> frequentItemsC  = new HashMap<Integer, Set<Integer>>();  
    	
    	// we scan the sequence where I-->J appear to search for items c that we could add.
    	// for each sequence containing I-->J.
    	int left =rule.tidsIJ.size();
    	for(Integer tid : rule.tidsIJ){
    		Sequence sequence = database.getSequences().get(tid);
			Short first = rule.occurencesIfirst.get(tid);
			
			// for each itemset after the first occurence of I
			for(int k=first+1; k < sequence.size(); k++){
				Integer[] itemset = sequence.get(k);
				// for each item
	itemLoop:	for(int m=0; m< itemset.length; m++){
					Integer itemC = itemset[m];
					// if lexical order is not respected or c is included in the rule already.			
					if(containsLEX(rule.getItemset1(), itemC) ||  containsLEXPlus(rule.getItemset2(), itemC)){
						continue;
					}
					
					Set<Integer> tidsItemC = frequentItemsC.get(itemC);
					if(tidsItemC == null){ 
						if(left < minsuppRelative){
							continue itemLoop;
						}	
					}else if(tidsItemC.size() + left < minsuppRelative){
						tidsItemC.remove(itemC);
						continue itemLoop;
					}
					if(tidsItemC == null){
						tidsItemC = new HashSet<Integer>(rule.tidsIJ.size());
						frequentItemsC.put(itemC, tidsItemC);
					}
					tidsItemC.add(tid);		
				}
			}
			left--;
		}
    	
    	// for each item c found, we create a rule	 	
    	for(Entry<Integer, Set<Integer>> entry : frequentItemsC.entrySet()){
    		Set<Integer> tidsI_JC = entry.getValue();
    		
    		// if the support is enough      Sup(r)  =  sup(I -->JUC)
    		if(tidsI_JC.size() >= minsuppRelative){  
        		Integer itemC = entry.getKey();
        		
    			Set<Integer> tidsJC = new HashSet<Integer>(rule.tidsJ.size());
    			Map<Integer, Short> occurencesJC = new HashMap<Integer, Short>();
    			
    			for(Integer tid: rule.tidsJ){
    				Short occurenceCLast = arrayMapItemCountLast[itemC].get(tid);
    	    		if(occurenceCLast != null){
    	    			tidsJC.add(tid);
    	    			Short occurenceJlast = rule.occurencesJlast.get(tid);
    	    			if(occurenceCLast < occurenceJlast){
    	    				occurencesJC.put(tid, occurenceCLast);
    	    			}else{
    	    				occurencesJC.put(tid, occurenceJlast);
    	    			}
    	    		}
    	    	}
    			// Create rule and calculate its confidence:  Conf(r) = sup(I-->JC) /  sup(I)	
				double confI_JC = ((double)tidsI_JC.size()) / rule.tidsI.size();
				int[] itemsetJC = new int[rule.getItemset2().length+1];
				System.arraycopy(rule.getItemset2(), 0, itemsetJC, 0, rule.getItemset2().length);
				itemsetJC[rule.getItemset2().length]= itemC;
				
				// if the confidence is enough
				Rule candidate = new Rule(rule.getItemset1(), itemsetJC, confI_JC, tidsI_JC.size(), 
						                  rule.tidsI, tidsJC, tidsI_JC, rule.occurencesIfirst, occurencesJC);
				if(confI_JC >= minConfidence){
					save(candidate, tidsI_JC.size());
				}
				registerAsCandidate(true, candidate); 
    		}
    	}
    	checkMemory();
	}

    
	/**
	 * This method calculate the frequency of each item in one database pass.
	 * @param database : a sequence database 
	 * @return A map such that key = item
	 *                         value = a map  where a key = tid  and a value = Occurence
	 * This map allows knowing the frequency of each item and their first and last occurence in each sequence.
	 */
	private void scanDatabase(SequenceDatabase database) {
		// (1) Count the support of each item in the database in one database pass
		// for each sequence
		for(int tid=0; tid< database.size(); tid++){
			Sequence sequence = database.getSequences().get(tid);
			// for each itemset
			for(short j=0; j< sequence.getItemsets().size(); j++){
				Integer[] itemset = sequence.get(j);
				// for each item
				for(int i=0; i<itemset.length; i++ ){
					Integer itemI = itemset[i];
					if(arrayMapItemCountFirst[itemI] == null){
						arrayMapItemCountFirst[itemI] =  new HashMap<Integer, Short>();
						arrayMapItemCountLast[itemI] = new HashMap<Integer, Short>();
					}
					Short oldPosition = arrayMapItemCountFirst[itemI].get(tid);
					if(oldPosition == null){
						arrayMapItemCountFirst[itemI].put(tid, j);
						arrayMapItemCountLast[itemI].put(tid, j);
					}else{
						arrayMapItemCountLast[itemI].put(tid, j);
					}
				}
			}
		}
	}

	/**
	 * 
	 * This method checks if the item "item" is in the itemset.
	 * It asumes that items in the itemset are sorted in lexical order
	 * This version also checks that if the item "item" was added it would be the largest one
	 * according to the lexical order
	 */
	public boolean containsLEXPlus(int[] itemset, int item) {
		for(int i=0; i< itemset.length; i++){
			if(itemset[i] == item){
				return true;
			}else if(itemset[i] > item){
				return true; // <-- xxxx
			}
		}
		return false;
	}
	
	/**
	 * This method checks if the item "item" is in the itemset.
	 * It asumes that items in the itemset are sorted in lexical order
	 * @param item
	 * @return
	 */
	public boolean containsLEX(int[] itemset, int item) {
		for(int i=0; i< itemset.length; i++){
			if(itemset[i] == item){
				return true;
			}else if(itemset[i] > item){
				return false;  // <-- xxxx
			}
		}
		return false;
	}
	
	public void printStats() {
		System.out.println("=============  TOPSEQRULES - STATS ========");
		System.out.println("Max candidates: " + maxCandidateCount);
		System.out.println("Sequential rules count: " + kRules.size());
		System.out.println("-");
		System.out.println("Total time: " + (((double)(timeEnd - timeStart))/1000d) + " s");
		System.out.println("Max memory: " + maxMemory);
		System.out.println("Minsup relative: " + minsuppRelative);
		System.out.println("==========================================");
	}

	public double getTotalTime(){
		return timeEnd - timeStart;
	}

	public void writeResultTofile(String path) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(path)); 
		Iterator<Rule> iter = kRules.iterator();
		while (iter.hasNext()) {
			Rule rule = (Rule) iter.next();
			StringBuffer buffer = new StringBuffer();
			buffer.append(rule.toString());
			// write separator
			buffer.append("  sup= ");
			// write support
			buffer.append(rule.getAbsoluteSupport());
			// write separator
			buffer.append("  conf= ");
			// write confidence
			buffer.append(rule.getConfidence());
			writer.write(buffer.toString());
			writer.newLine();
		}
		
		writer.close();
	}

}
