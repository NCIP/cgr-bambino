package TCGA;
// color scheme/mapping for heatmap and other widgets

import java.awt.*;
import java.util.*;

public class ColorScheme extends Observable {
  public static String COLOR_SCHEME_RED_GREEN = "Red/Green";
  public static String COLOR_SCHEME_RED_BLUE = "Red/Blue";
  public static String COLOR_SCHEME_MAGENTA_CYAN = "Magenta/Cyan";

  // the following 3 are based on wheel-opposite pairings using 3 primary & 3 secondary colors:
  // http://www.xaraxone.com/webxealot/workbook02/color_wheel_4.jpg
  public static String COLOR_SCHEME_RED_CYAN = "Red/Cyan";
  public static String COLOR_SCHEME_GREEN_MAGENTA = "Green/Magenta";
  public static String COLOR_SCHEME_BLUE_YELLOW = "Blue/Yellow";
  
  public static String COLOR_SCHEME_GREEN_YELLOW = "Green/Yellow";
  public static String COLOR_SCHEME_ORANGE_WHITE = "Orange/White";
  public static String COLOR_SCHEME_YELLOW_WHITE = "Yellow/White";

  public static final String LABEL_BLACK = "Black";
  public static final String LABEL_WHITE = "White";

  public static final String LABEL_RED = "Red";
  public static final String LABEL_GREEN = "Green";
  public static final String LABEL_BLUE = "Blue";

  public static final String LABEL_CYAN = "Cyan";
  public static final String LABEL_MAGENTA = "Magenta";
  public static final String LABEL_YELLOW = "Yellow";

  public static final String LABEL_ORANGE = "Orange";

  private ColorSchemeModel csm;
  private static final int MAX_COLOR_VALUE = 0xff;
  private Color[] color_map_up,color_map_down;
  private HashSet<Color> unique_colors;

  private static ArrayList<String> color_names_sorted;
  private static HashMap<String,Color> color_names = color_names_setup();
  private static HashMap<String,ColorSchemeModel> color_schemes = color_scheme_setup();

  public ColorScheme (Color up_color, Color down_color) {
    csm = new ColorSchemeModel(up_color, down_color);
    set_colorscheme_model(csm);
  }

  public ColorScheme (String name) {
    ColorSchemeModel model = color_schemes.get(name);
    if (model != null) {
      csm = new ColorSchemeModel(model.up_color, model.down_color);
      set_colorscheme_model(csm);
    } else {
      System.err.println("ERROR, no scheme named " +name);  // debug
    }
  }

  public void set_colorscheme_model (ColorSchemeModel csm) {
    set_colorscheme_model(csm, true);
  }

  public void set_colorscheme_model (ColorSchemeModel csm, boolean notify) {
    this.csm = csm;
    unique_colors = new HashSet<Color>();
    build_up_color_map();
    build_down_color_map();
    if (notify) {
      setChanged();
      notifyObservers();
    }
  }

  public void notify_observers() {
    // hack
    setChanged();
    notifyObservers();
  }

  public ColorSchemeModel get_colorscheme_model () {
    // current model
    return csm;
  }

  public static ColorSchemeModel get_colorscheme_model(String name) {
    // fixed model with this name; basically just used just for colors
    return color_schemes.get(name);
  }

  public int[] get_gradients() {
    return csm.gradients;
  }

  public int get_minimum_intensity_percent() {
    return csm.min_intensity_percent;
  }

  public Color[] get_up_color_map() {
    return color_map_up;
  }

  public Color[] get_down_color_map() {
    return color_map_down;
  }

  private void build_up_color_map() {
    color_map_up = get_color_map(csm.up_color, csm.gradients);
  }

  private void build_down_color_map() {
    color_map_down = get_color_map(csm.down_color, csm.gradients);
  }

