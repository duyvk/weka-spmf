package weka.spmf.sequential_rule.topseqrules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of a sequence database. Each sequence should have a unique id.
 * See examples in /test/ directory for the format of input files.
 * 
 * @author Philippe Fournier-Viger
 **/
public class SequenceDatabase {

	public int minItem = Integer.MAX_VALUE;
	public int maxItem = 0;
	public int tidsCount =0;

	// Contexte
	private final List<Sequence> sequences = new ArrayList<Sequence>();


	public void loadFile(String path) throws IOException {
		String thisLine;
		BufferedReader myInput = null;
		try {
			FileInputStream fin = new FileInputStream(new File(path));
			myInput = new BufferedReader(new InputStreamReader(fin));
			while ((thisLine = myInput.readLine()) != null) {
				// si la ligne n'est pas un commentaire
				if (thisLine.charAt(0) != '#') {
					// ajoute une séquence
					addSequence(thisLine.split(" "));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (myInput != null) {
				myInput.close();
			}
		}
	}
	
	public void loadFile(String path, int maxlineCount) throws IOException {
		String thisLine;
		BufferedReader myInput = null;
		try {
			FileInputStream fin = new FileInputStream(new File(path));
			myInput = new BufferedReader(new InputStreamReader(fin));
			int i=0;
			while ((thisLine = myInput.readLine()) != null) {
				// si la ligne n'est pas un commentaire
				if (thisLine.charAt(0) != '#') {
					// ajoute une séquence
					addSequence(thisLine.split(" "));
					i++;
					if(i == maxlineCount){
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (myInput != null) {
				myInput.close();
			}
		}
	}

	public void addSequence(String[] entiers) { //
		Sequence sequence = new Sequence();
		List<Integer> itemset = new ArrayList<Integer>();
		for (String entier : entiers) {
//			System.out.println("--" + entier + "--");
			if (entier.codePointAt(0) == '<') { // Timestamp
			} else if (entier.equals("-1")) { // itemset separator
				sequence.addItemset(itemset.toArray());
				itemset = new ArrayList<Integer>();
			} else if (entier.equals("-2")) { // indicates end of sequence
				sequences.add(sequence);
			} else { 
				Integer item = Integer.parseInt(entier);
				if(item >= maxItem){
					maxItem = item;
				}
				if(item < minItem){
					minItem = item;
				}
				itemset.add(item);
			}
		}
		tidsCount++;
	}

	public void addSequence(Sequence sequence) {
		sequences.add(sequence);
	}

	public void printContext() {
		System.out.println("============  CONTEXTE ==========");
		for (int i=0 ; i < sequences.size(); i++) { // pour chaque objet
			System.out.print(i + ":  ");
			sequences.get(i).print();
			System.out.println("");
		}
	}
	
	public void printDatabaseStats() {
		System.out.println("============  STATS ==========");
		System.out.println("Number of sequences : " + sequences.size());
		System.out.println("Min item:" + minItem);
		System.out.println("Max item:" + maxItem);
		// average size of sequence
		long size = 0;
		for(Sequence sequence : sequences){
			size += sequence.size();
		}
		double meansize = ((float)size) / ((float)sequences.size());
		System.out.println("mean size" + meansize);
	}
//	
//	public void printDatabaseStats() {
//		System.out.println("============  STATS ==========");
//		System.out.println("Number of sequences : " + sequences.size());
//		System.out.println("Min item:" + minItem);
//		System.out.println("Max item:" + maxItem);
//		// average size of sequence
//		long size = 0;
//		long sizeItems = 0;
//		ArrayList<Integer> listItems = new ArrayList<Integer>();
//		for(Sequence sequence : sequences){
//			int itemCount = 0;
//			for(Integer[] array : sequence.getItemsets()){
//				itemCount += array.length;
//			}
//			sizeItems += itemCount;
//			listItems.add(itemCount);
//			size += sequence.size();
//		}
//		double meansizeItems = ((float)sizeItems) / ((float)sequences.size());
//		// standard deviation
//		double std =0;
//		for(Integer elementList : listItems){
//			std += Math.abs((meansizeItems - elementList) / ((float)sequences.size()));
//		}
//		std = Math.sqrt(std);
//		
//		System.out.println("mean item count" + meansizeItems + " std : " + std);
////		System.out.println("mean itemset count" + meansize);
//	}

	@Override
	public String toString() {
		StringBuffer r = new StringBuffer();
		for (int i=0 ; i < sequences.size(); i++) { // pour chaque objet
			r.append(i);
			r.append(":  ");
			r.append(sequences.get(i).toString());
			r.append('\n');
		}
		return r.toString();
	}

	public int size() {
		return sequences.size();
	}

	public List<Sequence> getSequences() {
		return sequences;
	}

	
}
