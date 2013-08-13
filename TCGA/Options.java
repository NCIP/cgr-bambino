package TCGA;

import java.util.*;
import java.io.*;
import java.awt.Color;

public class Options {

  public Options () {
  }

  static boolean COMBINE_DATASETS_ADD_ROWS_MODE = true;
  // if true, combines heatmaps by adding new rows, preserving column
  // layout of first heatmap, and attempting to group data with
  // related barcodes.
  //
  // if false, combines heatmaps by adding new columns and modifying
  // column labels to include data type (e.g. copy number, gene
  // expression).  Combined dataset will retain one row per sample.

  static boolean GZIP_URL_STREAMS = true;

  static boolean MOUSE_ZOOM_INFLUENCED_BY_CURSOR = true;
  static boolean MOUSE_ZOOM_INFLUENCED_BY_CURSOR_IN_ONLY = true;
  static int MOUSE_ZOOM_INFLUENCE_DILUTION = 5;

  static int BRIGHTNESS_MINOR_TICK = 10;

  //  static boolean INCLUDE_DEVELOPMENT_CODE = true;
  static boolean INCLUDE_DEVELOPMENT_CODE = false;

  //  static boolean ENABLE_INDEPENDENT_SUBSET_COLORS = INCLUDE_DEVELOPMENT_CODE;
  static boolean ENABLE_INDEPENDENT_SUBSET_COLORS = true;
  
  //  public static boolean USE_INDEXED_COLOR = false;
  // lower memory usage at cost of reduced color depth 

  // public static boolean DISABLE_ACCELERATION = true;
  // display driver problems on my old PC...
  public static boolean DISABLE_ACCELERATION = false;

  // things to add:
  // - (default?) Y pixel scaling

  //public static float ZOOM_FACTOR = (float) 1.5;
  // public static float ZOOM_FACTOR = (float) 1.25;
  public static float ZOOM_FACTOR = (float) 1.15;

  public static boolean NAVIGATION_DEFAULT_ZOOM = true;

  //  public static boolean REVERSE_MOUSE_DRAG_SCROLL = true;
  // moved to MouseDragScroller??

  //  public static int SLIDER_COUNT = 6;
  //  public static int[] DEFAULT_COPYNUMBER_GRADIENTS = {1,3,4,5,10,20};
  // in this code, must be sorted 
  public static int SLIDER_COUNT = 5;
  public static int[] DEFAULT_COPYNUMBER_GRADIENTS = {1,3,4,5,10};

  //  public static int DEFAULT_MIN_COLOR_INTENSITY = 60;
  public static int DEFAULT_MIN_COLOR_INTENSITY_PERCENT = 30;

  public static boolean VERBOSE_ERRORS = true;

  public static boolean IS_APPLET = false;

  public static boolean NORMALIZE_SUMMARY_PANEL_PEAKS = true;

  public static boolean LOCK_ZOOM_OUT = true;

  public static int DISABLE_HORIZONTAL_ZOOM_COLUMNS = 20;
  // if viewing a dataset with a small number of columns,
  // disable horizontal zooming by default
  
  public static int MAX_GRADIENTS = 20;

  public static int INDEX_COLOR_CREATION_THRESHOLD = 10000000;
  // size of image to create in pixels where we switch to using
  // indexed color to save memory and prevent java out-of-heap-memory errors

  public static CommentOptions COMMENT_OPTIONS = new CommentOptions();

  public static boolean AUTO_OPTIMIZE_GRADIENTS = true;

  public static int DEFAULT_VERTICAL_PIXELS_PER_SAMPLE = 6;

  public static float MIN_VERTICAL_SCALE_FACTOR = 1.0f;
  //  public static float MAX_VERTICAL_SCALE_FACTOR = 20.0f;
  //  public static float MAX_VERTICAL_SCALE_FACTOR = 10.0f;
  public static float MAX_VERTICAL_SCALE_FACTOR = 11.0f;
  // FIX ME: default based on # of samples in input data??

  public static String DATA_TYPE = "Copy number";

