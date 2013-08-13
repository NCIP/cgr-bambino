package TCGA;

import java.util.*;

// questions:
// - what about clusters with strong NEGATIVE correlation??
//   right now we only focus on positives...

// FIX ME:
//  - SAVE distance_matrix for subsequent invocations??
//    => maybe not: visible subset changes will invalidate this cache...

public class ClusterTool extends Observable implements Runnable,Observer {
  // clustering wrapper

  private GenomicMeasurement gm;
  private double[][] distance_matrix;
  private double cluster_stop_threshold;
  private int distance_method = Cluster.DISTANCE_METHOD_COMPLETE;
  private Cluster control_cluster = null;
  private boolean separate_null_leftovers;
  private boolean report_results;
  private int visible_in_clusters;
  private boolean wants_zoom, matrix_build_cancelled;
  private ArrayList<Cluster> cluster_list_final;
  private ArrayList<GenomicSample> last_rows;

  public ClusterTool (GenomicMeasurement gm) {
    this.gm = gm;
  }

  public void set_distance_method (int method) {
    distance_method = method;
  }

  public int get_distance_method() {
    return distance_method;
  }

  public void set_wants_zoom (boolean z) {
    wants_zoom = z;
  }

  public int get_visible_in_clusters() {
    return visible_in_clusters;
  }

  public boolean wants_zoom () {
    return wants_zoom;
  }

  public void run() {
    do_cluster();
  }

  public void cluster(Cluster control_cluster, boolean separate_null_leftovers, boolean report_results) {
    this.control_cluster = control_cluster;
    this.separate_null_leftovers = separate_null_leftovers;
    this.report_results = report_results;
    new Thread(this).start();
  }

  public void set_cluster_stop_threshold (double r) {
    cluster_stop_threshold = r;
  }

  public double get_cluster_stop_threshold () {
    return cluster_stop_threshold;
  }