  private Color[] get_color_map (Color max_color, int[] gradients) {
    Color background_color = csm.white_mode ? Color.white : Color.black;
    // DEBUG

    int[] max_color_channel = new int[3];
    boolean[] channel_active = new boolean[3];
    
    max_color_channel[0] = max_color.getRed();
    max_color_channel[1] = max_color.getGreen();
    max_color_channel[2] = max_color.getBlue();

    int i,j;
    for (i = 0; i < 3; i++) {
      channel_active[i] = max_color_channel[i] > 0;
    }

    int steps = gradients.length - 1;
    int chunk_size = steps == 0 ? 0 : (100 - csm.min_intensity_percent) / steps;

    int v = 100 - (chunk_size * steps);
    // intensity at first gradient
    //    System.err.println("start intensity="+v);  // debug

    int[] intensities = new int[gradients.length];
    for (i=0; i < gradients.length; i++) {
      intensities[i] = v;
      //      System.err.println("index:"+i + " gradient_greater_than_or_equal_to:" + gradients[i] + " val:" + v);  // debug
      v += chunk_size;
    }

    unique_colors.add(background_color);

    // leading entries (before first gradients): background color
    Color[] results = new Color[GenomicMeasurement.MAX_DATAPOINT_VALUE + 1];
    for (i=0; i < gradients[0]; i++) {
      results[i] = background_color;
    }

    //
    //  build index of Colors for each possible data value
    //

    //    System.err.println("");  System.err.println("build start for " + max_color.getRed() + "," + max_color.getGreen() + "," + max_color.getBlue());  // debug

    int[] rgb = new int[3];
    for (i=0; i < gradients.length; i++) {
      int end = i == (gradients.length - 1) ? GenomicMeasurement.MAX_DATAPOINT_VALUE : gradients[i+1];
      Color c;
      if (intensities[i] == 100) {
	c = max_color;
      } else {
	if (csm.white_mode) {
	  // white mode:
	  // - at full intensity, color is as given
	  // - at intermediate intensities, color is paler (mixed with white)
	  for (j=0; j < 3; j++) {
	    if (channel_active[j]) {
	      rgb[j] = max_color_channel[j];
 	      // channels active in desired color are always at maximum as given
	    } else {
	      // other channels are used to fade the primary channel
	      rgb[j] = 255 - ((255 * intensities[i]) / 100);
	    }
	  }
	} else {
	  // dark background:
	  // colors become more intense from baseline value of 0 (black)
	  rgb[0] = (int) ((max_color.getRed() * intensities[i]) / 100);
	  rgb[1] = (int) ((max_color.getGreen() * intensities[i]) / 100);
	  rgb[2] = (int) ((max_color.getBlue() * intensities[i]) / 100);
	}

	//	System.err.println("  final: " + rgb[0] +","+rgb[1]+","+rgb[2]);  // debug

	c = new Color(rgb[0], rgb[1], rgb[2]);
      }
      unique_colors.add(c);

      //      System.err.println("grad:" + gradients[i] + " => " + c.getRed() + "," + c.getGreen() + "," + c.getBlue());  // debug
      for(j = gradients[i]; j <= end; j++) {
	//	System.err.println("  " + j);  // debug
	results[j] = c;
      }
    }

    return results;
  }

  public static void main(String[] argv) {
    ColorScheme cs = new ColorScheme(Color.red, Color.blue);
    int[] gradients = {2,5,10};
  }

  private static HashMap<String,ColorSchemeModel> color_scheme_setup() {
    HashMap<String,ColorSchemeModel> results = new HashMap<String,ColorSchemeModel>();
    results.put(COLOR_SCHEME_RED_GREEN, new ColorSchemeModel(Color.red, Color.green));
    results.put(COLOR_SCHEME_RED_BLUE, new ColorSchemeModel(Color.red, Color.blue));
    results.put(COLOR_SCHEME_MAGENTA_CYAN, new ColorSchemeModel(Color.magenta, Color.cyan));
    return results;
  }

  private static HashMap<String,Color> color_names_setup() {
    HashMap<String,Color> results = new HashMap<String,Color>();

    results.put(LABEL_WHITE, Color.white);
    // add white but not black because each color should be fadeable;
    // white (255,255,255) fades to black (0,0,0)

    results.put(LABEL_RED, Color.red);
    results.put(LABEL_BLUE, Color.blue);
    results.put(LABEL_GREEN, Color.green);
    // primary colors

    results.put(LABEL_CYAN, Color.cyan);
    results.put(LABEL_MAGENTA, Color.magenta);
    results.put(LABEL_YELLOW, Color.yellow);
    // secondary colors

    results.put(LABEL_ORANGE, Color.orange);
    // tertiary colors???

    color_names_sorted = new ArrayList<String>();
    color_names_sorted.addAll(results.keySet());
    Collections.sort(color_names_sorted);

    return results;
  }

  public static ArrayList<String> get_color_names() {
    return color_names_sorted;
  }

  public static Color get_color (String name) {
    return color_names.get(name);
  }

  public HashSet<Color> get_unique_colors() {
    return unique_colors;
  }

  public void import_gradients(ColorSchemeModel csm_arg) {
    csm.gradients = csm_arg.gradients;
    csm.min_intensity_percent = csm_arg.min_intensity_percent;
    if (false) {
      System.err.println("HACK!!");  // debug
      csm.min_intensity_percent=30;
    }
    set_colorscheme_model(csm);
  }

}
