package weka.spmf.general.datastructures.kdtree;

/**
 *This test show how to use the KDTree structure to find the nearest neighboor to a given point.
 *@author Philippe Fournier-Viger, 2011
 */
public class MainTestKDTree_NearestNeighboor {

	public static void main(String[] args) {
		// create an empty kd tree 
		KDTree tree = new KDTree();
		
		// Use a list of point to create the kd-tree
		double[][] points = new double[6][2];
		points[0] = new double[]{2d,3d};
		points[1] = new double[]{5d,4d};
		points[2] = new double[]{9d,6d};
		points[3] = new double[]{4d,7d};
		points[4] = new double[]{8d,1d};
		points[5] = new double[]{7d,2d};
		
		// insert the points into the tree.
		tree.buildtree(points);
		
		// print the tree for debugging purposes
		System.out.println("\nTREE: \n" + tree.toString() + "  \n\n Number of elements in tree: " + tree.size());
	
		double query [] = new double[]{7.9d,4d};
		double nearestpoint [] = tree.nearest(query);
		System.out.println("The nearest neighboor is: :" + toString(nearestpoint));
		
//		// find the best answer by brute force to verify the result
//		double min = Double.MAX_VALUE;
//		double[] closest = null;
//		for(int i=0; i< points.length; i++){
//			double dist = distance(query, points[i]);
//			if( dist < min){
//				min = dist;
//				closest = points[i];
//			}
//		}		
//		System.out.println(" good answer :" + toString(closest));
//		System.out.println();
//		
//		
	}
	
	public static String toString(double [] values){
		StringBuffer buffer = new StringBuffer();
		for(Double element : values ){
			buffer.append("   " + element);
		}
		return buffer.toString();
	}
	
	private static double distance(double[] node1, double[] node2) {
		double sum = 0;
		for(int i=0; i< node1.length; i++){
			sum +=  Math.pow(node1[i] - node2[i], 2);
		}
		return Math.sqrt(sum);
	}

}
