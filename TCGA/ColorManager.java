package TCGA;

import java.util.*;
import java.awt.Color;

public class ColorManager {
  private HashMap<String,ColorScheme> subset_colors;
  private SampleSubsets sample_subsets;
  private GenomicMeasurement gm;
  private HeatmapConfiguration config = null;

  private static final String GLOBAL_KEY = "__GLOBAL_COLOR__";
  private boolean multicolor_enabled = Options.ENABLE_INDEPENDENT_SUBSET_COLORS;

  //
  // colors defined for a black background:
  //
  private static Color COLOR_SELECTION = new Color(255,255,150);
  // mixed white/yellow
  private static Color COLOR_PATIENT_DIVIDER = Color.gray;
  private static Color COLOR_CLUSTER_DIVIDER = Color.yellow;
  private static Color COLOR_ZOOM_FOREGROUND = Color.black;
  private static Color COLOR_ZOOM_BACKGROUND = Color.white;

  public ColorManager (GenomicMeasurement gm) {
    this.gm=gm;
    sample_subsets = gm.get_sample_subsets();
    setup();
  }

  private void setup() {
    //
    //  initialize color schemes
    //
    subset_colors = new HashMap<String,ColorScheme>();
    subset_colors.put(GLOBAL_KEY, new ColorScheme(Options.DEFAULT_COLOR_SCHEME));

    if (!sample_subsets.isEmpty()) {
      // subsets available: create a separate color scheme for each
      // create even if multicolor initially disabled
      for (String subset : sample_subsets.get_subsets_arraylist()) {
	subset_colors.put(subset, new ColorScheme(Options.DEFAULT_COLOR_SCHEME));
      }
    }
    
    //
    //  load defaults from data file
    //
    boolean colors_set_from_data = false;
    CommentOptions options = gm.get_options();
    SampleSummaryInfo ssi = gm.get_sample_summary_info();

    String brightness = options.get("brightness");
    if (brightness != null) {
      Options.DEFAULT_MIN_COLOR_INTENSITY_PERCENT = Integer.parseInt(brightness);
      colors_set_from_data = true;
    }

    int[] scale = options.get_int_list("scale");
    if (scale != null) {
      //
      //  image color gradient values provided
      //

      if (options.has_option("neutral")) {
	//
	//  a "neutral" (center, "zero"-level) value for the data has been specified
	//
	byte neutral = (byte) options.get_single_neutral_level();
	if (neutral > 0) {
	  // HACK, cont'd: apply neutral level to gradients
	  for (int i=0; i < scale.length; i++) {
	    scale[i] -= neutral;
	  }
	}
      }

      colors_set_from_data = true;
      Options.DEFAULT_COPYNUMBER_GRADIENTS = scale;
      Options.MAX_GRADIENTS = scale[scale.length - 1] * 2;
    }

    if (colors_set_from_data) {
      // 
      // data file has specified color/gradient data.
      // apply this to all color schemes.
      //
      ColorSchemeModel csm = new ColorSchemeModel();
      csm.gradients = Options.DEFAULT_COPYNUMBER_GRADIENTS;
      csm.min_intensity_percent = Options.DEFAULT_MIN_COLOR_INTENSITY_PERCENT;

      for (ColorScheme cs : get_all_color_schemes()) {
	cs.import_gradients(csm);
      }
    } else if (Options.AUTO_OPTIMIZE_GRADIENTS) {
      //
      // automatically optimize each color scheme
      //
      get_global_color_scheme().import_gradients(ssi.calculate_gradients(null));

      if (is_multicolor_enabled()) {
	for (String subset : sample_subsets.get_subsets_arraylist()) {
	  //	  System.err.println("calculating for subset " + subset);  // debug
	  ColorScheme cs = subset_colors.get(subset);
	  cs.import_gradients(ssi.calculate_gradients(subset));
	}
      }
    } else {
      System.err.println("Warning: no gradients set in data or auto-optimize");  // debug
    }
    


  }

  public boolean is_multicolor_enabled() {
    return multicolor_enabled && !sample_subsets.isEmpty();
  }

  public void set_multicolor_enabled(boolean v) {
    multicolor_enabled = v;
  }


  public ColorScheme get_global_color_scheme() {
    return subset_colors.get(GLOBAL_KEY);
  }

  public HashMap<String,ColorScheme> get_subset_colors() {
    return subset_colors;
  }

  public ArrayList<ColorScheme> get_all_color_schemes() {
    ArrayList<ColorScheme> results = new ArrayList<ColorScheme>();
    // construct dynamically depending on options currently in effect;
    // i.e. we might have subsets but have disabled multicolor

    if (is_multicolor_enabled()) {
      // subsets available: create a separate color scheme for each
      // create even if multicolor initially disabled
      for (String subset : sample_subsets.get_subsets_arraylist()) {
	results.add(subset_colors.get(subset));
      }
    }
    results.add(subset_colors.get(GLOBAL_KEY));
    // can't just use addAll(sample_subsets.values()):
    //   1. in multicolor mode, an active subset color should be first in the list
    //      (vs. global which in this case would not be used)
    //   2. doesn't preserve ordering by subset
    return results;
  }

  public boolean is_white_mode() {
    if (config != null) {
      return config.white_mode;
    } else {
      System.err.println("warning: no HeatmapConfiguration ref!");  // debug
      return !Options.DEFAULT_BACKGROUND_BLACK;
    }
  }

  private Color invert_check (Color result) {
    if (is_white_mode()) result = invert_color(result);
    return result;
  }

  public Color get_selection_color() {
    return invert_check(COLOR_SELECTION);
  }

  public Color get_patient_divider_color() {
    return invert_check(COLOR_PATIENT_DIVIDER);
  }

  public Color get_cluster_divider_color() {
    return invert_check(COLOR_CLUSTER_DIVIDER);
  }

  public Color get_zoom_foreground_color() {
    return invert_check(COLOR_ZOOM_FOREGROUND);
  }

  public Color get_zoom_background_color() {
    return invert_check(COLOR_ZOOM_BACKGROUND);
  }

  private static Color invert_color (Color c) {
    return(new Color(
		     255 - c.getRed(),
		     255 - c.getGreen(),
		     255 - c.getBlue()
		     ));
  }

  public void set_config (HeatmapConfiguration config) {
    this.config = config;
  }

  


}