  public static boolean SUMMARY_PANEL_INCREASE_DECREASE_FANCY = false;

  //  public static String DEFAULT_COLOR_SCHEME = ColorScheme.COLOR_SCHEME_MAGENTA_TEAL;
  public static String DEFAULT_COLOR_SCHEME = ColorScheme.COLOR_SCHEME_RED_BLUE;
  // 7/16/08 -- back to this 
  //  public static boolean DEFAULT_BACKGROUND_BLACK = true;
  public static boolean DEFAULT_BACKGROUND_BLACK = false;
  // 10/2008: Ken 

  public static float MAX_Y_WINDOW_SIZE_FRACTION = 0.90f;

  public static Color DEFAULT_CUSTOM_TRACK_COLOR = Color.blue;

  public static int SUMMARY_PANEL_INDENT_PIXELS = 10;
  public static int SUMMARY_PANEL_FONT_HEIGHT = 11;

  //  public static boolean OPTIMIZED_INDEX_RENDERING = true;

  public static int MINIMUM_BINS_TO_DISPLAY_IN_SELECTION_ZOOM = 76;
  // ensure at least this many bins are displayed onscreen
  // (prevent a small selection from taking up the entire width of the display)

  public static boolean DEFAULT_USE_BINARY_SAMPLE_FILE = true;

  public static boolean PRESERVE_VERTICAL_SCALING_SETTING = true;
  // if user has set the vertical scale level,
  // respect it in all "reset" situations (zoom 1:1, out max, etc.)

  public static boolean SUPPRESS_DIVIDERS_IF_ALL_SINGLETONS = true;
  // when rebuilding dividers/separators (for sample grouping),
  // don't use any dividers if all the sample groups between dividers
  // contain only one sample.  This can happen if displaying only
  // one of many subsets of data -- the dividers become redundant 
  // and hamper the view at high zoom-out levels.

  //  public static String ABBREVIATION_COPY_NUMBER = "cna";
  //  public static String ABBREVIATION_GENE_EXPRESSION = "exp";
  public static String ABBREVIATION_COPY_NUMBER = "CNA";
  public static String ABBREVIATION_GENE_EXPRESSION = "EXP";

  public static int MIN_PIXELS_BETWEEN_BIN_BORDERS = 10;

  public static int MIN_COLUMNS_TO_SET_FIXED_WIDTH = 20;

  public static boolean STARTUP_RESTRICT_COLUMNS = false;
  // whether initial view is restricted to a specified set of columns

  public static boolean INTERACTIVE_RESTRICT_COLUMNS = false;
  // same as above, but started interactively

  public static HashSet<String> RESTRICT_COLUMNS = null;
  // columns to restrict to
  public static boolean RESTRICT_PRESERVE_BINS = false;

  public static boolean are_bins_paintable (GenomicSet gs, ScalePanel2 sp2) {
    int start_x = sp2.get_unscaled_x_start();
    int end_x = sp2.get_unscaled_x_end() + 1;
    // +1: allow extra for scaled bin central values
    float x_scale = sp2.get_horizontal_scale_level();

    GenomicBin b1 = null;
    GenomicBin b2 = null;
    for (GenomicBin gb : gs.get_bins()) {
      // hack: assumes bins are ordered
      if (gb.end >= start_x && gb.end <= end_x) {
	if (b1 == null) {
	  b1 = gb;
	} else if (b2 == null) {
	  b2 = gb;
	} else {
	  break;
	}
      }
    }

    boolean draw_bin_borders = false;
    if (b1 != null && b2 != null) {
      int x1 = (int) ((b1.end - start_x) * x_scale);
      int x2 = (int) ((b2.end - start_x) * x_scale);
      draw_bin_borders = x2 - x1 >= Options.MIN_PIXELS_BETWEEN_BIN_BORDERS;
    } else {
      draw_bin_borders = true;
    }
    
    //    System.err.println("dbb="+draw_bin_borders);  // debug
    return draw_bin_borders;
  }

}
