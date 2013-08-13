package TCGA;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;

public class CopyNumberVariationImage extends Observable implements Runnable,Observer {
  //
  //  render (largish) image for copy-number variation changes
  //
  private BufferedImage offscreen = null;
  //private Image offscreen;

  private boolean complete = false;
  private GenomicMeasurement gm;
  private Dimension raw_size;
  private IndexColorModelMap icmm;

  //  private static Color COLOR_NULL = Color.gray;
  private static Color COLOR_NULL = Color.darkGray;

  public CopyNumberVariationImage (GenomicMeasurement gm) {
    this.gm = gm;
    for (ColorScheme cs : gm.get_color_manager().get_all_color_schemes()) {
      // note: GenomicMeasurement must be completely loaded!
      cs.addObserver(this);
    }
    setup();
  }

  private void setup() {
    //    gm.addObserver(this);
    // initial update() will be made from ColorScheme
    refresh_image();
  }

  public void update (Observable o, Object arg) {
    if (gm.is_loaded()) {
      // - data and annotations are both loaded, render now possible
      // - can also receive updates from ColorScheme
      //      new Exception().printStackTrace();
      new Thread(this).start();
    }
  }

  public void run() {
    // async
    build_image();
  }

  private void build_image() {
    //
    //  build image from copy-number variation data
    //
    //    System.err.println("build_image()");  // debug
    ArrayList<GenomicSample> gm_rows = gm.get_visible_rows();
    int sample_count = gm_rows.size();

    //    new Exception().printStackTrace();

    if (sample_count == 0) {
      //
      //  if user has turned off all subsets, view will be empty
      //
      if (offscreen != null) {
	// ensure existing image is wiped
	Graphics g = offscreen.getGraphics();
	g.setColor(Color.black);
	g.fillRect(0,0,offscreen.getWidth(),offscreen.getHeight());
      }
    } else {
      int width = gm_rows.get(0).copynum_data.length;

      raw_size = new Dimension(width, sample_count);

      if (offscreen == null) {
	//  create a new BufferedImage only if we don't have one already.
	//  - problems with out-of-memory crashes
	//  - problems with existing references in components (JScrollPane, etc.)

	try {
	  // offscreen = new BufferedImage(width, sample_count, BufferedImage.TYPE_BYTE_INDEXED);
	  offscreen = new BufferedImage(width, gm.get_rows().size(), BufferedImage.TYPE_BYTE_INDEXED);
	  // create the image based on the complete size of the underlying dataset,
	  // which might be larger than the initially visible size
	  icmm = new IndexColorModelMap((IndexColorModel) offscreen.getColorModel());
	} catch (Throwable t) {
	  // out of memory error?  (applet)
	  new ErrorReporter(t);
	}
      }

      long start_time = System.currentTimeMillis();

      if (Options.DISABLE_ACCELERATION) {
	try {
	  offscreen.setAccelerationPriority(0);
	} catch (NoSuchMethodError e) {
	  System.err.println("can't set accleration priority on this JVM");  // debug
	}
      }

      Graphics g = offscreen.getGraphics();
      g.setColor(Color.black);
      g.fillRect(0,0,offscreen.getWidth(),offscreen.getHeight());
      // ensure entire image is cleared in case we're using less of it (subset)
    
      Color c;
      int x;
      byte point;
    
      if (GenomicSample.NULL_VALUE >= 0) {
	System.err.println("FATAL ERROR: NULL_VALUE must be < 0");  // debug
      }

      int[] rgb_array = new int[width];
      int rgb_null = COLOR_NULL.getRGB();

      //
      // write directly to raster's image buffer.
      // Fast, but requires knowledge/approximation of color map
      //

      //	long index_start = System.currentTimeMillis();
      //		System.err.println("index time=" + (System.currentTimeMillis() - index_start));  // debug
      byte null_i = (byte) icmm.find_closest_index(COLOR_NULL);

      WritableRaster raster = offscreen.getRaster();

      byte[] pixels = ((DataBufferByte) raster.getDataBuffer()).getData();
      // ugh, depending on image this may or may not be byte-based...
      int pi = 0;
      byte ci;

      SampleSubsets ss = gm.get_sample_subsets();
      ColorManager cm = gm.get_color_manager();

      if (cm.is_multicolor_enabled()) {
	//
	//  independent color schemes for each subset
	//
	String[] subsets = ss.get_subsets();
	HashMap<String,ColorScheme> cs_map = cm.get_subset_colors();

	for (String subset : ss.get_subsets_arraylist()) {
	  //
	  // make one pass for each subset (color map)
	  //
	  pi = 0;
	  ColorScheme cs = cs_map.get(subset);
	  Color[] map_up = cs.get_up_color_map();

	  if (false) {
	    // debug
	    System.err.println("render scheme for " + subset + " = " + cs);  // debug
	    System.err.println("  brightness: " + cs.get_minimum_intensity_percent());  // debug
	    int[] gradients = cs.get_gradients();
	    for (int gi=0; gi < gradients.length; gi++) {
	      System.err.println("  " + subset + " #" + gi + ":" + gradients[gi]);
	    }
	  }


	  Color[] map_down = cs.get_down_color_map();
	  byte[] indexed_up = build_index_map(map_up, icmm);
	  byte[] indexed_down = build_index_map(map_down, icmm);

	  for (GenomicSample s : gm_rows) {
	    if (s.sample_id.indexOf(subset) >= 0) {
	      //	      System.err.println("painting " + s.sample_id);  // debug
	      for (x=0; x < width; x++) {
		point = s.copynum_data[x];
		if (point >= 0) {
		  ci = indexed_up[point];
		} else if (point == GenomicSample.NULL_VALUE) {
		  // since we know NULL_VALUE is a negative number,
		  // delay this check until we've know the number is negative
		  // (maybe a little faster?)
		  ci = null_i;
		} else {
		  ci = indexed_down[(byte) Math.abs((int) point)];
		}
		pixels[pi++] = ci;
	      }
	    } else {
	      pi += width;
	    }
	  }

	}
      } else {
	//
	//  no subsets (or disabled); everything uses the same color scheme
	//
	ColorScheme cs = cm.get_global_color_scheme();
	Color[] map_up = cs.get_up_color_map();
	Color[] map_down = cs.get_down_color_map();
	byte[] indexed_up = build_index_map(map_up, icmm);
	byte[] indexed_down = build_index_map(map_down, icmm);

	for (GenomicSample s : gm_rows) {
	  for (x=0; x < width; x++) {
	    point = s.copynum_data[x];
	    if (point >= 0) {
	      ci = indexed_up[point];
	    } else if (point == GenomicSample.NULL_VALUE) {
	      // since we know NULL_VALUE is a negative number,
	      // delay this check until we've know the number is negative
	      // (maybe a little faster?)
	      ci = null_i;
	    } else {
	      ci = indexed_down[(byte) Math.abs((int) point)];
	    }
	    pixels[pi++] = ci;
	  }
	}
      }

      //      System.err.println("image rendering time: " + (System.currentTimeMillis() - start_time) + "ms");  // debug

    }

    complete = true;
    setChanged();
    notifyObservers();
  }

  public BufferedImage get_image() {
    return offscreen;
  }

  public int getWidth() {
    return offscreen.getWidth();
  }

  public int getHeight() {
    return offscreen.getHeight();
  }

  private void refresh_image () {
    update(null,null);
  }

  public boolean is_complete () {
    return complete;
  }

  public GenomicMeasurement get_genomicmeasurement() {
    return gm;
  }

  private int[] build_rgb_map (Color[] colors) {
    int[] rgb = new int[colors.length];
    for (int i = 0; i < colors.length; i++) {
      rgb[i] = colors[i].getRGB();
    }
    return rgb;
  }

  private byte[] build_index_map (Color[] colors, IndexColorModelMap icmm) {
    //    System.err.println("color len="+colors.length);  // debug
    byte[] map = new byte[colors.length];
    for (byte i = 0; i < colors.length; i++) {
      map[i] = (byte) icmm.find_closest_index(colors[i]);
    }
    return map;
  }

  public Dimension get_raw_size() {
    return raw_size;
  }

}
