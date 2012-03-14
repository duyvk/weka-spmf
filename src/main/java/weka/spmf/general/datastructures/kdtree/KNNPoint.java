package weka.spmf.general.datastructures.kdtree;

public class KNNPoint implements Comparable<KNNPoint>{
	
	double [] values;
	double distance;
	
	public KNNPoint(double values[], double distance){
		this.values = values;
		this.distance = distance;
	}

	public int compareTo(KNNPoint point2) {
		return (int)(this.distance  - point2.distance);
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("(");
		for(Double element : values ){
			buffer.append(" " + element);
		}
		buffer.append(")");
		return buffer.toString();
	}
	
	public boolean equals(Object point2){
		if(point2 == null){
			return false;
		}
		KNNPoint o2 = (KNNPoint)point2;
		for(int i=0; i < values.length; i++ ){
			if(o2.values[i] != values[i]){
				return false;
			}
		}
		return true;
	}
}
