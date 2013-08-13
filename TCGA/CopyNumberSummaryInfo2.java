package TCGA;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class CopyNumberSummaryInfo2 extends Observable implements Runnable,Observer {

  //  private static int CNC_THRESHOLD = 2;
  private int change_threshold = 2;

  private GenomicMeasurement gm;
  private boolean built = false;

  public byte[] percent_showing_any = null;
  public byte[] percent_showing_increase = null;
  public byte[] percent_showing_decrease = null;
  public byte max_percent_showing_any = 0;
  public byte max_percent_showing_increase = 0;
  public byte max_percent_showing_decrease = 0;

  private ColorScheme cs;

  public CopyNumberSummaryInfo2 (GenomicMeasurement gm) {
    // synchronous
    this.gm = gm;

    ArrayList<ColorScheme> schemes = gm.get_color_manager().get_all_color_schemes();
    cs = schemes.get(0);
    cs.addObserver(this);
    gm.addObserver(this);
    run();
  }
  
  public void run() {
    set_threshold_from_gradients();
    build_all();
  }

  public int get_change_threshold() {
    return change_threshold;
  }

  public String get_bin_label (int index) {
    return gm.get_headers()[index];
  }

  public void set_change_threshold(int v) {
    change_threshold = v;
    build_all();
  }

  public void update (Observable o, Object arg) {
    if (o instanceof ColorScheme) {
      set_threshold_from_gradients();
    } else if (o instanceof GenomicMeasurement) {
      if (gm.is_loaded()) {
	// final update from GenomicMeasurement
	set_threshold_from_gradients();
	// reset gradients, as load might have changed defaults
      }
    }
    if (gm.is_loaded()) build_all();
  }

  public boolean is_loaded () {
    return built;
  }

  private void build_all() {
    built = false;
    if (gm.is_loaded()) {
      generate_stats();
      // generate summary statistics
      built = true;
      setChanged();
      notifyObservers();
      // done
    }
  }

  private void set_threshold_from_gradients() {
    int[] gradients = cs.get_gradients();
    if (true) {
      //
      // use some fraction of maximum slider value
      //
      float FRACTION = 0.5f;
      int max_slider = gradients[gradients.length - 1];
      change_threshold = (int) (max_slider * FRACTION);
      if (change_threshold == 0) change_threshold=1;
      //      System.err.println("max slider:" + max_slider + " threshold="+change_threshold);
    } else {
      //
      //  use one of the gradients
      //

      //    int which = gradients.length - 2;
      // second-to-last gradient 
    
      int which = (int) (gradients.length / 2);
      which--;
      // "middle" gradient

      if (which < 0) which = 0;
      change_threshold = gradients[which];
      System.err.println("threshold="+change_threshold + " index="+which + " grads="+gradients.length);  // debug
    }

  }

  public byte[] get_percent_showing_any() {
    return percent_showing_any;
  }

  private void generate_stats() {
    int sample_count = gm.get_row_count();
    int genome_bin_size = 0;
    int[] bin_increase = null;
    int[] bin_decrease = null;
    int i;

    //
    // scan sample data and bin incidence of changes
    //
    for (GenomicSample gs : gm.get_visible_rows()) {
      if (genome_bin_size == 0) {
	genome_bin_size = gs.copynum_data.length;
	bin_increase = new int[genome_bin_size];
	bin_decrease = new int[genome_bin_size];
      }
      for (i=0; i < gs.copynum_data.length; i++) {
	if (gs.copynum_data[i] != GenomicSample.NULL_VALUE) {
	  if (gs.copynum_data[i] >= change_threshold) {
	    bin_increase[i]++;
	  } else if (gs.copynum_data[i] <= -change_threshold) {
	    bin_decrease[i]++;
	  }
	}
      }
    }

    //
    //  calculate percentage of samples changed per bin
    //
    percent_showing_any = new byte[genome_bin_size];
    percent_showing_increase = new byte[genome_bin_size];
    percent_showing_decrease = new byte[genome_bin_size];
    for (i=0; i < genome_bin_size; i++) {
      percent_showing_increase[i] = (byte) ((bin_increase[i] * 100) / sample_count);
      percent_showing_decrease[i] = (byte) ((bin_decrease[i] * 100) / sample_count);
      percent_showing_any[i] = (byte) (((bin_increase[i] + bin_decrease[i]) * 100) / sample_count);
    }

    //
    // max thresholds (for normalization)
    //
    max_percent_showing_any = find_maximum(percent_showing_any);
    max_percent_showing_increase = find_maximum(percent_showing_increase);
    max_percent_showing_decrease = find_maximum(percent_showing_decrease);

  }

  private byte find_maximum (byte[] array) {
    byte result = 0;
    int i;
    for (i=0; i < array.length; i++) {
      if (array[i] > result) result = array[i];
    }
    return result;
  }
  
  public static void main (String [] argv) {
    try {
      String fn = "broad_snp6_genomicmeasurement.txt";
      GenomicMeasurement gm = new GenomicMeasurement(fn, false);
      ColorScheme cs = new ColorScheme(Color.red, Color.blue);
      //      CopyNumberSummaryInfo2 cnsi = new CopyNumberSummaryInfo2(gm,cs);
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

}
