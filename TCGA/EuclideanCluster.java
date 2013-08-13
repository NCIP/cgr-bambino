package TCGA;

import java.util.*;

public class EuclideanCluster extends Cluster implements Comparator {
  private static Byte last_euclidean_limit = null;
  // vile

  public String get_method_name() {
    return "Euclidean distance";
  }

  public boolean are_higher_distance_values_closer() {
    // for Euclidean distances, a higher distance value means the two samples are further apart
    return false;
  }

  public double compute_single_distance(byte[] sample1, byte[] sample2) throws ArithmeticException {
    // compute distance between two samples using Euclidean distance
    //    System.err.println("limit="+EuclideanClusterControl.EUCLIDEAN_LIMIT_DISTANCE);
    return Statistics.euclidean_distance(sample1, sample2,
					 true, (byte) GenomicSample.NULL_VALUE,
					 EuclideanClusterControl.EUCLIDEAN_LIMIT_DISTANCE
					 );
  }

  public Cluster create_new_cluster() {
    return new EuclideanCluster();
  };

  public double get_default_cluster_stop_threshold() {
    return 0;
  }

  public void precluster(HashSet<Cluster> clusters, ArrayList<GenomicSample> rows) {}
  // not needed for Euclidean clusters

  public boolean validate_distance_matrix_cache() {
    boolean ok = true;

    if (last_euclidean_limit == null) {
      ok = false;
    } else {
      ok = last_euclidean_limit.equals(EuclideanClusterControl.EUCLIDEAN_LIMIT_DISTANCE);
    }
    last_euclidean_limit = Byte.valueOf(EuclideanClusterControl.EUCLIDEAN_LIMIT_DISTANCE);
    return ok;
  }


}
