package weka.spmf.sequentialpattern;

public abstract class AbstractAlgoPrefixSpan {
	
	public abstract Sequences runAlgorithm(SequenceDatabase contexte);
	
	public abstract double getMinSupp();
}
