package weka.spmf.general.datastructures.kdtree;

class KDNode {
	
	double values[];
	int d;
	KDNode above;
	KDNode below;
	
	
	public KDNode(double[] values, int d){
		this.values = values;
		this.d = d;
	}


}
