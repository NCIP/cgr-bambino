package TCGA;

import java.util.*;

public class HeatmapConfiguration {
  // startup/configuration options for heatmap instance

  //
  // main data components:
  //
  public GenomicMeasurement gm = null;
  public AnnotationFlatfile2 af = null;
  public AnnotationFlatfile2 vasari = null;
  public GenomicSet gs = null;
  public Heatmap6 parent_ref = null;

  public ArrayList<GenomicMeasurement> gm_supplemental = new ArrayList<GenomicMeasurement>();

  //
  // optional preferences and settings:
  //
  public String title = null;

  public boolean exit_on_close = true;
  public boolean display_sample_names = false;

  public boolean show_up_down_histogram = true;

  public Float minimum_initial_vertical_scale_factor = null;
  public Float maximum_vertical_scale_factor = null;
  
  public String start_bin = null;
  // start bin label (e.g. "chr7")
  public Integer start_pos = null;
  // start position within chromosome (nt)
  public Integer end_pos = null;
  // end position within chromosome (nt)

  public Pathway pathway;

  public String start_marker = null;
  // start centered on a particular marker/gene

  public BooleanOption enable_horizontal_zoom = new BooleanOption(true);
  public BooleanOption enable_vertical_zoom = new BooleanOption(true);

  public boolean white_mode = !Options.DEFAULT_BACKGROUND_BLACK;

  public boolean initial_max_x_zoom_checked = false;


  // fields used by SubsetCombiner:
  public GenomicMeasurement parent_gm;
  public ArrayList<String> parent_subsets;
  // end

  public HeatmapConfiguration() {
  }

  public HeatmapConfiguration (AnnotationFlatfile2 af, GenomicMeasurement gm, GenomicSet gs) throws Exception {
    this.af = af;
    this.gm = gm;
    this.gs = gs;
  }


}
