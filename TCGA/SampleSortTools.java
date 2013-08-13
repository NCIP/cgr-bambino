package TCGA;

import java.util.*;

public class SampleSortTools {

  public static String AVERAGE_VALUE = "Average value";
  public static String HIGHEST_ABSOLUTE_VALUE = "Highest absolute value";
  public static String CLUSTER_SUBSET_ALL = "All visible subsets";
  public static String CLUSTER_VALUE_ANY = "Positive or negative";
  public static String CLUSTER_VALUE_POSITIVE = "Positive only";
  public static String CLUSTER_VALUE_NEGATIVE = "Negative only";
  // hack: use strings for these values as they are used as labels in ClusterControl
  // maybe should be final but inconvenient for recompiling other classes if strings change
  private static final int NEUTRAL_VALUE = 0;

  private GenomicMeasurement gm;
  private GenomicSet gset;
  private ArrayList<GenomicSample> rows;
  private HashMap<String,ArrayList<GenomicSample>> patient2samples;
  private float minimum_value_to_cluster = 0;
  private boolean rearrange_columns = true;
  private float average;
  private int column_count;

  private String multi_sample_representative_method = AVERAGE_VALUE;
  private String subset_filter = CLUSTER_SUBSET_ALL;
  private String value_filter = CLUSTER_VALUE_ANY;

  public SampleSortTools (GenomicMeasurement gm, GenomicSet gset) {
    // sort with possible bin reordering
    this.gm = gm;
    this.gset = gset;
    rows = gm.get_rows();
    column_count = gm.get_column_count();
    setup();
  }

  public SampleSortTools (GenomicMeasurement gm) {
    // sort without reordering
    this.gm = gm;
    rows = gm.get_rows();
    column_count = gm.get_column_count();
    setup();
  }

  public SampleSortTools (ArrayList<GenomicSample> rows) {
    // only sorting a subset of samples
    this.rows = rows;
    column_count = rows.get(0).copynum_data.length;
    // hacktacular
    setup();
  }

  public void set_rearrange_columns (boolean value) {
    rearrange_columns = value;
  }

  public void set_multi_sample_representative_method (String method) {
    multi_sample_representative_method = method;
    // HACK: FIX ME, CHECK VALID
  }

  public void set_minimum_value_to_cluster (float value) {
    minimum_value_to_cluster = value;
  }

  public void set_value_filter (String method) {
    value_filter = method;
  }

  private void setup() {
    patient2samples = build_patient_to_sample_map();
  }

  public void set_subset_filter (String subset) {
    // sort/cluster based only on values for given subset
    subset_filter = subset;
  }

  public HashMap<String,ArrayList<GenomicSample>> get_patient2samples() {
    return patient2samples;
  }

  public float get_representative_value (String patient, int index) {
    //
    //  get representative data value for a given patient ID, 
    //  taking multiple samples and visibility/sort filters into account
    //
    byte point;
    ArrayList<Byte> points = new ArrayList<Byte>();
    ArrayList<GenomicSample> samples = patient2samples.get(patient);
    
    boolean filter_by_subset = !subset_filter.equals(CLUSTER_SUBSET_ALL);
    boolean filter_by_value = !value_filter.equals(CLUSTER_VALUE_ANY);
    boolean filter_by_value_positive = value_filter.equals(CLUSTER_VALUE_POSITIVE);

    for (GenomicSample gs : samples) {
      if (filter_by_subset && gs.sample_id.indexOf(subset_filter) == -1) {
	//	System.err.println("MISSING for " + gs.sample_id);  // debug
	continue;
      }
      
      if (gs.visible_in_display == false) {
	continue;
      }

      //	System.err.println("pat="+patient + " sample="+gs.sample_id);  // debug
      point = gs.copynum_data[index];

      if (point == GenomicSample.NULL_VALUE) continue;

      if (filter_by_value && 
	  (filter_by_value_positive ? point < 0 : point > 0)) continue;

      points.add(new Byte(point));
    }
    float representative;
    int size = points.size();
    if (size == 0) {
      representative = GenomicSample.NULL_VALUE;
    } else if (size == 1) {
      representative = points.get(0);
    } else {
      if (multi_sample_representative_method.equals(AVERAGE_VALUE)) {
	// use average value in observed set for representative value
	float total = 0;
	for (Byte b : points) {
	  total += b;
	}
	representative = total / size;
      } else {
	// use highest absolute value in observed set for representative value
	Collections.sort(points);
	int min = points.get(0);
	int max = points.get(points.size() - 1);
	int abs_min = Math.abs(min);
	representative = (abs_min > max) ? min : max;
      }
    }
    return representative;
  }

  public DoubleHashMap build_patient_subset_map() {
    // bucket samples by patient and subset ID
    System.err.println("FIX ME: replaced by gm.get_sample_for_patient_subset()");  // debug
    DoubleHashMap dhm = new DoubleHashMap();
    for (GenomicSample gs : rows) {
      dhm.put(gs.patient_id, gs.subset_id, gs);
    }
    return dhm;
  }

  public HashMap<String,ArrayList<GenomicSample>> build_patient_to_sample_map() {
    // bucket samples by patient ID, preserving existing order
    // (e.g. if multiple samples per patient from different centers)
    // NOTE: this map contains ALL samples, regardless of visibility or
    // desired sort order!

    HashMap<String,ArrayList<GenomicSample>> patient2samples = new HashMap<String,ArrayList<GenomicSample>>();

    for (GenomicSample gs : rows) {
      ArrayList<GenomicSample> bucket = patient2samples.get(gs.patient_id);
      if (bucket == null) patient2samples.put(gs.patient_id, bucket = new ArrayList<GenomicSample>());
      bucket.add(gs);
    }
    return patient2samples;
  }


