package TCGA;

import java.util.*;
import java.awt.Color;
import java.awt.Rectangle;

public class TrackInfo {

  private static byte DEFAULT_VALUE = 1;
  
  public static final String TRACK_START_KEY = "track_name";
  // track block always starts with this key

  private String track_name;
  private Color track_color = Options.DEFAULT_CUSTOM_TRACK_COLOR;
  HashSet<Integer> ranges = null;
  ArrayList<Range> ranges2 = new ArrayList<Range>();
  HashMap<Integer,Byte> range_values = null;
  private byte max_range_value = 0;
  private byte[] active = null;
  // at the moment looks more efficient than array of booleans
  private int width;
  private byte default_value = DEFAULT_VALUE;
  
  public void add(String key, String value) {
    // add info to track, if applicable
    if (key.equals("track_name")) {
      track_name = value;
    } else if (key.equals("track_bin")) {
      parse_ranges(value);
    } else if (key.equals("track_color")) {
      String[] c = value.split(",");
      if (c.length == 3) {
	track_color = new Color(Integer.parseInt(c[0]),
				Integer.parseInt(c[1]),
				Integer.parseInt(c[2])
				);
      } else {
	System.err.println("error in #track_color: must be R,G,B");  // debug
      }
    } else if (key.equals("track_constant")) {
      default_value = Byte.parseByte(value);
    } else {
      //      System.err.println(key +"=>"+value);  // debug
    }
  }

  public void set_width (int width) {
    // create integer-indexed view of entire data width
    this.width = width;
    active = new byte[width];
    byte v;
    for (int i = 0; i < width; i++) {
      v = 0;
      if (ranges.contains(i + 1)) {
	if (range_values.containsKey(i + 1)) {
	  v = range_values.get(new Integer(i + 1));
	} else {
	  v = default_value;
	}
      }
      //      System.err.println("setting " + i + ":" +v);  // debug

      active[i] = v;
    }
  }

  private void parse_ranges (String value) {
    ranges = new HashSet<Integer>();
    range_values = new HashMap<Integer,Byte>();
    String[] things = value.split(",");
    Integer key;
    max_range_value = 0;
    for (int i = 0; i < things.length; i++) {
      String thing = things[i];
      Byte range_value = null;
      if (thing.indexOf(":") > -1) {
	String[] t2 = thing.split(":");
	thing = t2[0];
	range_value = new Byte(t2[1]);
	if (range_value > max_range_value) max_range_value = range_value;
	//	System.err.println("thing="+thing + " rv:"+range_value);  // debug
      }
      int start,end;
      if (thing.indexOf("-") > -1) {
	// a range
	String[] range = thing.split("-");
	start = Integer.parseInt(range[0]);
	end = Integer.parseInt(range[1]);
      } else {
	start = end = Integer.parseInt(thing);
      }
      ranges2.add(new Range(start,end));
      for (int j = start; j <= end; j++) {
	key = new Integer(j);
	ranges.add(key);
	if (range_value != null) range_values.put(key, range_value);
      }
    }

    //    System.err.println("max_range_value:"+max_range_value);  // debug
  }

  public int get_width() {
    return width;
  }

  public String get_name() {
    return track_name;
  }

  public byte[] get_active() {
    return active;
  }

  public Color get_color() {
    return track_color;
  }

  public byte get_max_value() {
    return max_range_value > default_value ? max_range_value : default_value;
  }

  public Range get_range (int i) {
    i++;
    // range spec is 1-based
    Range result_r = null;
    for (Range r : ranges2) {
      //      System.err.println(i + " " + r.start + " " + r.end);  // debug
      if (i >= r.start && i <= r.end) {
	result_r = r;
	break;
      }
    }
    return result_r;
  }

  public Rectangle get_range_selection(int i) {
    Rectangle result = null;
    Range result_r = get_range(i);
    if (result_r != null) {
      result = new Rectangle();
      result.x = result_r.start - 1;
      result.width = (result_r.end - result_r.start) + 1;
    }
    
    return result;
  }

  public void collapse_to (ArrayList<Integer> used_bins) {
    int new_index = 1;
    HashSet<Integer> new_ranges = new HashSet<Integer>();
    ArrayList<Range> new_ranges2 = new ArrayList<Range>();
    for (Integer wanted_bin : used_bins) {
      if (ranges.contains(wanted_bin + 1)) {
	new_ranges.add(new_index);
	new_ranges2.add(new Range(new_index, new_index));
      }
      new_index++;
    }
    ranges = new_ranges;
    ranges2 = new_ranges2;
  }

}
