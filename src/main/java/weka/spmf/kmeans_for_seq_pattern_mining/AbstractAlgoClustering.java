package weka.spmf.kmeans_for_seq_pattern_mining;

import java.util.List;

import weka.spmf.sequentialpattern.ItemValued;

public abstract class AbstractAlgoClustering {
	
	public abstract List<Cluster> runAlgorithm(List<ItemValued> items);

}
