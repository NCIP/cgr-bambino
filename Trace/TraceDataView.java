package Trace;

public class TraceDataView {

  //  private static double DEFAULT_SCALE_FACTOR_LIMIT = 3.0;
  private static double DEFAULT_SCALE_FACTOR_LIMIT = 5.0;

  private TraceFile tf;
  private int start_point, chunk_size;
  private int[] buffer;
  private int[] max_by_type = new int[4];

  private double scale_factor = 1.0;
  // scaling factor used
  private double scale_factor_limit = DEFAULT_SCALE_FACTOR_LIMIT;
  // maximum scaling to allow
  private double max_scale_factor;
  // most we can scale samples in window without clipping

  private static boolean GLOBAL_AUTOSCALE = false;
  private static double GLOBAL_SCALE_FACTOR_LIMIT = DEFAULT_SCALE_FACTOR_LIMIT;

  private boolean autoscale = false;
  // automatically scale sample data in window, to a maximum of
  // scale_factor_limit

  public TraceDataView (TraceFile tf) {
    this.tf = tf;
  }

  public void set_buffer (int[] buffer) {
    this.buffer = buffer;
    chunk_size = buffer.length;
    // appropriate?
  }

  public void set_start_point (int sp) {
    start_point = sp;
  }

  public int get_sample (int base_type, int index) {
    return (int) (tf.trace_data[base_type][index] * scale_factor);
  }

  public void get_samples (int base_type) {
    int i;
    set_scale_factor();
    for (i=0; i < chunk_size; i++) {
      //      buffer[i] = tf.trace_data[base_type][start_point + i];
      buffer[i] = (int) (tf.trace_data[base_type][start_point + i] * scale_factor);
      // sample data plus any scaling factor in effect
    }
  }

  private void set_scale_factor () {
    if (GLOBAL_AUTOSCALE) {
      // static
      scale_factor = max_scale_factor;
      if (GLOBAL_SCALE_FACTOR_LIMIT > 0 &&
          scale_factor > GLOBAL_SCALE_FACTOR_LIMIT)
        scale_factor = GLOBAL_SCALE_FACTOR_LIMIT;
    } else if (autoscale) {
      // instance
      scale_factor = max_scale_factor;
      if (scale_factor > scale_factor_limit) scale_factor = scale_factor_limit;
    } else {
      // disabled
      scale_factor = 1.0;
    }
    //    System.err.println("max:" + max_scale_factor + " using scale: " + scale_factor + " global:" + GLOBAL_AUTOSCALE);  // debug
  }

  public void analyze () {
    //
    // - analyze the current view of the trace
    // - find max value for each channel
    // - find maximum scaling factor to avoid clipping
    //

    int base,v,i,max;
    int end = start_point + chunk_size;
    int max_for_all = 0;
    for (base=0; base < 4; base++) {
      max = 0;
      for (i=start_point; i < end; i++) {
        v = tf.trace_data[base][i];
        if (v > max) max = v;
      }
      max_by_type[base] = max;
      if (max > max_for_all) max_for_all = max;
      //      System.err.println("max for " + base + ":" + max);  // debug
    }
    max_scale_factor = (double) tf.max_amplitude / max_for_all;
    //    System.err.println("mfa:" + max_for_all);  // debug
    //    System.err.println("max scale:" + max_scale_factor);
    // System.err.println("");  // debug
  }

  public static void set_static_auto_normalization (boolean status) {
    GLOBAL_AUTOSCALE = status;
  }

  public static void set_static_auto_normalization_limit (double factor) {
    GLOBAL_SCALE_FACTOR_LIMIT = factor;
  }
  
}
