package TCGA;

import java.awt.image.*;
import java.util.*;
import java.awt.Color;

public class IndexColorModelMap {
  private IndexColorModel icm;
  private byte[] r,g,b;
  private HashMap<Color,Integer> color2index;

  public IndexColorModelMap (IndexColorModel icm) {
    this.icm = icm;
    int size = icm.getMapSize();
    icm.getReds(r = new byte[size]);
    icm.getGreens(g = new byte[size]);
    icm.getBlues(b = new byte[size]);
    color2index = new HashMap<Color,Integer>();
  }

  public static void main (String[] argv) {
    BufferedImage bi = new BufferedImage(800,600,BufferedImage.TYPE_BYTE_INDEXED);
    IndexColorModelMap icmm = new IndexColorModelMap((IndexColorModel) bi.getColorModel());
    System.err.println("index=" + icmm.find_closest_index(Color.magenta));  // debug
    Color c = new Color(255,0,0);
    System.err.println("index=" + icmm.find_closest_index(c));
    Color c2 = new Color(255,0,0);
    System.err.println("index=" + icmm.find_closest_index(c2));
  }

  public int find_closest_index(Color c) {
    //
    // find the closest index in the IndexColorModel for the given color.
    //
    int best_i = 0;
    if (color2index.containsKey(c)) {
      // cached
      best_i = color2index.get(c);
    } else {
      int size = icm.getMapSize();
      int i;
      byte cr = (byte) c.getRed();
      byte cg = (byte) c.getGreen();
      byte cb = (byte) c.getBlue();
    
      int best_diff = 0;
      int this_diff;
    
      for (i=0; i < size; i++) {
	this_diff = Math.abs(cr - r[i]) + Math.abs(cg - g[i]) + Math.abs(cb - b[i]);
	if (i == 0 || this_diff < best_diff) {
	  best_i = i;
	  best_diff = this_diff;
	}
      }
      color2index.put(c, new Integer(best_i));
    }

    return best_i;
  }

}
