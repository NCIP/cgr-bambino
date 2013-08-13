package TCGA;

import java.util.*;

public class SampleSummaryInfo {
  private static final int TRACK_COUNT = 100;
  // range of absolute values to track

  private GenomicMeasurement gm;
  private HashMap<String,int[]> sample2abs_counts,subset2abs_counts;
  private int[] global_abs_counts;
  // counts for all samples

  public SampleSummaryInfo (GenomicMeasurement gm) {
    this.gm = gm;
    setup();
  }

  private void setup() {
    ArrayList<GenomicSample> rows = gm.get_rows();
    sample2abs_counts = new HashMap<String,int[]>();
    subset2abs_counts = new HashMap<String,int[]>();

    int i;
    int[] abs_counts;

    for (GenomicSample gs : rows) {
      // count incidence of different absolute values for each sample
      abs_counts = new int[TRACK_COUNT];
      for (i=0; i < gs.copynum_data.length; i++) {
	abs_counts[Math.abs(gs.copynum_data[i])]++;
      }
      sample2abs_counts.put(gs.sample_id, abs_counts);
    }

    // get counts for all samples:
    global_abs_counts = new int[TRACK_COUNT];
    for (GenomicSample gs : rows) {
      abs_counts = sample2abs_counts.get(gs.sample_id);
      for (i=0; i < abs_counts.length; i++) {
	global_abs_counts[i] += abs_counts[i];
      }
    }

    // collect counts by subset:
    SampleSubsets ss = gm.get_sample_subsets();
    if (ss != null && !ss.isEmpty()) {
      for (String subset : ss.get_subsets_arraylist()) {
	//	System.err.println("sub="+subset);  // debug
	int[] subset_counts = new int[TRACK_COUNT];
	for (GenomicSample gs : rows) {
	  if (gs.sample_id.indexOf(subset) >= 0) {
	    // hack and the hacktones; got to be a better way
	    abs_counts = sample2abs_counts.get(gs.sample_id);
	    for (i=0; i < abs_counts.length; i++) {
	      subset_counts[i] += abs_counts[i];
	    }
	  }
	}
	subset2abs_counts.put(subset, subset_counts);
      }
    }

  }

  public int[] get_global_abs_counts() {
    return global_abs_counts;
  }

  public int[] get_subset_abs_counts(String subset) {
    return subset2abs_counts.get(subset);
  }

  //  public ColorSchemeModel calculate_gradients (int[] abs_counts) {
  public ColorSchemeModel calculate_gradients (String subset) {
    // 
    //  guesstimate initial gradient values based on data distribution
    //

    int[] abs_counts = subset == null ? get_global_abs_counts() : get_subset_abs_counts(subset);

    int total_active = 0;
    int i;
    int skip = Math.abs(GenomicSample.NULL_VALUE);
    for (i=1; i < TRACK_COUNT; i++) {
      if (i != skip) total_active += abs_counts[i];
    }

    HashSet hs = new HashSet();
    
    //    float[] rungs_to_generate = {0.05f, 0.10f, 0.80f, 0.90f, 0.95f, 0.99f};

    float[] rungs_to_generate = {0.25f, 0.50f, 0.75f, 0.90f, 0.95f, 0.97f, 0.99f};
    // Genome_CopyNumber_Broad_Paired_Affymetrix.txt gets only 1 gradient
    // unless we search for 99%

    //    float[] rungs_to_generate = {0.25f, 0.50f, 0.75f, 0.90f, 0.95f, 0.97f};
    //    float[] rungs_to_generate = {0.20f, 0.50f, 0.75f, 0.90f, 0.95f, 0.97f};
    for (i=0; i < rungs_to_generate.length; i++) {
      int rung = find_rung(abs_counts, total_active, rungs_to_generate[i]);
      //      System.err.println((rungs_to_generate[i] * 100) + "% rung: " + rung);  // debug
      hs.add(new Integer(rung));
    }
    ArrayList al = new ArrayList(hs);
    Collections.sort(al);
    //    System.err.println("sorted:"+al);  // debug

    int grad_count = al.size();
    if (grad_count > Options.SLIDER_COUNT) grad_count = Options.SLIDER_COUNT;
    // fixed number of gradients available

    ColorSchemeModel gr = new ColorSchemeModel();

    gr.gradients = new int[grad_count];

    int al_i;
    for (i=0, al_i=al.size() - grad_count; al_i < al.size(); i++, al_i++) {
      //      System.err.println("al_i="+al_i);  // debug
      gr.gradients[i] = ((Integer) al.get(al_i)).intValue();
      //      System.err.println("value="+gradients[i]);  // debug
    }

    gr.min_intensity_percent = ContrastControlPanel.calculate_minimum_intensity(gr.gradients.length);
    //    gr.base_intensity = 80 - (gr.gradients.length * 10);
    // better for 5 gradients (test2.tab)
    if (gr.min_intensity_percent < 10) gr.min_intensity_percent = 10;

    //    System.err.println("base intensity="+gr.base_intensity);  // debug
    //    System.err.println("gradient count="+grad_count);  // debug

    return gr;
  }

  public int find_rung (int[] abs_counts, int total, float rung_level) {
    int threshold = (int) (total * rung_level);
    int i;
    int so_far = 0;
    int rung = 0;
    int skip = Math.abs(GenomicSample.NULL_VALUE);
    for (i=1; i < TRACK_COUNT; i++) {
      if (i != skip) {
	so_far += abs_counts[i];
	if (so_far >= threshold) {
	  rung = i;
	  break;
	}
      }
    }
    return rung;
  }


}