  public ArrayList<GenomicSample> sort_by_cluster() {
    //    System.err.println("sort by cluster");  // debug

    HashSet<Integer> headers_left = new HashSet<Integer>();
    for (int i = 0; i < column_count; i++) {
      headers_left.add(i);
    }
    ArrayList<Integer> reordered_headers = new ArrayList<Integer>();

    HashSet<String> ids_left = new HashSet<String>(patient2samples.keySet());

    ArrayList<GenomicSample> rows_new = new ArrayList<GenomicSample>();
    ArrayList<Cluster> clusters = new ArrayList<Cluster>();
    
    while (headers_left.size() > 0 && ids_left.size() > 0) {
      //
      //  while still columns left to sort,
      //  find the best candidate column to sort based on data for remaining samples
      //
      int id_count = ids_left.size();

      float best_frac = -1.0f;
      boolean best_up = false;
      int best_header = -1;

      //
      //  find the header with the most extreme percentage of changes in samples
      //  (either up or down):
      //
      for (Integer h : headers_left) {
	int hi = h;
	int total_up = 0;
	int total_down = 0;
	for (String id : ids_left) {
	  float rep = get_representative_value(id, hi);
	  if (rep != GenomicSample.NULL_VALUE && rep != NEUTRAL_VALUE) {
	    if (rep >= minimum_value_to_cluster) {
	      total_up++;
	    } else if (rep <= - minimum_value_to_cluster) {
	      total_down++;
	    }
	  }
	}

	float frac_up = (float) total_up / id_count;
	float frac_down = (float) total_down / id_count;

	if (best_frac == -1.0f || frac_up > best_frac) {
	  best_header = h;
	  best_frac = frac_up;
	  best_up = true;
	}

	if (frac_down > best_frac) {
	  best_header = h;
	  best_frac = frac_down;
	  best_up = false;
	}
      }

      //
      //  aggregate samples showing change in desired direction
      //

      ArrayList<String> ids_to_remove = new ArrayList<String>();
      
      ArrayList<GenomicSample> queue = new ArrayList<GenomicSample>();

      for (String id : ids_left) {
	float rep = get_representative_value(id, best_header);
	if (rep != GenomicSample.NULL_VALUE && 
	    rep != NEUTRAL_VALUE &&
	    (best_up ? rep >= minimum_value_to_cluster : rep <= - minimum_value_to_cluster)) {
	  queue.addAll(patient2samples.get(id));
	  ids_to_remove.add(id);
	}
      }

      if (queue.size() == 0) {
	//	System.err.println("stopping: nothing else clusters");  // debug
	break;
      } else {
	//	System.err.println("best="+ best_header + " count=" + queue.size());  // debug
	SampleSortTools sst = new SampleSortTools(queue);
	ArrayList<GenomicSample> sorted = sst.sort_by_bin(best_header);
	rows_new.addAll(sorted);

	PearsonRCluster cluster = new PearsonRCluster();
	// FIX ME: not this type; new subtype?
	for (int i = 0; i < sorted.size(); i++) {
	  cluster.add(i);
	  // bogus/placeholder data
	}

	cluster.set_sample_data(sorted);
	clusters.add(cluster);

	headers_left.remove(best_header);
	reordered_headers.add(best_header);
	ids_left.removeAll(ids_to_remove);
      }

    }

    reordered_headers.addAll(headers_left);
    // leftover headers

    for (String id : ids_left) {
      // leftover samples
      rows_new.addAll(patient2samples.get(id));
    }

    //    System.err.println("reordered="+reordered_headers);  // debug

    if (gset != null && rearrange_columns) {
      if (gset.is_marker_bin()) {
	for (GenomicSample gs : rows_new) {
	  // This will reorder the underlying sample data.
	  // However subwindows already use copies of the original data
	  // (filtered to show only a subset of bins) so this won't 
	  // corrupt the GenomicSample data in the original/startup view.
	  gs.reorder_data(reordered_headers);
	}
	gm.reorder_headers(reordered_headers);
	gset.generate_marker_bins();
	// rebuild bins to reflect new header ordering
      } else {
	System.err.println("can't reorder headers: set not binned by marker");  // debug
      }
    }

    if (gm != null) {
      gm.get_divider_manager().set_cluster_list(clusters);
      gm.set_rows(rows_new, false);
      // don't reset cluster dividers as we've just set them manually
    }
    return rows_new;
  }

  public ArrayList<GenomicSample> sort_by_bin (int index) {
    // foreach sample:
    // - record highest observed value for each patient ID for given bin
    // - sort unique list of highest observed values
    // - iterate through and rebuild list

    HashMapArrayList rep2samples = new HashMapArrayList();

    float total_data = 0;
    int usable_values = 0;
    for (String patient : patient2samples.keySet()) {
      float rep = get_representative_value(patient,index);
      if (rep != GenomicSample.NULL_VALUE) {
	total_data += rep;
	usable_values++;
      }
      rep2samples.addAll(new Float(rep),
			 patient2samples.get(patient));
      // bucket samples by representative values
    }

    //    System.err.println("keyset="+rep2samples.keySet());  // debug

    average = total_data / usable_values;

    ArrayList<Float> representative = new ArrayList<Float>(rep2samples.keySet());
    Collections.sort(representative);
    //    System.err.println("representative="+representative);  // debug

    ArrayList<GenomicSample> rows_new = new ArrayList<GenomicSample>();
    for (Float f : representative) {
      ArrayList<GenomicSample> all = rep2samples.get(f);
      //      System.err.println("all="+all);  // debug
      rows_new.addAll(all);
    }

    return(rows_new);
  }

  public float get_average() {
    return average;
  }

}
