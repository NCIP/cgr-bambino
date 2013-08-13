package TCGA;

import java.util.*;

public abstract class Cluster implements Comparator {
  public static final double UNDEF_DISTANCE = -100;
  // undefined/uncomputable/null distance value
  // - for Pearson's r, values are -1 to +1
  // - for Euclidean distance, values are always positive

  public static final int DISTANCE_METHOD_COMPLETE = 1;
  public static final int DISTANCE_METHOD_MEAN = 2;
  public static final int DISTANCE_METHOD_MINIMUM = 3;

  private int[] ids = null;
  private ArrayList<GenomicSample> sample_data = null;
  private double internal_distance;

  public abstract String get_method_name();
  // name of clustering method
  
  public abstract boolean are_higher_distance_values_closer();
  // for Pearson's r, a higher r means the two samples are closer
  // for Euclidean distance, a higher value means the two samples are further apart

  public abstract double compute_single_distance(byte[] sample1, byte[] sample2) throws ArithmeticException;
  // compute distance between two samples

  public abstract Cluster create_new_cluster();
  public abstract double get_default_cluster_stop_threshold();
  // FIX ME: move abstract methods to an interface??

  public abstract void precluster(HashSet<Cluster> clusters, ArrayList<GenomicSample> rows);

  public abstract boolean validate_distance_matrix_cache();

  public int[] get_ids() {
    return ids;
  }

  public void set_internal_distance(double d) {
    internal_distance=d;
  }

  public double get_internal_distance() {
    return internal_distance;
  }
  
  public void add (int id) {
    // add a single ID to this cluster
    int current_size = ids == null ? 0 : ids.length;
    int next_size = current_size + 1;
    int[] ids_new = new int[next_size];
    if (current_size > 0) System.arraycopy(ids, 0, ids_new, 0, current_size);
    ids_new[current_size] = id;
    ids = ids_new;
  }

  public void addAll (Cluster other) {
    // fold another cluster into this one
    int[] other_ids = other.get_ids();
    int current_size = ids == null ? 0 : ids.length;
    int next_size = current_size + other_ids.length;
    int[] ids_new = new int[next_size];
    if (current_size > 0) System.arraycopy(ids, 0, ids_new, 0, current_size);
    System.arraycopy(other_ids, 0, ids_new, current_size, other_ids.length);
    ids = ids_new;
  }

  private void dump_array(String msg, int[] array) {
    System.err.println(msg);  // debug
    if (array == null) {
      System.err.println("  null");  // debug
    } else {
      for (int i=0; i < array.length; i++) {
	System.err.println("  " + array[i]);  // debug
      }
    }
  }

  public int size () {
    return ids.length;
  }

  public double compare_cluster (Cluster other, double[][] distance_matrix, int distance_metric) {
    //    System.err.println("compare " + this + " vs " + other);  // debug

    int[] other_ids = other.get_ids();
    double result = UNDEF_DISTANCE;

    if (ids.length == 1 && other_ids.length == 1) {
      result = distance_matrix[ids[0]][other_ids[0]];
    } else {
      //      System.err.println("multi comparison " + ids.size() + " " + other_ids.size());  // debug
      int usable=0;
      int i,j,x,y;
      double distance;

      if (distance_metric == DISTANCE_METHOD_MEAN) {
	// compute average distance between clusters
	double usable_distances = 0;
	for (i = 0; i < ids.length; i++) {
	  x = ids[i];
	  for (j = 0; j < other_ids.length; j++) {
	    y = other_ids[j];
	    distance = distance_matrix[x][y];
	    if (distance != UNDEF_DISTANCE) {
	      usable++;
	      usable_distances += distance;
	      //	    System.err.println("  " + x + " vs " + y + " avg=" + r);  // debug
	    }
	  }
	}
	result = usable == 0 ? UNDEF_DISTANCE : usable_distances / usable;
      } else if (distance_metric == DISTANCE_METHOD_COMPLETE) {
	// Comparator is the maximum distance between elements of each cluster.
	//
	// Since higher r scores indicate lower distance,
	// the lowest observed r score indicates the most distant relationship
	// between elements of the two clusters.
	result = are_higher_distance_values_closer() ? find_lowest_distance(other_ids, distance_matrix) :
	  find_highest_distance(other_ids, distance_matrix);
      } else if (distance_metric == DISTANCE_METHOD_MINIMUM) {
	// Comparator is the minimum distance between elements of each cluster.
	//
	// Since higher r scores indicate lower distance,
	// the highest observed r score indicates the closest relationship
	// between elements of the two clusters.
	result = are_higher_distance_values_closer() ? find_highest_distance(other_ids, distance_matrix) :
	  find_lowest_distance(other_ids, distance_matrix);
      } else {
	System.err.println("ERROR: unknown distance method!");  // debug
      }
    }
    return result;
  }

  // begin Comparator interface
  public int compare (Object x, Object y) {
    //
    // for sorting: compare by size only
    //
    int xs = ((Cluster) x).size();
    int ys = ((Cluster) y).size();
    if (xs == ys) {
      return 0;
    } else if (xs < ys) {
      return -1;
    } else {
      return 1;
    }
  }

  public boolean equals (Object x, Object y) {
    // same size only
    return ((Cluster) x).size() == ((Cluster) y).size();
  }
  // end Comparator interface


  private double find_lowest_distance (int[] other_ids, double[][] distance_matrix) {
    double distance;
    int i,j,x,y;
    int usable=0;
    double result = UNDEF_DISTANCE;
    // will be initialized later
    for (i = 0; i < ids.length; i++) {
      x = ids[i];
      for (j = 0; j < other_ids.length; j++) {
	y = other_ids[j];
	distance = distance_matrix[x][y];
	if (distance != UNDEF_DISTANCE) {
	  if (usable++ == 0) result = distance;
	  // first usable score: init result
	  if (distance < result) result = distance;
	  // result is the lowest observed r
	}
      }
    }
    return result;
  }

  private double find_highest_distance (int[] other_ids, double[][] distance_matrix) {
    double distance;
    int i,j,x,y;
    int usable=0;
    double result = UNDEF_DISTANCE;
    // will be initialized later
    for (i = 0; i < ids.length; i++) {
      x = ids[i];
      for (j = 0; j < other_ids.length; j++) {
	y = other_ids[j];
	distance = distance_matrix[x][y];
	if (distance != UNDEF_DISTANCE) {
	  if (usable++ == 0) result = distance;
	  // first usable score: init result
	  if (distance > result) result = distance;
	  // result is the highest observed r
	}
      }
    }
    return result;
  }

  public ArrayList<GenomicSample> get_sample_data() {
    return sample_data;
  }

  public void set_sample_data(ArrayList<GenomicSample> sample_data) {
    //    System.err.println("set data for " + this + " to "+sample_data);  // debug
    this.sample_data = sample_data;
  }

  public static String describe_distance_method (int method) {
    switch (method) {
    case DISTANCE_METHOD_COMPLETE: return "Complete";
    case DISTANCE_METHOD_MEAN: return "Mean";
    case DISTANCE_METHOD_MINIMUM: return "Minimum";
    default: return null;
    }
  }


}
