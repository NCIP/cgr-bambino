package TCGA;

import java.util.*;

public class PearsonRCluster extends Cluster implements Comparator {

  private static final double DEFAULT_MIN_R_TO_CLUSTER = 0.90;
  public static boolean CLUSTER_EMPTY_SEQUENCES = true;
  // hacktacular

  public String get_method_name() {
    return "Pearson's r";
  }

  public boolean are_higher_distance_values_closer() {
    // for Pearson's r, a higher r value means the two samples are closer together
    return true;
  }

  public double compute_single_distance(byte[] sample1, byte[] sample2) throws ArithmeticException {
    // compute distance between two samples using Pearson's r
    return Statistics.pearson_r(sample1, sample2,
				true, (byte) GenomicSample.NULL_VALUE,

				//				true
				false
				// setting this value to "true"
				// assigns an r value of 1.0 to
				// identical samples (for which r
				// value can't be computed).  This was
				// intended to allow empty rows to
				// cluster together.  However it can
				// cause false cluster joins (see
				// ~edmonson/notes/log from
				// 10/20/2008).  Replaced by
				// "cluster empty samples" option.
				);
  }

  public Cluster create_new_cluster() {
    return new PearsonRCluster();
  };

  public double get_default_cluster_stop_threshold() {
    return DEFAULT_MIN_R_TO_CLUSTER;
  }

  public void precluster(HashSet<Cluster> clusters, ArrayList<GenomicSample> rows) {
    // optionally combine entirely-empty clusters together.
    // Pearson's r calculation is undef between any two identical rows,
    // so empty rows will never cluster together.
    if (CLUSTER_EMPTY_SEQUENCES) {
      //      System.err.println("preclustering empty sequences");  // debug
      ArrayList<Cluster> empty_clusters = new ArrayList<Cluster>();

      for (Cluster c : clusters) {
	int[] ids = c.get_ids();
	boolean all_empty = true;
	for (int i = 0; i < ids.length; i++) {
	  GenomicSample gs = rows.get(ids[i]);
	  if (!gs.all_empty) {
	    all_empty = false;
	    break;
	  }
	}
	if (all_empty) empty_clusters.add(c);
      }

      if (empty_clusters.size() > 1) {
	Cluster master = empty_clusters.get(0);
	for (int i = 1; i < empty_clusters.size(); i++) {
	  Cluster sub = empty_clusters.get(i);
	  master.addAll(sub);
	  clusters.remove(sub);
	}
      }
    }
  }

  public boolean validate_distance_matrix_cache() {
    // no special requirements
    return true;
  }

}