  private void do_cluster() {
    //    ArrayList<GenomicSample> rows = gm.get_visible_rows();
    ArrayList<GenomicSample> rows = gm.get_visible_rows_raw();
    int rlen = rows.size();
    double r;
    
    boolean use_progress_bar = true;
    // FIX ME: finish!

    boolean SUBSORT = true;
    // FIX ME: user option

    // - create sample "control" cluster of desired type (Pearson's r, Euclidean)
    // - this cluster will:
    //   - build distance matrix (static method)
    //   - control gathering of "best" result
    //   - control clustering cutoff

    int i,j;
    GenomicSample x,y;

    //    System.err.println("rows=" + rows);  // debug

    boolean cache_ok = last_rows != null && rows.equals(last_rows);
    if (!control_cluster.validate_distance_matrix_cache()) cache_ok = false;

    if (cache_ok) {
      System.err.println("distance matrix: cached!");
    } else {
      //      System.err.println("distance matrix: rebuild needed");  // debug

      distance_matrix = new double[rlen][rlen];
      // r scores for every row to every other row
      // because of gaps maybe more memory-efficient to use DoubleHashMap?
      // (hash lookups will be slower though).
      // FIX ME: move to static method of "Cluster" implementation


      //
      //  build distance matrix for all row-to-row comparisons
      //

      long matrix_start_time = System.currentTimeMillis();

      ProgressFrame pf=null;
      matrix_build_cancelled = false;
      if (use_progress_bar) {
	pf = new ProgressFrame("Working...", "Computing distance matrix...", ((rlen * rlen) / 2), true);
	pf.addObserver(this);
      }
      int counter=0;

      MATRIX_START:
      for (i = 0; i < rlen; i++) {
	//      System.err.println("i is " + i + " of " + rlen);  // debug

	x = rows.get(i);
	for (j = i + 1; j < rlen; j++) {

	  if (use_progress_bar) {
	    if (matrix_build_cancelled) break MATRIX_START;
	    pf.setValue(counter++);
	  }

	  try {
	    r = control_cluster.compute_single_distance(x.copynum_data, rows.get(j).copynum_data);
	    // will throw an exception if NaN result
	  } catch (ArithmeticException e) {
	    r = Cluster.UNDEF_DISTANCE;
	  }

	  distance_matrix[i][j] = r;
	  distance_matrix[j][i] = r;
	}
      }


      long matrix_end_time = System.currentTimeMillis();
      //      System.err.println("matrix build time: " + (matrix_end_time - matrix_start_time));  // debug
      //    System.exit(1);

      if (use_progress_bar) {
	pf.setVisible(false);
	if (matrix_build_cancelled) return;
      }
    }

    last_rows = rows;

    long cluster_start_time = System.currentTimeMillis();

    //
    //  initialize clusters:
    //  create a cluster for each sample ID (may be multiple samples per ID)
    //
    HashMap<String,Cluster> id2cluster = new HashMap<String,Cluster>();
    for (i = 0; i < rlen; i++) {
      Integer row = Integer.valueOf(i);
      String patient_id = rows.get(row).patient_id;
      Cluster cluster = id2cluster.get(patient_id);
      if (cluster == null) id2cluster.put(patient_id, cluster = control_cluster.create_new_cluster());
      cluster.add(row);
    }

    HashSet<Cluster> clusters = new HashSet<Cluster>(id2cluster.values());
    control_cluster.precluster(clusters, rows);

    //
    //  - iterate through clusters
    //  - find best comparison between 2 clusters
    //  - combine these closest 2 clusters
    //  - repeat until can't cluster within threshold
    //

    //    System.err.println("FIX ME: NEED REPEATABLE ITERATION ORDER");  // debug

    int csize;

    boolean ALLOW_R_CACHE = true;
    DoubleHashMap r_cache = new DoubleHashMap();

    while ((csize = clusters.size()) > 1) {
      //      System.err.println("clusters left:"+csize);  // debug

      ArrayList<Cluster> clist = new ArrayList<Cluster>(clusters);
      // to create predictable iteration order

      int best_i = 0;
      int best_j = 0;
      double best_distance = Cluster.UNDEF_DISTANCE;
      double this_r;
      Cluster i_cluster,j_cluster;
      Double rc;
      
      for (i=0; i < csize; i++) {
	i_cluster = clist.get(i);

	for (j=i+1; j < csize; j++) {
	  j_cluster = clist.get(j);

	  if (ALLOW_R_CACHE) {
	    rc = (Double) r_cache.get(i_cluster, j_cluster);
	    if (rc != null) {
	      //	    System.err.println("hey now: HIT");  // debug
	      this_r = rc.doubleValue();
	    } else {
	      this_r = i_cluster.compare_cluster(j_cluster, distance_matrix, distance_method);
	      r_cache.put(i_cluster, j_cluster, new Double(this_r));
	    }
	  } else {
	    // no caching
	    this_r = i_cluster.compare_cluster(j_cluster, distance_matrix, distance_method);
	  }

	  if (this_r != Cluster.UNDEF_DISTANCE &&
	      ((control_cluster.are_higher_distance_values_closer() ? this_r > best_distance : this_r < best_distance) ||
	       best_distance == Cluster.UNDEF_DISTANCE)) {
	    best_i = i;
	    best_j = j;
	    best_distance = this_r;
	  }
	}
      }

      if (
	  best_distance != Cluster.UNDEF_DISTANCE &&
	  control_cluster.are_higher_distance_values_closer() ? best_distance >= cluster_stop_threshold : best_distance <= cluster_stop_threshold) {
	Cluster best_x = clist.get(best_i);
	Cluster best_y = clist.get(best_j);

	if (false) {
	  System.err.print("best_distance:"+best_distance + " " );
	  int[] best_x_rows = best_x.get_ids();
	  int[] best_y_rows = best_y.get_ids();
	  System.err.print("  cluster 1: ");  // debug
	  for (int ci=0; ci < best_x_rows.length; ci++) {
	    System.err.print(rows.get(best_x_rows[ci]).sample_id + ", ");
	  }

	  System.err.print("  cluster 2: ");  // debug
	  for (int ci=0; ci < best_y_rows.length; ci++) {
	    System.err.print(rows.get(best_y_rows[ci]).sample_id + ", ");
	  }
	  System.err.println("\n");  // debug


	  //	  + " firsts=" + rows.get(best_x.get_ids()[0]).sample_id + " " + rows.get(best_y.get_ids()[0]).sample_id);
	}
	best_x.addAll(best_y);
	// combine the 2 best-matching clusters together
	best_x.set_internal_distance(best_distance);

	// since we've added best_y to best_x, all cache references 
	// to best_x are now stale; clean
	r_cache.removeAll(best_x);

	clusters.remove(best_y);
	// remove the old cluster from the top list
      } else {
	//	System.err.println("stopping clustering, best_distance is " + best_distance);  // debug
	break;
      }
    }

    //
    //  sort clusters by size, largest first:
    //
    ArrayList<Cluster> cluster_list = new ArrayList<Cluster>(clusters);
    Collections.sort(cluster_list, cluster_list.get(0));
    Collections.reverse(cluster_list);

    //    System.err.println("final cluster count: " + clusters.size());  // debug
    ArrayList<GenomicSample> rows_new = new ArrayList<GenomicSample>();

    HashSet<Integer> dividers = new HashSet<Integer>();

    // FIX ME:
    // user option to add dividers between sample IDs too

    int cluster_i=0;

    cluster_list_final = new ArrayList<Cluster>();

    for (Cluster prc : cluster_list) {
      if (prc.size() > 1) {
	// skip "clusters" of 1 (orphaned ID)
	ArrayList<GenomicSample> cr = new ArrayList<GenomicSample>();
	for (Integer ri : prc.get_ids()) {
	  //	System.err.println(" order:" +ri);  // debug
	  cr.add(rows.get(ri));
	}

	HashSet<String> patient_ids = GenomicSample.get_patient_ids(cr);

	//	ArrayList<Integer> members = new ArrayList<Integer>(prc.get_ids());
	//	Collections.sort(members);
	//	System.err.println("cluster #" + ++cluster_i + ", size=" + prc.size() + " members:"+members);

	if (patient_ids.size() > 1) {
	  // make sure "cluster" has data for more than one patient ID

	  if (SUBSORT) {
	    SampleSortTools sst = new SampleSortTools(cr);
	    cr = sst.sort_by_cluster();
	  }
	  prc.set_sample_data(cr);
	  cluster_list_final.add(prc);
	
	  dividers.add(new Integer(rows_new.size()));
	}
	//	rows_new.add(separator);
	// use "row_count" variable because separators will pad new count
      }
    }
    
    visible_in_clusters = gm.set_order_by_cluster(cluster_list_final, separate_null_leftovers);

    setChanged();
    notifyObservers();

    //    System.err.println("done!");  // debug
    //    System.err.println("clustering time: "+ (System.currentTimeMillis() - cluster_start_time));

    if (report_results) new ClusterReporter(this, gm);

  }


  public static void main(String[] argv) {
    try {
      GenomicMeasurement gm = new GenomicMeasurement("Gene_Mutation_top8_new.txt", false);
      ClusterTool ct = new ClusterTool(gm);
      ct.cluster(new PearsonRCluster(), true, true);
    } catch (Exception e) {
      System.err.println("ERROR:"+e);  // debug
    }
  }

  public ArrayList<Cluster> get_clusters() {
    return cluster_list_final;
  }

  // begin Observer stub
  public void update (Observable o, Object arg) {
    matrix_build_cancelled = true;
  }
  // end Observer stub

  
  

}

